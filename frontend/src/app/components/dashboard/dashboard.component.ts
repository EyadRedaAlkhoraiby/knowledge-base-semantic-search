import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { Document, SearchResult, SearchResponse, KnowledgeBaseStats } from '../../models/document.model';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
    // Data
    documents: Document[] = [];
    searchResults: SearchResult[] = [];
    stats: KnowledgeBaseStats = { documentCount: 0, embeddingDimension: 768 };

    // UI State
    searchQuery = '';
    selectedCategory = '';
    categories: string[] = [];
    showResults = false;
    showAddModal = false;
    showEditModal = false;
    showDetailModal = false;
    showScrollTop = false;

    // Form
    newDocTitle = '';
    newDocCategory = '';
    newDocContent = '';

    // Detail
    selectedDoc: Document | null = null;

    // Edit
    editDocId = '';
    editDocTitle = '';
    editDocCategory = '';
    editDocContent = '';

    constructor(
        public apiService: ApiService,
        public authService: AuthService,
        public toastService: ToastService,
        private router: Router
    ) { }

    ngOnInit() {
        this.refreshData();
        this.loadCategories();
    }

    get isAdmin(): boolean {
        return this.authService.isAdmin;
    }

    get currentUser() {
        return this.authService.currentUser;
    }

    refreshData() {
        this.apiService.getDocuments().subscribe({
            next: (docs) => this.documents = docs,
            error: () => this.toastService.show('Failed to load documents', 'error')
        });
        this.apiService.getStats().subscribe({
            next: (stats) => this.stats = stats,
            error: () => { }
        });
        this.loadCategories();
    }

    loadCategories() {
        this.apiService.getCategories().subscribe({
            next: (cats) => this.categories = cats,
            error: () => { }
        });
    }

    // Search
    handleSearch() {
        const query = this.searchQuery.trim();
        if (!query) {
            this.showResults = false;
            return;
        }
        this.showResults = true;
        this.searchResults = [];

        this.apiService.search(query, 10, this.selectedCategory || undefined).subscribe({
            next: (data) => this.searchResults = data.results,
            error: () => this.toastService.show('Search failed', 'error')
        });
    }

    onSearchKeypress(event: KeyboardEvent) {
        if (event.key === 'Enter') this.handleSearch();
    }

    // Add Document
    openAddModal() {
        this.showAddModal = true;
        this.newDocTitle = '';
        this.newDocCategory = '';
        this.newDocContent = '';
    }

    closeAddModal() {
        this.showAddModal = false;
    }

    submitDocument() {
        if (!this.newDocTitle.trim() || !this.newDocContent.trim()) {
            this.toastService.show('Please fill in all required fields', 'error');
            return;
        }

        this.apiService.addDocument(
            this.newDocTitle.trim(),
            this.newDocContent.trim(),
            this.newDocCategory.trim() || 'General'
        ).subscribe({
            next: () => {
                this.toastService.show('Document added successfully!', 'success');
                this.closeAddModal();
                this.refreshData();
            },
            error: (err) => this.toastService.show(err.error?.error || 'Failed to add document', 'error')
        });
    }

    // Detail Modal
    showDocument(doc: Document) {
        this.selectedDoc = doc;
        this.showDetailModal = true;
    }

    closeDetailModal() {
        this.showDetailModal = false;
        this.selectedDoc = null;
    }

    // Edit Document
    openEditModal(event: Event, doc: Document) {
        event.stopPropagation();
        this.editDocId = doc.id;
        this.editDocTitle = doc.title;
        this.editDocCategory = doc.category || '';
        this.editDocContent = doc.content;
        this.showEditModal = true;
    }

    closeEditModal() {
        this.showEditModal = false;
    }

    submitEdit() {
        if (!this.editDocTitle.trim() || !this.editDocContent.trim()) {
            this.toastService.show('Please fill in all required fields', 'error');
            return;
        }
        this.apiService.updateDocument(
            this.editDocId,
            this.editDocTitle.trim(),
            this.editDocContent.trim(),
            this.editDocCategory.trim() || 'General'
        ).subscribe({
            next: () => {
                this.toastService.show('Document updated successfully!', 'success');
                this.closeEditModal();
                this.refreshData();
            },
            error: (err) => this.toastService.show(err.error?.error || 'Failed to update document', 'error')
        });
    }

    // Delete
    deleteDocument(event: Event, id: string) {
        event.stopPropagation();
        if (!confirm('Are you sure you want to delete this document?')) return;

        this.apiService.deleteDocument(id).subscribe({
            next: () => {
                this.toastService.show('Document deleted', 'success');
                this.refreshData();
            },
            error: () => this.toastService.show('Failed to delete document', 'error')
        });
    }

    deleteAllDocuments() {
        if (!confirm('⚠️ Are you sure you want to DELETE ALL documents?\n\nThis action cannot be undone!')) return;

        this.apiService.deleteAllDocuments().subscribe({
            next: () => {
                this.toastService.show('🗑️ All documents deleted successfully!', 'success');
                this.refreshData();
            },
            error: () => this.toastService.show('Failed to delete all documents', 'error')
        });
    }

    // File Import
    onFileSelected(event: Event) {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        if (!file) return;

        this.toastService.show('Importing documents... This may take a moment.', 'info');

        this.apiService.importFile(file).subscribe({
            next: (result) => {
                this.toastService.show(`✓ Imported ${result.imported} documents!`, 'success');
                this.refreshData();
            },
            error: (err) => this.toastService.show('Failed to import: ' + (err.error?.error || err.message), 'error')
        });

        input.value = '';
    }

    // Logout
    handleLogout() {
        this.authService.logout().subscribe({
            next: () => this.router.navigate(['/login']),
            error: () => this.router.navigate(['/login'])
        });
    }

    // Format date
    formatDate(dateString: string): string {
        if (!dateString) return 'Unknown';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    // Scroll to top
    @HostListener('window:scroll')
    onScroll() {
        this.showScrollTop = window.scrollY > 300;
    }

    scrollToTop() {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    // Modal backdrop click
    onModalBackdropClick(event: Event, type: string) {
        if (event.target === event.currentTarget) {
            if (type === 'add') this.closeAddModal();
            if (type === 'edit') this.closeEditModal();
            if (type === 'detail') this.closeDetailModal();
        }
    }

    @HostListener('document:keydown.escape')
    onEscape() {
        this.closeAddModal();
        this.closeEditModal();
        this.closeDetailModal();
    }
}

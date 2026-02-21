/**
 * Knowledge Base with Semantic Search - Frontend JavaScript
 * 
 * This file handles:
 * - API calls to the Spring Boot backend
 * - UI updates and DOM manipulation
 * - Search functionality
 * - Document CRUD operations
 */

// API Base URL
const API_BASE = '/api';

// Current user state
let currentUser = null;

// DOM Elements
const elements = {
    searchInput: document.getElementById('searchInput'),
    searchBtn: document.getElementById('searchBtn'),
    resultsSection: document.getElementById('resultsSection'),
    resultsContainer: document.getElementById('resultsContainer'),
    resultsCount: document.getElementById('resultsCount'),
    documentsGrid: document.getElementById('documentsGrid'),
    docCount: document.getElementById('docCount'),
    vocabSize: document.getElementById('vocabSize'),

    // Add Document Modal
    modalOverlay: document.getElementById('modalOverlay'),
    addDocBtn: document.getElementById('addDocBtn'),
    closeModalBtn: document.getElementById('closeModalBtn'),
    cancelBtn: document.getElementById('cancelBtn'),
    documentForm: document.getElementById('documentForm'),
    docTitle: document.getElementById('docTitle'),
    docCategory: document.getElementById('docCategory'),
    docContent: document.getElementById('docContent'),

    // CSV Import
    importCsvBtn: document.getElementById('importCsvBtn'),
    csvFileInput: document.getElementById('csvFileInput'),

    // Detail Modal
    detailModalOverlay: document.getElementById('detailModalOverlay'),
    closeDetailBtn: document.getElementById('closeDetailBtn'),
    detailTitle: document.getElementById('detailTitle'),
    detailCategory: document.getElementById('detailCategory'),
    detailText: document.getElementById('detailText'),
    detailMeta: document.getElementById('detailMeta'),

    // Toast
    toast: document.getElementById('toast'),

    // Auth Elements
    userMenu: document.getElementById('userMenu'),
    usernameDisplay: document.getElementById('usernameDisplay'),
    userRoleDisplay: document.getElementById('userRoleDisplay'),
    logoutBtn: document.getElementById('logoutBtn'),

    // Scroll to Top
    scrollTopBtn: document.getElementById('scrollTopBtn'),

    // Delete All
    deleteAllBtn: document.getElementById('deleteAllBtn')
};

// =====================
// API Functions
// =====================

/**
 * Fetches all documents from the backend.
 */
async function fetchDocuments() {
    try {
        const response = await fetch(`${API_BASE}/documents`);
        if (!response.ok) throw new Error('Failed to fetch documents');
        return await response.json();
    } catch (error) {
        console.error('Error fetching documents:', error);
        showToast('Failed to load documents', 'error');
        return [];
    }
}

/**
 * Fetches knowledge base statistics.
 */
async function fetchStats() {
    try {
        const response = await fetch(`${API_BASE}/stats`);
        if (!response.ok) throw new Error('Failed to fetch stats');
        return await response.json();
    } catch (error) {
        console.error('Error fetching stats:', error);
        return { documentCount: 0, vocabularySize: 0 };
    }
}

/**
 * Performs semantic search.
 */
async function performSearch(query) {
    try {
        const response = await fetch(`${API_BASE}/search?query=${encodeURIComponent(query)}&maxResults=10`);
        if (!response.ok) throw new Error('Search failed');
        return await response.json();
    } catch (error) {
        console.error('Error performing search:', error);
        showToast('Search failed', 'error');
        return { results: [] };
    }
}

/**
 * Adds a new document.
 */
async function addDocument(title, content, category) {
    try {
        const response = await fetch(`${API_BASE}/documents`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, content, category })
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'Failed to add document');
        }
        return await response.json();
    } catch (error) {
        console.error('Error adding document:', error);
        showToast(error.message, 'error');
        return null;
    }
}

/**
 * Imports documents from a file (CSV or Excel).
 */
async function importFile(file) {
    try {
        const formData = new FormData();
        formData.append('file', file);

        showToast('Importing documents... This may take a moment.', 'info');

        const response = await fetch(`${API_BASE}/import`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to import CSV');
        }

        const result = await response.json();
        showToast(`✓ Imported ${result.imported} documents!`, 'success');

        // Refresh the documents list and stats
        await refreshData();

        return result;
    } catch (error) {
        console.error('Error importing CSV:', error);
        showToast('Failed to import: ' + error.message, 'error');
        return null;
    }
}

/**
 * Deletes a document.
 */
async function deleteDocument(id) {
    try {
        const response = await fetch(`${API_BASE}/documents/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to delete document');
        return true;
    } catch (error) {
        console.error('Error deleting document:', error);
        showToast('Failed to delete document', 'error');
        return false;
    }
}

/**
 * Deletes all documents (Admin only).
 */
async function deleteAllDocuments() {
    if (!confirm('⚠️ Are you sure you want to DELETE ALL documents?\n\nThis action cannot be undone!')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/documents/all`, {
            method: 'DELETE'
        });

        if (!response.ok) throw new Error('Failed to delete all documents');

        showToast('🗑️ All documents deleted successfully!', 'success');
        await refreshData();
    } catch (error) {
        console.error('Error deleting all documents:', error);
        showToast('Failed to delete all documents', 'error');
    }
}

// =====================
// UI Rendering Functions
// =====================

/**
 * Updates the stats display.
 */
function updateStats(stats) {
    elements.docCount.textContent = stats.documentCount;
    elements.vocabSize.textContent = stats.embeddingDimension || 768;
}

/**
 * Renders the documents grid.
 */
function renderDocuments(documents) {
    if (documents.length === 0) {
        elements.documentsGrid.innerHTML = `
            <div class="loading">No documents found</div>
        `;
        return;
    }

    // Check if current user is admin
    const isAdmin = currentUser && (currentUser.isAdmin || currentUser.role === 'ADMIN');

    elements.documentsGrid.innerHTML = documents.map(doc => `
        <div class="document-card" data-id="${doc.id}">
            <h3 class="document-title">${escapeHtml(doc.title)}</h3>
            <span class="document-category">${escapeHtml(doc.category || 'Uncategorized')}</span>
            ${isAdmin ? `
            <div class="document-meta-small" style="font-size: 0.8em; color: #666; margin-bottom: 5px;">
                <span>👤 ${escapeHtml(doc.createdBy || 'System')}</span>
            </div>
            ` : ''}

            <p class="document-snippet">${escapeHtml(doc.content)}</p>
            ${isAdmin ? `
            <div class="document-actions">
                <button class="delete-btn" data-id="${doc.id}" onclick="handleDelete(event, '${doc.id}')">Delete</button>
            </div>
            ` : ''}
        </div>
    `).join('');

    // Add click handlers for viewing details
    document.querySelectorAll('.document-card').forEach(card => {
        card.addEventListener('click', (e) => {
            if (!e.target.classList.contains('delete-btn')) {
                const doc = documents.find(d => d.id === card.dataset.id);
                showDocumentDetail(doc);
            }
        });
    });
}

/**
 * Renders search results.
 */
function renderSearchResults(data) {
    elements.resultsSection.classList.add('active');
    elements.resultsCount.textContent = `${data.results.length} results`;

    if (data.results.length === 0) {
        elements.resultsContainer.innerHTML = `
            <div class="loading">No matching documents found</div>
        `;
        return;
    }

    elements.resultsContainer.innerHTML = data.results.map((result, index) => `
        <div class="result-card ${index === 0 ? 'best-match' : ''}" data-id="${result.document.id}">
            <div class="result-header">
                <h3 class="result-title">${escapeHtml(result.document.title)}</h3>
                ${index === 0 ? `
                <div class="similarity-badge most-matched">
                    <span>⭐</span>
                    Most Matched
                </div>
                ` : ''}
            </div>
            <p class="result-content">${escapeHtml(result.document.content)}</p>
            <span class="result-category">${escapeHtml(result.document.category || 'Uncategorized')}</span>
        </div>
    `).join('');

    // Add click handlers for viewing details
    document.querySelectorAll('.result-card').forEach(card => {
        card.addEventListener('click', () => {
            const result = data.results.find(r => r.document.id === card.dataset.id);
            showDocumentDetail(result.document);
        });
    });
}

/**
 * Shows document detail modal.
 */
function showDocumentDetail(doc) {
    elements.detailTitle.textContent = doc.title;
    elements.detailCategory.textContent = doc.category || 'Uncategorized';
    elements.detailText.textContent = doc.content;
    const isAdmin = currentUser && (currentUser.isAdmin || currentUser.role === 'ADMIN');
    elements.detailMeta.innerHTML = `
        <span>📅 Created: ${formatDate(doc.createdAt)}</span>
        ${isAdmin ? `<br><span>👤 Created By: ${escapeHtml(doc.createdBy || 'System')}</span>` : ''}
    `;
    elements.detailModalOverlay.classList.add('active');
}

/**
 * Shows a toast notification.
 */
function showToast(message, type = 'success') {
    elements.toast.textContent = message;
    elements.toast.className = `toast active ${type}`;

    setTimeout(() => {
        elements.toast.classList.remove('active');
    }, 3000);
}

// =====================
// Event Handlers
// =====================

/**
 * Handles search submission.
 */
async function handleSearch() {
    const query = elements.searchInput.value.trim();
    if (!query) {
        elements.resultsSection.classList.remove('active');
        return;
    }

    elements.resultsContainer.innerHTML = '<div class="loading">Searching...</div>';
    elements.resultsSection.classList.add('active');

    const data = await performSearch(query);
    renderSearchResults(data);
}

/**
 * Handles document form submission.
 */
async function handleAddDocument(e) {
    e.preventDefault();

    const title = elements.docTitle.value.trim();
    const content = elements.docContent.value.trim();
    const category = elements.docCategory.value.trim() || 'General';

    if (!title || !content) {
        showToast('Please fill in all required fields', 'error');
        return;
    }

    const doc = await addDocument(title, content, category);
    if (doc) {
        showToast('Document added successfully!', 'success');
        closeModal();
        refreshData();
    }
}

/**
 * Handles document deletion.
 */
async function handleDelete(e, id) {
    e.stopPropagation();

    if (!confirm('Are you sure you want to delete this document?')) {
        return;
    }

    const success = await deleteDocument(id);
    if (success) {
        showToast('Document deleted', 'success');
        refreshData();
    }
}

/**
 * Opens the add document modal.
 */
function openModal() {
    elements.modalOverlay.classList.add('active');
    elements.docTitle.focus();
}

/**
 * Closes the add document modal.
 */
function closeModal() {
    elements.modalOverlay.classList.remove('active');
    elements.documentForm.reset();
}

/**
 * Closes the detail modal.
 */
function closeDetailModal() {
    elements.detailModalOverlay.classList.remove('active');
}

// =====================
// Utility Functions
// =====================

/**
 * Escapes HTML to prevent XSS.
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text || '';
    return div.innerHTML;
}

/**
 * Formats a date string.
 */
function formatDate(dateString) {
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

/**
 * Refreshes all data from the backend.
 */
async function refreshData() {
    const [documents, stats] = await Promise.all([
        fetchDocuments(),
        fetchStats()
    ]);

    renderDocuments(documents);
    updateStats(stats);
}

// =====================
// Initialize
// =====================


// Event Listeners
if (elements.searchBtn) elements.searchBtn.addEventListener('click', handleSearch);
if (elements.searchInput) {
    elements.searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleSearch();
    });
}
if (elements.addDocBtn) elements.addDocBtn.addEventListener('click', openModal);
if (elements.closeModalBtn) elements.closeModalBtn.addEventListener('click', closeModal);
if (elements.cancelBtn) elements.cancelBtn.addEventListener('click', closeModal);
if (elements.documentForm) elements.documentForm.addEventListener('submit', handleAddDocument);
if (elements.deleteAllBtn) elements.deleteAllBtn.addEventListener('click', deleteAllDocuments);


elements.closeDetailBtn.addEventListener('click', closeDetailModal);
elements.detailModalOverlay.addEventListener('click', (e) => {
    if (e.target === elements.detailModalOverlay) closeDetailModal();
});

elements.modalOverlay.addEventListener('click', (e) => {
    if (e.target === elements.modalOverlay) closeModal();
});

// CSV Import
elements.importCsvBtn.addEventListener('click', () => {
    elements.csvFileInput.click();
});

elements.csvFileInput.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (file) {
        await importFile(file);
        elements.csvFileInput.value = ''; // Reset for next upload
    }
});

// Keyboard shortcuts
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        closeModal();
        closeDetailModal();
    }
});

// =====================
// Authentication Functions
// =====================

/**
 * Fetch current user info from API
 */
async function fetchCurrentUser() {
    try {
        const response = await fetch(`${API_BASE}/auth/me`);
        if (response.ok) {
            return await response.json();
        }
        return null;
    } catch (error) {
        console.error('Error fetching user info:', error);
        return null;
    }
}

/**
 * Update UI based on user role
 */
function updateUIForRole(user) {
    if (!user) return;

    currentUser = user;

    // Update user display
    if (elements.usernameDisplay) {
        elements.usernameDisplay.textContent = user.username;
    }
    if (elements.userRoleDisplay) {
        elements.userRoleDisplay.textContent = user.role;
    }

    // Show/hide admin-only elements
    const isAdmin = user.isAdmin || user.role === 'ADMIN';

    // Add/remove admin buttons based on role
    if (elements.addDocBtn) {
        elements.addDocBtn.style.display = isAdmin ? 'block' : 'none';
    }
    if (elements.importCsvBtn) {
        elements.importCsvBtn.style.display = isAdmin ? 'block' : 'none';
    }
    if (elements.deleteAllBtn) {
        elements.deleteAllBtn.style.display = isAdmin ? 'block' : 'none';
    }

    // Show all admin-only elements
    document.querySelectorAll('.admin-only').forEach(el => {
        el.style.display = isAdmin ? 'block' : 'none';
    });
}

/**
 * Handle logout
 */
async function handleLogout() {
    try {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/api/auth/logout';
        document.body.appendChild(form);
        form.submit();
    } catch (error) {
        console.error('Logout error:', error);
        window.location.href = '/login.html';
    }
}

// Logout button event
if (elements.logoutBtn) {
    elements.logoutBtn.addEventListener('click', handleLogout);
}

// =====================
// Initialize Application
// =====================

document.addEventListener('DOMContentLoaded', async () => {
    // Fetch current user first
    const user = await fetchCurrentUser();
    if (user) {
        updateUIForRole(user);
    }

    // Load data
    refreshData();
});

// =====================
// Scroll to Top
// =====================

// Show/hide scroll button based on scroll position
window.addEventListener('scroll', () => {
    if (elements.scrollTopBtn) {
        if (window.scrollY > 300) {
            elements.scrollTopBtn.classList.add('visible');
        } else {
            elements.scrollTopBtn.classList.remove('visible');
        }
    }
});

// Scroll to top when button clicked
if (elements.scrollTopBtn) {
    elements.scrollTopBtn.addEventListener('click', () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Document, SearchResponse, KnowledgeBaseStats } from '../models/document.model';

@Injectable({
    providedIn: 'root'
})
export class ApiService {
    private readonly apiBase = '/api';

    constructor(private http: HttpClient) { }

    // Documents
    getDocuments(): Observable<Document[]> {
        return this.http.get<Document[]>(`${this.apiBase}/documents`);
    }

    getDocument(id: string): Observable<Document> {
        return this.http.get<Document>(`${this.apiBase}/documents/${id}`);
    }

    addDocument(title: string, content: string, category: string): Observable<Document> {
        return this.http.post<Document>(`${this.apiBase}/documents`, { title, content, category });
    }

    deleteDocument(id: string): Observable<any> {
        return this.http.delete(`${this.apiBase}/documents/${id}`);
    }

    deleteAllDocuments(): Observable<any> {
        return this.http.delete(`${this.apiBase}/documents/all`);
    }

    updateDocument(id: string, title: string, content: string, category: string): Observable<Document> {
        return this.http.put<Document>(`${this.apiBase}/documents/${id}`, { title, content, category });
    }

    // Search
    search(query: string, maxResults: number = 10, category?: string): Observable<SearchResponse> {
        let params = new HttpParams()
            .set('query', query)
            .set('maxResults', maxResults.toString());
        if (category) {
            params = params.set('category', category);
        }
        return this.http.get<SearchResponse>(`${this.apiBase}/search`, { params });
    }

    // Stats
    getStats(): Observable<KnowledgeBaseStats> {
        return this.http.get<KnowledgeBaseStats>(`${this.apiBase}/stats`);
    }

    // File Import
    importFile(file: File): Observable<any> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post(`${this.apiBase}/import`, formData);
    }

    // Categories
    getCategories(): Observable<string[]> {
        return this.http.get<string[]>(`${this.apiBase}/categories`);
    }
}

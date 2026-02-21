export interface Document {
    id: string;
    title: string;
    content: string;
    category: string;
    createdAt: string;
    createdBy: string;
}

export interface SearchResult {
    document: Document;
    similarityScore: number;
    scorePercentage: number;
}

export interface SearchResponse {
    query: string;
    totalResults: number;
    results: SearchResult[];
}

export interface KnowledgeBaseStats {
    documentCount: number;
    embeddingDimension: number;
}

export interface User {
    id: number;
    username: string;
    email: string;
    role: string;
    isAdmin: boolean;
}

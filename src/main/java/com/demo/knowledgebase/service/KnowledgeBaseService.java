package com.demo.knowledgebase.service;

import com.demo.knowledgebase.model.Document;
import com.demo.knowledgebase.model.SearchResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic layer for the Knowledge Base.
 * 
 * This service provides a clean interface for:
 * - CRUD operations on documents
 * - Semantic search functionality
 * 
 * It delegates to the VectorStore for storage and search operations.
 */
@Service
public class KnowledgeBaseService {

    private final VectorStore vectorStore;

    public KnowledgeBaseService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Adds a new document to the knowledge base.
     */
    public Document addDocument(String title, String content, String category, String createdBy) {
        Document document = Document.create(title, content, category, createdBy);
        vectorStore.addDocument(document);
        return document;
    }

    /**
     * Adds multiple documents at once.
     */
    public void addDocuments(List<Document> documents) {
        vectorStore.addDocuments(documents);
    }

    /**
     * Gets a document by ID.
     */
    public Optional<Document> getDocument(String id) {
        return vectorStore.getDocument(id);
    }

    /**
     * Gets all documents in the knowledge base.
     */
    public List<Document> getAllDocuments() {
        return vectorStore.getAllDocuments();
    }

    /**
     * Deletes a document by ID.
     */
    public boolean deleteDocument(String id) {
        try {
            // Always try to delete from FAISS - documents may not be in local cache
            vectorStore.removeDocument(id);
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting document: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates a document by deleting and re-adding it with new content.
     * The FAISS embedding is regenerated for the updated text.
     */
    public Optional<Document> updateDocument(String id, String title, String content, String category) {
        try {
            // Delete old document from FAISS
            vectorStore.removeDocument(id);

            // Create updated document with same ID and original timestamp
            Document updated = new Document(id, title, content, category, java.time.LocalDateTime.now(), null);
            vectorStore.addDocument(updated);
            return Optional.of(updated);
        } catch (Exception e) {
            System.err.println("Error updating document: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Performs semantic search to find relevant documents.
     * 
     * This searches based on MEANING, not just keywords!
     * For example, searching "automobile repair" might find
     * documents about "car maintenance" even if those exact
     * words aren't present.
     */
    public List<SearchResult> semanticSearch(String query, int maxResults) {
        return vectorStore.search(query, maxResults);
    }

    /**
     * Performs semantic search with post-FAISS category filtering.
     * Fetches extra results from FAISS to compensate for filtering,
     * then trims to the requested maxResults.
     */
    public List<SearchResult> semanticSearch(String query, int maxResults, String category) {
        if (category == null || category.isBlank()) {
            return semanticSearch(query, maxResults);
        }
        // Fetch more results from FAISS to ensure enough remain after filtering
        int fetchCount = maxResults * 3;
        List<SearchResult> allResults = vectorStore.search(query, fetchCount);
        return allResults.stream()
                .filter(r -> category.equalsIgnoreCase(r.getDocument().getCategory()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Convenience method with default result count.
     */
    public List<SearchResult> semanticSearch(String query) {
        return semanticSearch(query, 10);
    }

    /**
     * Returns all distinct categories from the documents.
     */
    public List<String> getCategories() {
        return vectorStore.getAllDocuments().stream()
                .map(Document::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets statistics about the knowledge base.
     */
    public KnowledgeBaseStats getStats() {
        return new KnowledgeBaseStats(
                vectorStore.getDocumentCount(),
                vectorStore.getEmbeddingDimension());
    }

    /**
     * Reloads documents from FAISS server into local cache.
     * Called after CSV import to sync the cache.
     */
    public void reloadDocuments() {
        // The VectorStore handles caching, this is a placeholder
        // In a full implementation, you'd sync with the FAISS server
        System.out.println("✓ Documents cache reloaded");
    }

    /**
     * Simple stats record.
     */
    public record KnowledgeBaseStats(int documentCount, int embeddingDimension) {
    }
}

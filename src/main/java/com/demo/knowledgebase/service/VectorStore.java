package com.demo.knowledgebase.service;

import com.demo.knowledgebase.model.Document;
import com.demo.knowledgebase.model.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Vector Store using FAISS (Facebook AI Similarity Search).
 * 
 * === WHAT IS FAISS? ===
 * FAISS is a library for efficient similarity search developed by Facebook AI.
 * It can search through millions of vectors in milliseconds!
 * 
 * === HOW IT WORKS ===
 * 1. Documents are stored in the Python FAISS server
 * 2. Each document is converted to a 384-dim vector (embedding)
 * 3. FAISS indexes these vectors for fast similarity search
 * 4. Search queries are also converted to vectors and compared
 * 
 * === PERSISTENCE ===
 * Unlike in-memory storage, FAISS saves to disk:
 * - faiss_data/faiss_index.bin (the vector index)
 * - faiss_data/documents.json (document metadata)
 */
@Service
public class VectorStore {

    private final RestTemplate restTemplate;
    private final String faissServerUrl;

    // Local cache for fast document access
    private final Map<String, Document> localDocuments = new HashMap<>();

    public VectorStore(
            @Value("${embedding.server.url:http://localhost:8000}") String faissServerUrl) {
        this.restTemplate = new RestTemplate();
        this.faissServerUrl = faissServerUrl;
        System.out.println("✓ VectorStore initialized");
        System.out.println("  → Using FAISS server at: " + faissServerUrl);
    }

    /**
     * Adds a document to the FAISS index.
     */
    public void addDocument(Document document) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("id", document.getId());
            body.put("text", document.getTextForEmbedding());
            body.put("metadata", Map.of(
                    "title", document.getTitle(),
                    "category", document.getCategory(),
                    "content", document.getContent(),
                    "createdAt", document.getCreatedAt().toString(),
                    "createdBy", document.getCreatedBy() != null ? document.getCreatedBy() : "System"));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(
                    faissServerUrl + "/index/add",
                    request,
                    Map.class);

            // Cache locally
            localDocuments.put(document.getId(), document);

        } catch (Exception e) {
            System.err.println("Error adding document to FAISS: " + e.getMessage());
            throw new RuntimeException("Failed to add document to FAISS: " + e.getMessage(), e);
        }
    }

    /**
     * Adds multiple documents at once (batch operation - much faster!).
     */
    public void addDocuments(List<Document> docs) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            List<Map<String, Object>> docsData = docs.stream().map(doc -> {
                Map<String, Object> docMap = new HashMap<>();
                docMap.put("id", doc.getId());
                docMap.put("text", doc.getTextForEmbedding());
                docMap.put("metadata", Map.of(
                        "title", doc.getTitle(),
                        "category", doc.getCategory(),
                        "content", doc.getContent(),
                        "createdAt", doc.getCreatedAt().toString(),
                        "createdBy", doc.getCreatedBy() != null ? doc.getCreatedBy() : "System"));
                return docMap;
            }).collect(Collectors.toList());

            Map<String, Object> body = new HashMap<>();
            body.put("documents", docsData);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    faissServerUrl + "/index/add-batch",
                    request,
                    Map.class);

            // Cache locally
            for (Document doc : docs) {
                localDocuments.put(doc.getId(), doc);
            }

            if (response != null) {
                System.out.println("✓ Added " + response.get("added") + " documents to FAISS index");
                System.out.println("  → Total documents: " + response.get("total_documents"));
            }

        } catch (Exception e) {
            System.err.println("Error adding documents to FAISS: " + e.getMessage());
            throw new RuntimeException("Failed to add documents to FAISS: " + e.getMessage(), e);
        }
    }

    /**
     * Removes a document from the index.
     */
    public void removeDocument(String documentId) {
        try {
            restTemplate.delete(faissServerUrl + "/index/document/" + documentId);
            localDocuments.remove(documentId);
        } catch (Exception e) {
            System.err.println("Error removing document: " + e.getMessage());
        }
    }

    /**
     * Performs semantic search using FAISS.
     * 
     * @param query The search query text
     * @param topK  Maximum number of results to return
     * @return List of search results sorted by similarity (highest first)
     */
    @SuppressWarnings("unchecked")
    public List<SearchResult> search(String query, int topK) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("query", query);
            body.put("top_k", topK);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            Map<String, Object> response = restTemplate.postForObject(
                    faissServerUrl + "/index/search",
                    request,
                    Map.class);

            if (response == null || !response.containsKey("results")) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

            return results.stream().map(result -> {
                String id = (String) result.get("id");
                Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");

                // Get from local cache or create from metadata
                Document doc = localDocuments.get(id);
                if (doc == null && metadata != null) {
                    doc = new Document(
                            id,
                            (String) metadata.get("title"),
                            (String) metadata.get("content"),
                            (String) metadata.get("category"),
                            java.time.LocalDateTime.now(),
                            (String) metadata.get("createdBy"));
                }

                // FAISS returns L2 distance (lower is better).
                // Convert to similarity score (0 to 1, higher is better).
                // Formula: 1 / (1 + distance)
                double distance = ((Number) result.get("score")).doubleValue();
                double similarity = 1.0 / (1.0 + distance);

                return new SearchResult(doc, similarity);
            }).collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error searching FAISS: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets a document by ID.
     */
    public Optional<Document> getDocument(String id) {
        return Optional.ofNullable(localDocuments.get(id));
    }

    /**
     * Gets all documents from FAISS server.
     */
    @SuppressWarnings("unchecked")
    public List<Document> getAllDocuments() {
        try {
            // Fetch from FAISS server
            List<Map<String, Object>> response = restTemplate.getForObject(
                    faissServerUrl + "/index/documents",
                    List.class);

            if (response != null) {
                List<Document> docs = new ArrayList<>();
                for (Map<String, Object> docData : response) {
                    String id = (String) docData.get("id");
                    String title = (String) docData.get("title");
                    String content = (String) docData.get("content");
                    String category = (String) docData.get("category");

                    // Use setter-based creation
                    Document doc = new Document();
                    doc.setId(id);
                    doc.setTitle(title != null ? title : "Untitled");
                    doc.setContent(content != null ? content : "");
                    doc.setCategory(category != null ? category : "General");
                    doc.setCreatedAt(java.time.LocalDateTime.now());
                    doc.setCreatedBy((String) docData.get("createdBy"));

                    docs.add(doc);
                }
                return docs;
            }
        } catch (Exception e) {
            System.err.println("Error fetching documents from FAISS: " + e.getMessage());
            e.printStackTrace();
        }
        // Fall back to local cache
        return new ArrayList<>(localDocuments.values());
    }

    /**
     * Returns the total number of documents in the FAISS index.
     */
    @SuppressWarnings("unchecked")
    public int getDocumentCount() {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    faissServerUrl + "/index/stats",
                    Map.class);
            if (response != null) {
                return ((Number) response.get("total_documents")).intValue();
            }
        } catch (Exception e) {
            // Fall back to local count
        }
        return localDocuments.size();
    }

    /**
     * Returns the embedding dimension (384 for multilingual-e5-small).
     */
    public int getEmbeddingDimension() {
        return 384;
    }

    /**
     * Gets FAISS index statistics.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStats() {
        try {
            return restTemplate.getForObject(
                    faissServerUrl + "/index/stats",
                    Map.class);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Clears the entire FAISS index.
     */
    public void clearIndex() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForObject(
                    faissServerUrl + "/index/clear",
                    new HttpEntity<>(Map.of(), headers),
                    Map.class);

            localDocuments.clear();
            System.out.println("✓ FAISS index cleared");
        } catch (Exception e) {
            System.err.println("Error clearing index: " + e.getMessage());
        }
    }
}

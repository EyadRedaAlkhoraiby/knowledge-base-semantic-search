package com.demo.knowledgebase.controller;

import com.demo.knowledgebase.model.Document;
import com.demo.knowledgebase.model.SearchResult;
import com.demo.knowledgebase.service.FileImportService;
import com.demo.knowledgebase.service.KnowledgeBaseService;
import com.demo.knowledgebase.service.KnowledgeBaseService.KnowledgeBaseStats;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for the Knowledge Base.
 * 
 * Provides endpoints for:
 * - CRUD operations on documents
 * - Semantic search
 * - Statistics
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend access
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final FileImportService fileImportService;
    private final RestTemplate restTemplate;
    private final String faissServerUrl;

    public KnowledgeBaseController(
            KnowledgeBaseService knowledgeBaseService,
            FileImportService fileImportService,
            @Value("${embedding.server.url:http://localhost:8000}") String faissServerUrl) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.fileImportService = fileImportService;
        this.restTemplate = new RestTemplate();
        this.faissServerUrl = faissServerUrl;
    }

    // =====================
    // CSV Import
    // =====================

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importFile(@RequestParam("file") MultipartFile file,
            Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "Anonymous";
            Map<String, Object> result = fileImportService.importFile(file, username);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    // =====================
    // Document CRUD Operations
    // =====================

    /**
     * GET /api/documents - Get all documents
     */
    @GetMapping("/documents")
    public List<Document> getAllDocuments() {
        return knowledgeBaseService.getAllDocuments();
    }

    /**
     * GET /api/documents/{id} - Get a specific document
     */
    @GetMapping("/documents/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable String id) {
        return knowledgeBaseService.getDocument(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/documents - Add a new document
     * 
     * Request body should contain:
     * - title: Document title
     * - content: Document content
     * - category: Document category
     */
    @PostMapping("/documents")
    public ResponseEntity<?> addDocument(@RequestBody DocumentRequest request, Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "Anonymous";
            Document document = knowledgeBaseService.addDocument(
                    request.title(),
                    request.content(),
                    request.category(),
                    username);
            return ResponseEntity.status(HttpStatus.CREATED).body(document);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * DELETE /api/documents/{id} - Delete a document
     */
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String id) {
        if (knowledgeBaseService.deleteDocument(id)) {
            return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * DELETE /api/documents/all - Delete all documents (ADMIN only)
     */
    @DeleteMapping("/documents/all")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> deleteAllDocuments() {
        try {
            Map<String, Object> result = restTemplate.postForObject(
                    faissServerUrl + "/index/clear",
                    null,
                    Map.class);
            return ResponseEntity.ok(result != null ? result : Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/documents/{id} - Update a document (ADMIN only)
     */
    @PutMapping("/documents/{id}")
    public ResponseEntity<?> updateDocument(@PathVariable String id, @RequestBody DocumentRequest request) {
        return knowledgeBaseService.updateDocument(
                id,
                request.title(),
                request.content(),
                request.category()).map(doc -> ResponseEntity.ok((Object) doc))
                .orElse(ResponseEntity.notFound().build());
    }

    // =====================
    // Semantic Search
    // =====================

    /**
     * GET /api/search?query=...&category=...&maxResults=... - Perform semantic
     * search
     * 
     * This is the key feature! It finds documents semantically similar
     * to the query, not just keyword matches.
     * Optionally filters results by category after FAISS returns them.
     */
    @GetMapping("/search")
    public SearchResponse semanticSearch(
            @RequestParam String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") int maxResults) {

        List<SearchResult> results = knowledgeBaseService.semanticSearch(query, maxResults, category);

        return new SearchResponse(
                query,
                results.size(),
                results);
    }

    // =====================
    // Categories
    // =====================

    /**
     * GET /api/categories - Get distinct document categories
     */
    @GetMapping("/categories")
    public List<String> getCategories() {
        return knowledgeBaseService.getCategories();
    }

    // =====================
    // Statistics
    // =====================

    /**
     * GET /api/stats - Get knowledge base statistics
     */
    @GetMapping("/stats")
    public KnowledgeBaseStats getStats() {
        return knowledgeBaseService.getStats();
    }

    // =====================
    // DTOs (Data Transfer Objects)
    // =====================

    /**
     * Request body for creating a new document.
     */
    public record DocumentRequest(String title, String content, String category) {
    }

    /**
     * Response wrapper for search results.
     */
    public record SearchResponse(String query, int totalResults, List<SearchResult> results) {
    }
}

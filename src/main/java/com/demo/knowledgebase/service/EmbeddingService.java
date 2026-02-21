package com.demo.knowledgebase.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service for checking embedding server health and getting stats.
 * 
 * Note: The actual embedding generation is now handled by the FAISS server.
 * This service is mainly for health checks and configuration.
 */
@Service
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private final String serverUrl;

    public EmbeddingService(
            @Value("${embedding.server.url:http://localhost:8000}") String serverUrl) {
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl;
        System.out.println("✓ Embedding Service initialized");
        System.out.println("  → Server: " + serverUrl);
        System.out.println("  → Model: multilingual-e5-small (100 languages, Arabic ✓)");
        System.out.println("  → Vector DB: FAISS with persistence");
    }

    /**
     * Returns the embedding dimension (384 for multilingual-e5-small).
     */
    public int getEmbeddingDimension() {
        return 384;
    }

    /**
     * Checks if the FAISS server is available.
     */
    @SuppressWarnings("unchecked")
    public boolean isServerHealthy() {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    serverUrl + "/health",
                    Map.class);
            return response != null && "healthy".equals(response.get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets server statistics.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getServerStats() {
        try {
            return restTemplate.getForObject(
                    serverUrl + "/index/stats",
                    Map.class);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}

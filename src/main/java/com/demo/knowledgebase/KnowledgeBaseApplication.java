package com.demo.knowledgebase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Knowledge Base with Semantic Search application.
 * 
 * This application demonstrates:
 * - A Knowledge Base system for storing documents
 * - Semantic Search using vector embeddings and cosine similarity
 * - RESTful API for CRUD operations and search
 */
@SpringBootApplication
public class KnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseApplication.class, args);
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  Knowledge Base with Semantic Search is running!");
        System.out.println("  Open http://localhost:8080 in your browser");
        System.out.println("=".repeat(60) + "\n");
    }
}

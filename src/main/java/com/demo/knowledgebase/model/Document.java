package com.demo.knowledgebase.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a document in the Knowledge Base.
 * 
 * A document contains:
 * - id: Unique identifier
 * - title: The document's title
 * - content: The full text content
 * - category: Classification category
 * - createdAt: When the document was added
 * 
 * This is the core entity that will be stored and searched.
 */
public class Document {

    private String id;
    private String title;
    private String content;
    private String category;
    private LocalDateTime createdAt;
    private String createdBy;

    // Default constructor
    public Document() {
    }

    // All-args constructor
    public Document(String id, String title, String content, String category, LocalDateTime createdAt,
            String createdBy) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Factory method to create a new document with auto-generated ID and timestamp.
     */
    public static Document create(String title, String content, String category, String createdBy) {
        Document doc = new Document();
        doc.setId(UUID.randomUUID().toString());
        doc.setTitle(title);
        doc.setContent(content);
        doc.setCategory(category);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setCreatedBy(createdBy);
        return doc;
    }

    /**
     * Returns the combined text used for generating embeddings.
     * We combine title and content to get a full representation of the document.
     */
    public String getTextForEmbedding() {
        return title + " " + content;
    }
}

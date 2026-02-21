package com.demo.knowledgebase.model;

/**
 * Represents a search result from the semantic search.
 * 
 * Contains the document and its similarity score.
 * The score ranges from 0 to 1, where:
 * - 1.0 = Perfect match (identical meaning)
 * - 0.0 = No similarity at all
 */
public class SearchResult {

    private Document document;
    private double similarityScore;

    // Default constructor
    public SearchResult() {
    }

    // All-args constructor
    public SearchResult(Document document, double similarityScore) {
        this.document = document;
        this.similarityScore = similarityScore;
    }

    // Getters and Setters
    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    /**
     * Returns the similarity as a percentage (0-100).
     */
    public int getScorePercentage() {
        return (int) Math.round(similarityScore * 100);
    }
}

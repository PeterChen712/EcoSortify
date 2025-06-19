package com.example.glean.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

/**
 * Article model for NewsAPI.org response
 * Based on EcoSortify implementation for environmental news
 */
public class Article {
    
    @SerializedName("source")
    private Source source;
    
    @SerializedName("author")
    private String author;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("url")
    private String url;
    
    @SerializedName("urlToImage")
    private String urlToImage;
    
    @SerializedName("publishedAt")
    private String publishedAt;
    
    @SerializedName("content")
    private String content;
    
    // Additional fields for enhanced functionality
    private Date publishedDate;
    private String formattedDate;
    private boolean isIndonesian;
    private String cleanedTitle;
    private String cleanedDescription;
    
    // Constructors
    public Article() {}
    
    public Article(String title, String description, String url, String source, String formattedDate) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.formattedDate = formattedDate;
        if (source != null) {
            this.source = new Source();
            this.source.setName(source);
        }
    }
    
    // Getters and Setters
    public Source getSource() {
        return source;
    }
    
    public void setSource(Source source) {
        this.source = source;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUrlToImage() {
        return urlToImage;
    }
    
    public void setUrlToImage(String urlToImage) {
        this.urlToImage = urlToImage;
    }
    
    public String getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    // Enhanced getters/setters
    public Date getPublishedDate() {
        return publishedDate;
    }
    
    public void setPublishedDate(Date publishedDate) {
        this.publishedDate = publishedDate;
    }
    
    public String getFormattedDate() {
        return formattedDate;
    }
    
    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }
    
    public boolean isIndonesian() {
        return isIndonesian;
    }
    
    public void setIndonesian(boolean indonesian) {
        isIndonesian = indonesian;
    }
    
    public String getCleanedTitle() {
        return cleanedTitle;
    }
    
    public void setCleanedTitle(String cleanedTitle) {
        this.cleanedTitle = cleanedTitle;
    }
    
    public String getCleanedDescription() {
        return cleanedDescription;
    }
    
    public void setCleanedDescription(String cleanedDescription) {
        this.cleanedDescription = cleanedDescription;
    }
    
    // Utility methods
    public String getSourceName() {
        if (source != null && source.getName() != null) {
            return source.getName();
        }
        return "Unknown";
    }
      public boolean hasValidUrl() {
        return url != null && !url.isEmpty() && 
               (url.startsWith("http://") || url.startsWith("https://")) &&
               !url.contains("removed") && !url.contains("deleted") &&
               url.length() > 10 && url.contains(".");
    }
    
    public boolean hasValidTitle() {
        return title != null && !title.isEmpty() && 
               !title.equals("[Removed]") && !title.toLowerCase().contains("removed") &&
               title.length() > 5;
    }
    
    public boolean hasValidDescription() {
        return description != null && !description.isEmpty() && 
               !description.equals("[Removed]") && !description.toLowerCase().contains("removed") &&
               description.length() > 10;
    }
    
    /**
     * Check if article content is environmentally relevant
     */
    public boolean isEnvironmentallyRelevant() {
        // Use UrlValidator for content relevance check
        return com.example.glean.helper.UrlValidator.isEnvironmentallyRelevant(title, description);
    }
    
    /**
     * Get relevance score (0-100)
     */
    public int getRelevanceScore() {
        return com.example.glean.helper.UrlValidator.calculateRelevanceScore(title, description);
    }
    
    /**
     * Comprehensive validation for article quality
     */
    public boolean isValidArticle() {
        // Basic field validation
        if (!hasValidTitle() || !hasValidUrl()) {
            return false;
        }
        
        // Content relevance check
        if (!isEnvironmentallyRelevant()) {
            return false;
        }
        
        // Minimum relevance score threshold
        if (getRelevanceScore() < 15) {
            return false;
        }
        
        return true;    }
    
    // Detect article category based on content
    private String detectCategory() {
        String text = (title + " " + description).toLowerCase();
        
        if (containsKeywords(text, "daur ulang", "recycle", "recycling")) {
            return "daur ulang";
        } else if (containsKeywords(text, "sampah", "limbah", "waste", "garbage")) {
            return "pengelolaan sampah";
        } else if (containsKeywords(text, "plastik", "plastic")) {
            return "limbah plastik";
        } else if (containsKeywords(text, "lingkungan", "environment", "environmental")) {
            return "pelestarian lingkungan";
        } else if (containsKeywords(text, "energi terbarukan", "renewable energy", "solar", "wind")) {
            return "energi terbarukan";
        } else if (containsKeywords(text, "perubahan iklim", "climate change", "global warming")) {
            return "perubahan iklim";
        } else if (containsKeywords(text, "konservasi", "conservation", "biodiversity")) {
            return "konservasi";
        } else if (containsKeywords(text, "polusi", "pollution", "contamination")) {
            return "polusi";
        }
        
        return "lingkungan";
    }
    
    private boolean containsKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    // Source inner class
    public static class Source {
        @SerializedName("id")
        private String id;
        
        @SerializedName("name")
        private String name;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
}

package com.example.glean.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "news")
public class NewsEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String title;
    private String preview;
    private String fullContent;
    private String date;
    private String source;
    private String imageUrl;
    private String url;
    private String category;
    private long createdAt;
    
    // Default constructor
    public NewsEntity() {
        this.createdAt = System.currentTimeMillis();
    }
    
    // Constructor with required fields
    public NewsEntity(String title, String preview, String source, String date) {
        this.title = title;
        this.preview = preview;
        this.source = source;
        this.date = date;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    
    public String getFullContent() { return fullContent; }
    public void setFullContent(String fullContent) { this.fullContent = fullContent; }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
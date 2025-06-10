package com.example.glean.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.annotation.NonNull;

@Entity(tableName = "news")
public class NewsItem {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;
    
    @ColumnInfo(name = "title")
    private String title;
    
    @ColumnInfo(name = "preview")
    private String preview;
    
    @ColumnInfo(name = "fullContent")
    private String fullContent;
    
    @ColumnInfo(name = "date")
    private String date;
    
    @ColumnInfo(name = "source")
    private String source;
    
    @ColumnInfo(name = "imageUrl")
    private String imageUrl;
    
    @ColumnInfo(name = "url")
    private String url;
    
    @ColumnInfo(name = "category")
    private String category;
    
    @ColumnInfo(name = "createdAt")
    private String createdAt;
    
    // Add these fields as actual database columns
    @ColumnInfo(name = "timestamp")
    private long timestamp;
    
    @ColumnInfo(name = "isRead")
    private boolean isRead;
    
    @ColumnInfo(name = "isFavorite")
    private boolean isFavorite;
    
    @ColumnInfo(name = "isOfflineAvailable")
    private boolean isOfflineAvailable;
    
    @ColumnInfo(name = "readingTimeMinutes")
    private int readingTimeMinutes;
    
    // Constructors
    public NewsItem() {
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.isFavorite = false;
        this.isOfflineAvailable = false;
        this.readingTimeMinutes = 3;
    }
      @Ignore
    public NewsItem(String title, String preview, String fullContent, String date, 
                   String source, String imageUrl, String url, String category) {
        this();
        this.title = title;
        this.preview = preview;
        this.fullContent = fullContent;
        this.date = date;
        this.source = source;
        this.imageUrl = imageUrl;
        this.url = url;
        this.category = category;
    }
    
    // Additional constructor for Article conversion
    @Ignore
    public NewsItem(String title, String description, String url, String source, String formattedDate) {
        this();
        this.title = title;
        this.preview = description;
        this.url = url;
        this.source = source;
        this.date = formattedDate;
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
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    // Database field getters/setters
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    
    public boolean isOfflineAvailable() { return isOfflineAvailable; }
    public void setOfflineAvailable(boolean offlineAvailable) { isOfflineAvailable = offlineAvailable; }
    
    public int getReadingTimeMinutes() { return readingTimeMinutes; }
    public void setReadingTimeMinutes(int readingTimeMinutes) { this.readingTimeMinutes = readingTimeMinutes; }
    
    // Utility methods
    public String getFormattedDate() {
        if (date != null && !date.isEmpty()) {
            return date;
        }
        return "Unknown date";
    }
    
    public int getCategoryColor() {
        // This method can be used by the adapter to determine category colors
        if (category == null) return android.R.color.darker_gray;
        
        switch (category.toLowerCase()) {
            case "climate change":
                return android.R.color.holo_red_light;
            case "renewable energy":
                return android.R.color.holo_green_light;
            case "conservation":
                return android.R.color.holo_blue_light;
            case "pollution":
                return android.R.color.holo_orange_light;
            case "sustainability":
                return android.R.color.holo_green_dark;
            case "green technology":
                return android.R.color.holo_blue_dark;
            case "environmental policy":
                return android.R.color.holo_purple;
            default:
                return android.R.color.darker_gray;
        }
    }
}
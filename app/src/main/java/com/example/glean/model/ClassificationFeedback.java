package com.example.glean.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "classification_feedback")
public class ClassificationFeedback {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String originalPrediction;
    private String correctedType;
    private String imageUri;
    private long timestamp;
    private String userEmail;
    private boolean isSubmitted;
    
    public ClassificationFeedback() {
        this.timestamp = System.currentTimeMillis();
        this.isSubmitted = false;
    }
    
    public ClassificationFeedback(String originalPrediction, String correctedType, String imageUri, String userEmail) {
        this.originalPrediction = originalPrediction;
        this.correctedType = correctedType;
        this.imageUri = imageUri;
        this.userEmail = userEmail;
        this.timestamp = System.currentTimeMillis();
        this.isSubmitted = false;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getOriginalPrediction() {
        return originalPrediction;
    }
    
    public void setOriginalPrediction(String originalPrediction) {
        this.originalPrediction = originalPrediction;
    }
    
    public String getCorrectedType() {
        return correctedType;
    }
    
    public void setCorrectedType(String correctedType) {
        this.correctedType = correctedType;
    }
    
    public String getImageUri() {
        return imageUri;
    }
    
    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public boolean isSubmitted() {
        return isSubmitted;
    }
    
    public void setSubmitted(boolean submitted) {
        isSubmitted = submitted;
    }
}

package com.example.glean.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "trash")
public class TrashEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int userId;
    private int recordId;
    private String trashType;
    private String imagePath;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String description;
    private String mlLabel;      // Add ML prediction label
    private float confidence;    // Add ML confidence score
    
    // Constructor

    public TrashEntity() {
        this.mlLabel = "";
        this.confidence = 0.0f;
    }    @Ignore
    public TrashEntity(int userId, int recordId, String trashType, String imagePath, 
                      double latitude, double longitude, long timestamp, String description) {
        this.userId = userId;
        this.recordId = recordId;
        this.trashType = trashType;
        this.imagePath = imagePath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.description = description;
        this.mlLabel = "";
        this.confidence = 0.0f;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public int getRecordId() {
        return recordId;
    }
    
    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }
    
    public String getTrashType() {
        return trashType;
    }
    
    public void setTrashType(String trashType) {
        this.trashType = trashType;
    }
    
    // Backward compatibility method
    public String getType() {
        return getTrashType();
    }
    
    public void setType(String type) {
        setTrashType(type);
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    // Backward compatibility method
    public String getPhotoPath() {
        return getImagePath();
    }
    
    public void setPhotoPath(String photoPath) {
        setImagePath(photoPath);
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getMlLabel() {
        return mlLabel;
    }
    
    public void setMlLabel(String mlLabel) {
        this.mlLabel = mlLabel;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
}
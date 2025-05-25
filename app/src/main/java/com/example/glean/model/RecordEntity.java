package com.example.glean.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "records")
public class RecordEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int userId;
    private String date;
    private long startTime;
    private long endTime;
    private long duration;        // Add this if missing
    private float totalDistance;  // Add this column
    private int trashCount;
    private String notes;
    
    // Default constructor
    public RecordEntity() {
        this.totalDistance = 0f;
        this.duration = 0L;
        this.endTime = 0L;
        this.trashCount = 0;
        this.notes = "";
    }
    
    // Constructor with required fields
    public RecordEntity(int userId, String date, long startTime) {
        this.userId = userId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = 0L;
        this.duration = 0L;
        this.totalDistance = 0f;
        this.trashCount = 0;
        this.notes = "";
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
    public float getTotalDistance() { return totalDistance; }
    public void setTotalDistance(float totalDistance) { this.totalDistance = totalDistance; }
    
    public int getTrashCount() { return trashCount; }
    public void setTrashCount(int trashCount) { this.trashCount = trashCount; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
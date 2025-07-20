package com.example.glean.model.firebase;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase model for user data stored in Firestore
 */
public class FirebaseUser {
    private String userId;
    private String fullName;
    private String email;
    private long createdAt;
    private double totalPloggingDistance;
    private long totalPloggingTime;
    private int totalTrashCollected;
    private int currentLevel;
    private int currentPoints;
    private long lastActiveAt;
    
    public FirebaseUser() {
        // Required empty constructor for Firestore
    }
    
    public FirebaseUser(String userId, String fullName, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
        this.totalPloggingDistance = 0.0;
        this.totalPloggingTime = 0L;
        this.totalTrashCollected = 0;
        this.currentLevel = 1;
        this.currentPoints = 0;
        this.lastActiveAt = System.currentTimeMillis();
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("fullName", fullName);
        result.put("email", email);
        result.put("createdAt", createdAt);
        result.put("totalPloggingDistance", totalPloggingDistance);
        result.put("totalPloggingTime", totalPloggingTime);
        result.put("totalTrashCollected", totalTrashCollected);
        result.put("currentLevel", currentLevel);
        result.put("currentPoints", currentPoints);
        result.put("lastActiveAt", lastActiveAt);
        return result;
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public double getTotalPloggingDistance() { return totalPloggingDistance; }
    public void setTotalPloggingDistance(double totalPloggingDistance) { 
        this.totalPloggingDistance = totalPloggingDistance; 
    }
    
    public long getTotalPloggingTime() { return totalPloggingTime; }
    public void setTotalPloggingTime(long totalPloggingTime) { 
        this.totalPloggingTime = totalPloggingTime; 
    }
    
    public int getTotalTrashCollected() { return totalTrashCollected; }
    public void setTotalTrashCollected(int totalTrashCollected) { 
        this.totalTrashCollected = totalTrashCollected; 
    }
    
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    
    public int getCurrentPoints() { return currentPoints; }
    public void setCurrentPoints(int currentPoints) { this.currentPoints = currentPoints; }
    
    public long getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(long lastActiveAt) { this.lastActiveAt = lastActiveAt; }
}

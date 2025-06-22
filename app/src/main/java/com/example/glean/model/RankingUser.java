package com.example.glean.model;

import java.io.Serializable;

public class RankingUser implements Serializable {
    private String userId;
    private String username;
    private String fullName;
    private String photoURL;
    private String profileImageUrl;
    private String activeAvatar; // Added for local avatar selection
    private int totalPoints;
    private double totalDistance;
    private int totalTrashCollected;
    private int trashCount;
    private int badgeCount;
    private int position;
    private long lastUpdated;
    
    public RankingUser() {} // Required for Firebase
      public RankingUser(String userId, String username, String fullName, int totalPoints, 
                    double totalDistance, int totalTrashCollected, long lastUpdated) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.totalPoints = totalPoints;
        this.totalDistance = totalDistance;
        this.totalTrashCollected = totalTrashCollected;
        this.trashCount = totalTrashCollected;
        this.lastUpdated = lastUpdated;
        this.badgeCount = 0;
        this.position = 0;
    }
    
    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
      public String getPhotoURL() { return photoURL; }
    public void setPhotoURL(String photoURL) { this.photoURL = photoURL; }
    
    public String getActiveAvatar() { return activeAvatar; }
    public void setActiveAvatar(String activeAvatar) { this.activeAvatar = activeAvatar; }
    
    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    
    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
    
    public int getTotalTrashCollected() { return totalTrashCollected; }
    public void setTotalTrashCollected(int totalTrashCollected) { this.totalTrashCollected = totalTrashCollected; }
    
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public String getProfileImageUrl() { 
        return profileImageUrl != null ? profileImageUrl : photoURL; 
    }
    public void setProfileImageUrl(String profileImageUrl) { 
        this.profileImageUrl = profileImageUrl; 
    }
    
    public int getTrashCount() { return trashCount; }
    public void setTrashCount(int trashCount) { this.trashCount = trashCount; }
    
    public int getBadgeCount() { return badgeCount; }
    public void setBadgeCount(int badgeCount) { this.badgeCount = badgeCount; }
    
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    
    public String getFormattedStats() {
        return String.format("Points: %d • Distance: %.1fkm • Trash: %d", 
                           totalPoints, totalDistance, totalTrashCollected);
    }
}
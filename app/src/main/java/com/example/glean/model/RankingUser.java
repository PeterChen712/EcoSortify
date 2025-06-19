package com.example.glean.model;

import java.io.Serializable;

public class RankingUser implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String username;
    private String profileImageUrl;
    private int totalPoints;
    private double totalDistance;
    private int trashCount;
    private int badgeCount;
    private int position;

    public RankingUser() {
        // Empty constructor needed for Firebase
    }

    public RankingUser(String userId, String username, String profileImageUrl, 
                      int totalPoints, double totalDistance, int trashCount, int badgeCount) {
        this.userId = userId;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
        this.totalPoints = totalPoints;
        this.totalDistance = totalDistance;
        this.trashCount = trashCount;
        this.badgeCount = badgeCount;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public int getTrashCount() {
        return trashCount;
    }

    public void setTrashCount(int trashCount) {
        this.trashCount = trashCount;
    }

    public int getBadgeCount() {
        return badgeCount;
    }

    public void setBadgeCount(int badgeCount) {
        this.badgeCount = badgeCount;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getFormattedDistance() {
        return String.format("%.1f km", totalDistance / 1000.0);
    }    public String getFormattedStats() {
        return String.format("%.1f km • %d poin", 
                totalDistance / 1000.0, totalPoints);
    }
}

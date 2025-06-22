package com.example.glean.model;

public class UserStats {
    private int totalPoints;
    private double totalDistance;
    private int totalTrashCollected;
    private int totalSessions;
    private long totalDuration;
    private long lastUpdated;
    
    public UserStats() {} // Required for Firebase
    
    public UserStats(int totalPoints, double totalDistance, int totalTrashCollected, 
                    int totalSessions, long totalDuration, long lastUpdated) {
        this.totalPoints = totalPoints;
        this.totalDistance = totalDistance;
        this.totalTrashCollected = totalTrashCollected;
        this.totalSessions = totalSessions;
        this.totalDuration = totalDuration;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters and setters
    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    
    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
    
    public int getTotalTrashCollected() { return totalTrashCollected; }
    public void setTotalTrashCollected(int totalTrashCollected) { this.totalTrashCollected = totalTrashCollected; }
    
    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
    
    public long getTotalDuration() { return totalDuration; }
    public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }
    
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    
    @Override
    public String toString() {
        return "UserStats{points=" + totalPoints + ", distance=" + totalDistance + 
               ", trash=" + totalTrashCollected + ", sessions=" + totalSessions + "}";
    }
}
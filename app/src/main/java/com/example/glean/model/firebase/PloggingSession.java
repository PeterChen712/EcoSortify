package com.example.glean.model.firebase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase model for plogging session data stored in Firestore
 */
public class PloggingSession {
    private String sessionId;
    private String userId;
    private long startTime;
    private long endTime;
    private double totalDistance;
    private long duration;
    private int trashCollected;
    private int pointsEarned;
    private List<LatLngPoint> route;
    private List<TrashCollectionPoint> trashPoints;
    private String status; // "active", "completed", "cancelled"
    private long createdAt;
    private long updatedAt;
    
    public PloggingSession() {
        // Required empty constructor for Firestore
    }
    
    public PloggingSession(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.startTime = System.currentTimeMillis();
        this.status = "active";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.totalDistance = 0.0;
        this.duration = 0L;
        this.trashCollected = 0;
        this.pointsEarned = 0;
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("userId", userId);
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("totalDistance", totalDistance);
        result.put("duration", duration);
        result.put("trashCollected", trashCollected);
        result.put("pointsEarned", pointsEarned);
        result.put("route", route);
        result.put("trashPoints", trashPoints);
        result.put("status", status);
        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);
        return result;
    }
    
    // Inner class for route points
    public static class LatLngPoint {
        private double latitude;
        private double longitude;
        private long timestamp;
        
        public LatLngPoint() {}
        
        public LatLngPoint(double latitude, double longitude, long timestamp) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp;
        }
        
        // Getters and setters
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    // Inner class for trash collection points
    public static class TrashCollectionPoint {
        private double latitude;
        private double longitude;
        private String trashType;
        private int points;
        private String photoUrl;
        private long timestamp;
        
        public TrashCollectionPoint() {}
        
        public TrashCollectionPoint(double latitude, double longitude, String trashType, int points, long timestamp) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.trashType = trashType;
            this.points = points;
            this.timestamp = timestamp;
        }
        
        // Getters and setters
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        
        public String getTrashType() { return trashType; }
        public void setTrashType(String trashType) { this.trashType = trashType; }
        
        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }
        
        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    // Main class getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    
    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
    public int getTrashCollected() { return trashCollected; }
    public void setTrashCollected(int trashCollected) { this.trashCollected = trashCollected; }
    
    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
    
    public List<LatLngPoint> getRoute() { return route; }
    public void setRoute(List<LatLngPoint> route) { this.route = route; }
    
    public List<TrashCollectionPoint> getTrashPoints() { return trashPoints; }
    public void setTrashPoints(List<TrashCollectionPoint> trashPoints) { this.trashPoints = trashPoints; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}

package com.example.glean.model;

public class RankingEntity {
    private int position;
    private String username;
    private String avatar;
    private int points;
    private float trashCollected;
    private float distance;
    private int badges;
    private boolean isCurrentUser;
    private String period; // weekly, monthly, all_time
    
    public RankingEntity() {}
    
    public RankingEntity(int position, String username, int points, 
                        float trashCollected, float distance, int badges) {
        this.position = position;
        this.username = username;
        this.points = points;
        this.trashCollected = trashCollected;
        this.distance = distance;
        this.badges = badges;
    }
    
    // Getters and Setters
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }
    
    public float getTrashCollected() {
        return trashCollected;
    }
    
    public void setTrashCollected(float trashCollected) {
        this.trashCollected = trashCollected;
    }
    
    public float getDistance() {
        return distance;
    }
    
    public void setDistance(float distance) {
        this.distance = distance;
    }
    
    public int getBadges() {
        return badges;
    }
    
    public void setBadges(int badges) {
        this.badges = badges;
    }
    
    public boolean isCurrentUser() {
        return isCurrentUser;
    }
    
    public void setCurrentUser(boolean currentUser) {
        isCurrentUser = currentUser;
    }
    
    public String getPeriod() {
        return period;
    }
    
    public void setPeriod(String period) {
        this.period = period;
    }
}

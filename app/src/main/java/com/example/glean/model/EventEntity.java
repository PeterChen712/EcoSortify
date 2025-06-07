package com.example.glean.model;

public class EventEntity {
    private String id;
    private String title;
    private String description;
    private String type; // event, challenge, contest
    private String location;
    private long date;
    private int participants;
    private int maxParticipants;
    private String imageUrl;
    private String organizer;
    private boolean isJoined;
    
    // For challenges
    private int progress;
    private int target;
    private String reward;
    private long timeLeft;
    
    // For contests
    private String prize;
    
    public EventEntity() {}
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public long getDate() {
        return date;
    }
    
    public void setDate(long date) {
        this.date = date;
    }
    
    public int getParticipants() {
        return participants;
    }
    
    public void setParticipants(int participants) {
        this.participants = participants;
    }
    
    public int getMaxParticipants() {
        return maxParticipants;
    }
    
    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getOrganizer() {
        return organizer;
    }
    
    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }
    
    public boolean isJoined() {
        return isJoined;
    }
    
    public void setJoined(boolean joined) {
        isJoined = joined;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    public int getTarget() {
        return target;
    }
    
    public void setTarget(int target) {
        this.target = target;
    }
    
    public String getReward() {
        return reward;
    }
    
    public void setReward(String reward) {
        this.reward = reward;
    }
    
    public long getTimeLeft() {
        return timeLeft;
    }
    
    public void setTimeLeft(long timeLeft) {
        this.timeLeft = timeLeft;
    }
    
    public String getPrize() {
        return prize;
    }
    
    public void setPrize(String prize) {
        this.prize = prize;
    }
}

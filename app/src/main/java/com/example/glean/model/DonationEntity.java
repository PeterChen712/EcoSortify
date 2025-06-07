package com.example.glean.model;

public class DonationEntity {
    private String id;
    private String title;
    private String description;
    private String type; // donation, collaboration, program
    private String imageUrl;
    private String organization;
    
    // For donations
    private long targetAmount;
    private long currentAmount;
    private int donorCount;
    private long timeLeft;
    
    // For collaborations
    private String partner;
    private int participants;
    private String reward;
    
    // For programs
    private String location;
    private int volunteers;
    private String impact;
    
    public DonationEntity() {}
    
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
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getOrganization() {
        return organization;
    }
    
    public void setOrganization(String organization) {
        this.organization = organization;
    }
    
    public long getTargetAmount() {
        return targetAmount;
    }
    
    public void setTargetAmount(long targetAmount) {
        this.targetAmount = targetAmount;
    }
    
    public long getCurrentAmount() {
        return currentAmount;
    }
    
    public void setCurrentAmount(long currentAmount) {
        this.currentAmount = currentAmount;
    }
    
    public int getDonorCount() {
        return donorCount;
    }
    
    public void setDonorCount(int donorCount) {
        this.donorCount = donorCount;
    }
    
    public long getTimeLeft() {
        return timeLeft;
    }
    
    public void setTimeLeft(long timeLeft) {
        this.timeLeft = timeLeft;
    }
    
    public String getPartner() {
        return partner;
    }
    
    public void setPartner(String partner) {
        this.partner = partner;
    }
    
    public int getParticipants() {
        return participants;
    }
    
    public void setParticipants(int participants) {
        this.participants = participants;
    }
    
    public String getReward() {
        return reward;
    }
    
    public void setReward(String reward) {
        this.reward = reward;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public int getVolunteers() {
        return volunteers;
    }
    
    public void setVolunteers(int volunteers) {
        this.volunteers = volunteers;
    }
    
    public String getImpact() {
        return impact;
    }
    
    public void setImpact(String impact) {
        this.impact = impact;
    }
}

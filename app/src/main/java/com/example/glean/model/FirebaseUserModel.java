package com.example.glean.model;

import java.util.List;
import java.util.Map;

public class FirebaseUserModel {
    private String uid;
    private String email;
    private String displayName;
    private String photoUrl;
    private String firstName;
    private String lastName;
    private long joinDate;
    private int totalPoints;
    private int totalPloggingCount;
    private double totalDistance;
    private List<String> achievements;
    private Map<String, Object> preferences;
    private String currentDecoration;
    private boolean isPublicProfile;
    private String location;
    private String bio;

    public FirebaseUserModel() {
        // Required for Firebase
    }

    public FirebaseUserModel(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.joinDate = System.currentTimeMillis();
        this.totalPoints = 0;
        this.totalPloggingCount = 0;
        this.totalDistance = 0.0;
        this.isPublicProfile = true;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public long getJoinDate() { return joinDate; }
    public void setJoinDate(long joinDate) { this.joinDate = joinDate; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public int getTotalPloggingCount() { return totalPloggingCount; }
    public void setTotalPloggingCount(int totalPloggingCount) { this.totalPloggingCount = totalPloggingCount; }

    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }

    public List<String> getAchievements() { return achievements; }
    public void setAchievements(List<String> achievements) { this.achievements = achievements; }

    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> preferences) { this.preferences = preferences; }

    public String getCurrentDecoration() { return currentDecoration; }
    public void setCurrentDecoration(String currentDecoration) { this.currentDecoration = currentDecoration; }

    public boolean isPublicProfile() { return isPublicProfile; }
    public void setPublicProfile(boolean publicProfile) { isPublicProfile = publicProfile; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
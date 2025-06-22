package com.example.glean.model;

import java.util.ArrayList;
import java.util.List;

public class UserProfile {
    private String userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String avatarUrl;
    private String badgeUrl;
    private long lastUpdated;
    
    // Additional fields to handle various Firebase document structures
    private String fullName;
    private String nama;
    private String photoURL;
    private String profileImagePath;
    private int totalPoints;
    private double totalKm;
    private int currentPoints;
    private double totalPloggingDistance;
    private int totalTrashCollected;
    private int currentLevel;
    
    // Profile customization fields
    private List<String> selectedBadges;
    private List<String> ownedBackgrounds;
    private String activeBackground;
    private String activeAvatar; // Field baru untuk avatar aktif
    
    public UserProfile() {} // Required for Firebase
    
    public UserProfile(String userId, String username, String firstName, String lastName,
                      String email, String avatarUrl, String badgeUrl, long lastUpdated) {
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.badgeUrl = badgeUrl;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters and setters for main fields
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getFirstName() { 
        if (firstName != null && !firstName.isEmpty()) {
            return firstName;
        } else if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.split(" ");
            return parts.length > 0 ? parts[0] : fullName;
        } else if (nama != null && !nama.isEmpty()) {
            String[] parts = nama.split(" ");
            return parts.length > 0 ? parts[0] : nama;
        }
        return firstName;
    }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { 
        if (lastName != null && !lastName.isEmpty()) {
            return lastName;
        } else if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.split(" ");
            if (parts.length > 1) {
                StringBuilder lastNameBuilder = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) lastNameBuilder.append(" ");
                    lastNameBuilder.append(parts[i]);
                }
                return lastNameBuilder.toString();
            }
        } else if (nama != null && !nama.isEmpty()) {
            String[] parts = nama.split(" ");
            if (parts.length > 1) {
                StringBuilder lastNameBuilder = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) lastNameBuilder.append(" ");
                    lastNameBuilder.append(parts[i]);
                }
                return lastNameBuilder.toString();
            }
        }
        return lastName;
    }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAvatarUrl() { 
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            return avatarUrl;
        } else if (photoURL != null && !photoURL.isEmpty()) {
            return photoURL;
        } else if (profileImagePath != null && !profileImagePath.isEmpty()) {
            return profileImagePath;
        }
        return avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getBadgeUrl() { return badgeUrl; }
    public void setBadgeUrl(String badgeUrl) { this.badgeUrl = badgeUrl; }
    
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    
    // Additional getters and setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { 
        this.fullName = fullName;
        if ((firstName == null || firstName.isEmpty()) && fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.split(" ");
            if (parts.length > 0) {
                firstName = parts[0];
                if (parts.length > 1) {
                    StringBuilder lastNameBuilder = new StringBuilder();
                    for (int i = 1; i < parts.length; i++) {
                        if (i > 1) lastNameBuilder.append(" ");
                        lastNameBuilder.append(parts[i]);
                    }
                    lastName = lastNameBuilder.toString();
                }
            }
        }
    }
    
    public String getNama() { return nama; }
    public void setNama(String nama) { 
        this.nama = nama;
        if ((firstName == null || firstName.isEmpty()) && nama != null && !nama.isEmpty()) {
            String[] parts = nama.split(" ");
            if (parts.length > 0) {
                firstName = parts[0];
                if (parts.length > 1) {
                    StringBuilder lastNameBuilder = new StringBuilder();
                    for (int i = 1; i < parts.length; i++) {
                        if (i > 1) lastNameBuilder.append(" ");
                        lastNameBuilder.append(parts[i]);
                    }
                    lastName = lastNameBuilder.toString();
                }
            }
        }
    }
    
    public String getPhotoURL() { return photoURL; }
    public void setPhotoURL(String photoURL) { 
        this.photoURL = photoURL;
        if ((avatarUrl == null || avatarUrl.isEmpty()) && photoURL != null && !photoURL.isEmpty()) {
            avatarUrl = photoURL;
        }
    }
    
    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { 
        this.profileImagePath = profileImagePath;
        if ((avatarUrl == null || avatarUrl.isEmpty()) && profileImagePath != null && !profileImagePath.isEmpty()) {
            avatarUrl = profileImagePath;
        }
    }
    
    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    
    public double getTotalKm() { return totalKm; }
    public void setTotalKm(double totalKm) { this.totalKm = totalKm; }
    
    public int getCurrentPoints() { return currentPoints; }
    public void setCurrentPoints(int currentPoints) { this.currentPoints = currentPoints; }
    
    public double getTotalPloggingDistance() { return totalPloggingDistance; }
    public void setTotalPloggingDistance(double totalPloggingDistance) { this.totalPloggingDistance = totalPloggingDistance; }
    
    public int getTotalTrashCollected() { return totalTrashCollected; }
    public void setTotalTrashCollected(int totalTrashCollected) { this.totalTrashCollected = totalTrashCollected; }
    
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    
    // Profile customization getters and setters
    public List<String> getSelectedBadges() { 
        return selectedBadges != null ? selectedBadges : new ArrayList<>(); 
    }
    public void setSelectedBadges(List<String> selectedBadges) { this.selectedBadges = selectedBadges; }
    
    public List<String> getOwnedBackgrounds() { 
        return ownedBackgrounds != null ? ownedBackgrounds : new ArrayList<>(); 
    }
    public void setOwnedBackgrounds(List<String> ownedBackgrounds) { this.ownedBackgrounds = ownedBackgrounds; }
      public String getActiveBackground() { 
        return activeBackground != null ? activeBackground : "default"; 
    }
    public void setActiveBackground(String activeBackground) { this.activeBackground = activeBackground; }
    
    public String getActiveAvatar() {
        return activeAvatar != null ? activeAvatar : "avatar_1";
    }
    public void setActiveAvatar(String activeAvatar) { 
        this.activeAvatar = activeAvatar; 
    }
    
    /**
     * Get the display name for UI purposes
     */
    public String getDisplayName() {
        String first = getFirstName();
        String last = getLastName();
        
        if (first != null && !first.isEmpty()) {
            if (last != null && !last.isEmpty()) {
                return first + " " + last;
            }
            return first;
        } else if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        } else if (nama != null && !nama.isEmpty()) {
            return nama;
        } else if (username != null && !username.isEmpty()) {
            return username;
        } else if (email != null && !email.isEmpty()) {
            return email.split("@")[0];
        }
        
        return "User";
    }
}


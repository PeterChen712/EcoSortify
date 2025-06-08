package com.example.glean.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String email;
    private String password;
    private String username;
    private String firstName;
    private String lastName;
    private String profileImagePath;
    private long createdAt;
    private int points;
    private String decorations;
    private String activeDecoration;
    
    // Default constructor
    public UserEntity() {
        this.createdAt = System.currentTimeMillis();
        this.points = 0;
        this.decorations = "";
        this.activeDecoration = "";
    }
    
    // Constructor with required fields
    public UserEntity(String email, String password) {
        this.email = email;
        this.password = password;
        this.createdAt = System.currentTimeMillis();
        this.points = 0;
        this.decorations = "";
        this.activeDecoration = "";
    }
    
    // Constructor with email, password, and username
    public UserEntity(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.createdAt = System.currentTimeMillis();
        this.points = 0;
        this.decorations = "";
        this.activeDecoration = "";
    }
    
    // Getters and Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public String getEmail() { 
        return email; 
    }
    
    public void setEmail(String email) { 
        this.email = email; 
    }
    
    public String getPassword() { 
        return password; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
    }
    
    public String getUsername() { 
        return username; 
    }
    
    public void setUsername(String username) { 
        this.username = username; 
    }
    
    public String getFirstName() { 
        return firstName; 
    }
    
    public void setFirstName(String firstName) { 
        this.firstName = firstName; 
    }
    
    public String getLastName() { 
        return lastName; 
    }
    
    public void setLastName(String lastName) { 
        this.lastName = lastName; 
    }
    
    public String getProfileImagePath() { 
        return profileImagePath; 
    }
    
    public void setProfileImagePath(String profileImagePath) { 
        this.profileImagePath = profileImagePath; 
    }
    
    public long getCreatedAt() { 
        return createdAt; 
    }
    
    public void setCreatedAt(long createdAt) { 
        this.createdAt = createdAt; 
    }
    
    public int getPoints() { 
        return points; 
    }
    
    public void setPoints(int points) { 
        this.points = points; 
    }
    
    // Add these methods for compatibility with PloggingFragment
    public int getTotalPoints() { 
        return points; 
    }
    
    public void setTotalPoints(int totalPoints) { 
        this.points = totalPoints; 
    }
    
    public String getDecorations() { 
        return decorations; 
    }
    
    public void setDecorations(String decorations) { 
        this.decorations = decorations; 
    }
    
    public String getActiveDecoration() { 
        return activeDecoration; 
    }
    
    public void setActiveDecoration(String activeDecoration) { 
        this.activeDecoration = activeDecoration; 
    }
    
    // Utility methods
    public void addPoints(int pointsToAdd) {
        this.points += pointsToAdd;
    }
    
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else if (username != null) {
            return username;
        } else {
            return email;
        }
    }
    
    public String getDisplayName() {
        if (username != null && !username.trim().isEmpty()) {
            return username;
        } else {
            return getFullName();
        }
    }
    
    public String getName() {
        if (username != null && !username.isEmpty()) {
            return username;
        } else if (firstName != null && !firstName.isEmpty()) {
            return firstName + (lastName != null && !lastName.isEmpty() ? " " + lastName : "");
        } else {
            return email != null ? email.split("@")[0] : "User";
        }
    }
    
    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", points=" + points +
                ", createdAt=" + createdAt +
                ", decorations='" + decorations + '\'' +
                ", activeDecoration='" + activeDecoration + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        UserEntity that = (UserEntity) o;
        
        if (id != that.id) return false;
        return email != null ? email.equals(that.email) : that.email == null;
    }
    
    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (email != null ? email.hashCode() : 0);
        return result;
    }
}
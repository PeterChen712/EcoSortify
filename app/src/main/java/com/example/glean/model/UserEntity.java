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
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    
    public String getDecorations() { return decorations; }
    public void setDecorations(String decorations) { this.decorations = decorations; }
    
    public String getActiveDecoration() { return activeDecoration; }
    public void setActiveDecoration(String activeDecoration) { this.activeDecoration = activeDecoration; }
}
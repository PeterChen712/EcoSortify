package com.example.glean.model;

/**
 * Avatar model for local avatar system
 * Each avatar has an ID and corresponding drawable resource
 */
public class Avatar {
    private String id;
    private String name;
    private int drawableResourceId;
    private boolean isUnlocked;
    private int requiredPoints;

    public Avatar(String id, String name, int drawableResourceId) {
        this.id = id;
        this.name = name;
        this.drawableResourceId = drawableResourceId;
        this.isUnlocked = true; // Default to unlocked
        this.requiredPoints = 0;
    }

    public Avatar(String id, String name, int drawableResourceId, int requiredPoints) {
        this.id = id;
        this.name = name;
        this.drawableResourceId = drawableResourceId;
        this.isUnlocked = false;
        this.requiredPoints = requiredPoints;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDrawableResourceId() {
        return drawableResourceId;
    }

    public void setDrawableResourceId(int drawableResourceId) {
        this.drawableResourceId = drawableResourceId;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public int getRequiredPoints() {
        return requiredPoints;
    }

    public void setRequiredPoints(int requiredPoints) {
        this.requiredPoints = requiredPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Avatar avatar = (Avatar) o;
        return id != null ? id.equals(avatar.id) : avatar.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Avatar{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", drawableResourceId=" + drawableResourceId +
                ", isUnlocked=" + isUnlocked +
                ", requiredPoints=" + requiredPoints +
                '}';
    }
}

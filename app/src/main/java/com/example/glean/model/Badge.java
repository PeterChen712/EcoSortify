package com.example.glean.model;

public class Badge {
    private int id;
    private String name;
    private String description;
    private String type;
    private int level;
    private boolean earned;
    private long earnedDate;
    private int requiredPoints;
    private int iconResource;

    public Badge() {}

    public Badge(int id, String name, String description, String type, int level, boolean earned) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.level = level;
        this.earned = earned;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isEarned() {
        return earned;
    }

    public void setEarned(boolean earned) {
        this.earned = earned;
        if (earned && earnedDate == 0) {
            earnedDate = System.currentTimeMillis();
        }
    }

    public long getEarnedDate() {
        return earnedDate;
    }

    public void setEarnedDate(long earnedDate) {
        this.earnedDate = earnedDate;
    }

    public int getRequiredPoints() {
        return requiredPoints;
    }

    public void setRequiredPoints(int requiredPoints) {
        this.requiredPoints = requiredPoints;
    }

    public int getIconResource() {
        return iconResource;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Badge badge = (Badge) obj;
        return id == badge.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Badge{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", level=" + level +
                ", earned=" + earned +
                '}';
    }

    // Static factory methods for common badges
    public static Badge createDistanceBadge(int level, boolean earned) {
        String[] names = {"Walker", "Hiker", "Explorer"};
        String[] descriptions = {"Walk 1km", "Walk 10km", "Walk 50km"};
        int[] points = {100, 500, 2000};
        
        Badge badge = new Badge();
        badge.setType("distance");
        badge.setLevel(level);
        badge.setName(names[Math.min(level - 1, names.length - 1)]);
        badge.setDescription(descriptions[Math.min(level - 1, descriptions.length - 1)]);
        badge.setRequiredPoints(points[Math.min(level - 1, points.length - 1)]);
        badge.setEarned(earned);
        return badge;
    }

    public static Badge createCleanupBadge(int level, boolean earned) {
        String[] names = {"Cleaner", "Guardian", "Champion"};
        String[] descriptions = {"Clean 5 items", "Clean 25 items", "Clean 100 items"};
        int[] points = {50, 250, 1000};
        
        Badge badge = new Badge();
        badge.setType("cleanup");
        badge.setLevel(level);
        badge.setName(names[Math.min(level - 1, names.length - 1)]);
        badge.setDescription(descriptions[Math.min(level - 1, descriptions.length - 1)]);
        badge.setRequiredPoints(points[Math.min(level - 1, points.length - 1)]);
        badge.setEarned(earned);
        return badge;
    }
}
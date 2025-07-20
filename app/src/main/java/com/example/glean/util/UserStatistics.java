package com.example.glean.util;

public class UserStatistics {
    private int totalRecords;
    private float totalDistance;
    private long totalDuration;
    private int totalPoints;
    private float averageDistance;
    private long averageDuration;
    private float maxDistance;
    private long maxDuration;

    public UserStatistics() {
        // Default constructor
    }

    public UserStatistics(int totalRecords, float totalDistance, long totalDuration, int totalPoints) {
        this.totalRecords = totalRecords;
        this.totalDistance = totalDistance;
        this.totalDuration = totalDuration;
        this.totalPoints = totalPoints;
        
        // Calculate averages
        if (totalRecords > 0) {
            this.averageDistance = totalDistance / totalRecords;
            this.averageDuration = totalDuration / totalRecords;
        }
    }

    // Getters and Setters
    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(float totalDistance) {
        this.totalDistance = totalDistance;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public float getAverageDistance() {
        return averageDistance;
    }

    public void setAverageDistance(float averageDistance) {
        this.averageDistance = averageDistance;
    }

    public long getAverageDuration() {
        return averageDuration;
    }

    public void setAverageDuration(long averageDuration) {
        this.averageDuration = averageDuration;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
    }

    public long getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(long maxDuration) {
        this.maxDuration = maxDuration;
    }

    // Utility methods
    public float getTotalDistanceInKm() {
        return totalDistance / 1000f;
    }

    public float getAverageDistanceInKm() {
        return averageDistance / 1000f;
    }

    public float getMaxDistanceInKm() {
        return maxDistance / 1000f;
    }

    public long getTotalDurationInMinutes() {
        return totalDuration / (60 * 1000);
    }

    public long getAverageDurationInMinutes() {
        return averageDuration / (60 * 1000);
    }

    public long getMaxDurationInMinutes() {
        return maxDuration / (60 * 1000);
    }

    @Override
    public String toString() {
        return "UserStatistics{" +
                "totalRecords=" + totalRecords +
                ", totalDistance=" + totalDistance +
                ", totalDuration=" + totalDuration +
                ", totalPoints=" + totalPoints +
                '}';
    }
}

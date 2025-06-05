package com.example.glean.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(tableName = "records",
        foreignKeys = @ForeignKey(entity = UserEntity.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "userId")})
public class RecordEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int userId;
    private float distance; // in meters
    private long duration; // in milliseconds
    private float averageSpeed; // in m/s
    private int points;
    private String routeData; // JSON string of route coordinates
    private String imagePath; // path to captured trash image
    private String description;
    private double startLatitude;
    private double startLongitude;
    private double endLatitude;
    private double endLongitude;
    private long createdAt;
    private long updatedAt;
    private boolean isUploaded;
    private String type; // "run", "plog", "cleanup"

    // Constructors
    public RecordEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isUploaded = false;
    }

    public RecordEntity(int userId, float distance, long duration, int points) {
        this();
        this.userId = userId;
        this.distance = distance;
        this.duration = duration;
        this.points = points;
        this.averageSpeed = duration > 0 ? distance / (duration / 1000f) : 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
        // Recalculate average speed when distance changes
        if (duration > 0) {
            this.averageSpeed = distance / (duration / 1000f);
        }
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
        // Recalculate average speed when duration changes
        if (duration > 0) {
            this.averageSpeed = distance / (duration / 1000f);
        }
    }

    public float getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(float averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getRouteData() {
        return routeData;
    }

    public void setRouteData(String routeData) {
        this.routeData = routeData;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartLatitude() {
        return startLatitude;
    }

    public void setStartLatitude(double startLatitude) {
        this.startLatitude = startLatitude;
    }

    public double getStartLongitude() {
        return startLongitude;
    }

    public void setStartLongitude(double startLongitude) {
        this.startLongitude = startLongitude;
    }

    public double getEndLatitude() {
        return endLatitude;
    }

    public void setEndLatitude(double endLatitude) {
        this.endLatitude = endLatitude;
    }

    public double getEndLongitude() {
        return endLongitude;
    }

    public void setEndLongitude(double endLongitude) {
        this.endLongitude = endLongitude;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isUploaded() {
        return isUploaded;
    }

    public void setUploaded(boolean uploaded) {
        isUploaded = uploaded;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Utility methods
    public float getDistanceInKm() {
        return distance / 1000f;
    }

    public float getSpeedInKmh() {
        return averageSpeed * 3.6f;
    }

    public long getDurationInMinutes() {
        return duration / (60 * 1000);
    }

    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "RecordEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", distance=" + distance +
                ", duration=" + duration +
                ", points=" + points +
                ", type='" + type + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
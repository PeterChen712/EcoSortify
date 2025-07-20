package com.example.glean.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "location_points",
    foreignKeys = @ForeignKey(
        entity = RecordEntity.class,
        parentColumns = "id",
        childColumns = "recordId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("recordId")}
)
public class LocationPointEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private int recordId;
    private double latitude;
    private double longitude;
    private double altitude;
    private long timestamp;
    private float distanceFromLast;
    
    public LocationPointEntity() {
    }
    
    @Ignore
    public LocationPointEntity(int recordId, double latitude, double longitude, double altitude, long timestamp, float distanceFromLast) {
        this.recordId = recordId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.timestamp = timestamp;
        this.distanceFromLast = distanceFromLast;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public int getRecordId() {
        return recordId;
    }
    
    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public double getAltitude() {
        return altitude;
    }
    
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public float getDistanceFromLast() {
        return distanceFromLast;
    }
    
    public void setDistanceFromLast(float distanceFromLast) {
        this.distanceFromLast = distanceFromLast;
    }
}
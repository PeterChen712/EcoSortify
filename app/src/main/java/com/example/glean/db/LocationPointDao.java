package com.example.glean.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.LocationPointEntity;

import java.util.List;

@Dao
public interface LocationPointDao {
    
    @Insert
    long insert(LocationPointEntity locationPoint);
    
    @Update
    void update(LocationPointEntity locationPoint);
    
    @Delete
    void delete(LocationPointEntity locationPoint);
    
    @Query("SELECT * FROM location_points WHERE id = :id")
    LiveData<LocationPointEntity> getLocationPointById(int id);
    
    @Query("SELECT * FROM location_points WHERE id = :id")
    LocationPointEntity getLocationPointByIdSync(int id);
    
    @Query("SELECT * FROM location_points WHERE recordId = :recordId ORDER BY timestamp ASC")
    LiveData<List<LocationPointEntity>> getLocationPointsByRecordId(int recordId);
    
    @Query("SELECT * FROM location_points WHERE recordId = :recordId ORDER BY timestamp ASC")
    List<LocationPointEntity> getLocationPointsByRecordIdSync(int recordId);
    
    @Query("SELECT * FROM location_points ORDER BY timestamp DESC")
    LiveData<List<LocationPointEntity>> getAllLocationPoints();
    
    @Query("SELECT * FROM location_points ORDER BY timestamp DESC")
    List<LocationPointEntity> getAllLocationPointsSync();
    
    @Query("SELECT COUNT(*) FROM location_points WHERE recordId = :recordId")
    int getLocationPointCountByRecordId(int recordId);
    
    @Query("SELECT SUM(distanceFromLast) FROM location_points WHERE recordId = :recordId")
    float getTotalDistanceByRecordId(int recordId);
    
    @Query("DELETE FROM location_points WHERE recordId = :recordId")
    void deleteLocationPointsByRecordId(int recordId);
    
    @Query("SELECT * FROM location_points WHERE recordId = :recordId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    List<LocationPointEntity> getLocationPointsByTimeRange(int recordId, long startTime, long endTime);
    
    @Query("SELECT * FROM location_points WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon")
    List<LocationPointEntity> getLocationPointsByBounds(double minLat, double maxLat, double minLon, double maxLon);
}
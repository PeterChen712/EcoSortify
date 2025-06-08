package com.example.glean.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.RecordEntity;

import java.util.List;

@Dao
public interface DaoRecord {
    
    @Insert
    long insert(RecordEntity record);
    
    @Update
    void update(RecordEntity record);
    
    @Delete
    void delete(RecordEntity record);
    
    @Query("SELECT * FROM records WHERE id = :recordId")
    LiveData<RecordEntity> getRecordById(int recordId);
    
    @Query("SELECT * FROM records WHERE id = :recordId")
    RecordEntity getRecordByIdSync(int recordId);
    
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<RecordEntity>> getRecordsByUserId(int userId);
    
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY createdAt DESC")
    List<RecordEntity> getRecordsByUserIdSync(int userId);
    
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY createdAt DESC")
    List<RecordEntity> getAllRecordsByUserIdSync(int userId);
    
    @Query("SELECT * FROM records ORDER BY createdAt DESC")
    LiveData<List<RecordEntity>> getAllRecords();
    
    @Query("SELECT * FROM records ORDER BY createdAt DESC")
    List<RecordEntity> getAllRecordsSync();
    
    @Query("UPDATE records SET distance = distance + :additionalDistance WHERE id = :recordId")
    void updateDistance(int recordId, float additionalDistance);
    
    @Query("UPDATE records SET duration = :duration WHERE id = :recordId")
    void updateDuration(int recordId, long duration);
    
    @Query("UPDATE records SET updatedAt = :updatedAt WHERE id = :recordId")
    void updateTimestamp(int recordId, long updatedAt);
    
    @Query("UPDATE records SET distance = :distance, duration = :duration, updatedAt = :updatedAt WHERE id = :recordId")
    void updateRecordStats(int recordId, float distance, long duration, long updatedAt);
    
    @Query("SELECT COUNT(*) FROM records WHERE userId = :userId")
    int getRecordCountByUserId(int userId);
    
    @Query("SELECT SUM(distance) FROM records WHERE userId = :userId")
    float getTotalDistanceByUserId(int userId);
    
    @Query("SELECT SUM(duration) FROM records WHERE userId = :userId")
    long getTotalDurationByUserId(int userId);
    
    @Query("SELECT * FROM records WHERE userId = :userId AND createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    LiveData<List<RecordEntity>> getRecordsByDateRange(int userId, long startTime, long endTime);
      @Query("DELETE FROM records WHERE id = :recordId")
    void deleteRecordById(int recordId);
    
    @Query("DELETE FROM records")
    void deleteAll();
    
    @Query("SELECT SUM(points) FROM records WHERE userId = :userId")
    int getTotalPointsByUserId(int userId);
    
    @Query("SELECT * FROM records WHERE userId = :userId AND type = :type ORDER BY createdAt DESC")
    LiveData<List<RecordEntity>> getRecordsByType(int userId, String type);
    
    @Query("SELECT AVG(distance) FROM records WHERE userId = :userId")
    float getAverageDistanceByUserId(int userId);
    
    @Query("SELECT AVG(duration) FROM records WHERE userId = :userId")
    long getAverageDurationByUserId(int userId);
}
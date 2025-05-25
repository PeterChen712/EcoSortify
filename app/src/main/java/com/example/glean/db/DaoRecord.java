package com.example.glean.dao;

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
    
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY startTime DESC")
    LiveData<List<RecordEntity>> getRecordsByUserId(int userId);
    
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY startTime DESC")
    List<RecordEntity> getRecordsByUserIdSync(int userId);
    
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY startTime DESC")
    List<RecordEntity> getAllRecordsByUserIdSync(int userId);
    
    @Query("SELECT * FROM records ORDER BY startTime DESC")
    LiveData<List<RecordEntity>> getAllRecords();
    
    @Query("SELECT * FROM records ORDER BY startTime DESC")
    List<RecordEntity> getAllRecordsSync();
    
    @Query("UPDATE records SET totalDistance = totalDistance + :additionalDistance WHERE id = :recordId")
    void updateDistance(int recordId, float additionalDistance);
    
    @Query("UPDATE records SET duration = :duration WHERE id = :recordId")
    void updateDuration(int recordId, long duration);
    
    @Query("UPDATE records SET endTime = :endTime WHERE id = :recordId")
    void updateEndTime(int recordId, long endTime);
    
    @Query("UPDATE records SET totalDistance = :totalDistance, duration = :duration, endTime = :endTime WHERE id = :recordId")
    void updateRecordStats(int recordId, float totalDistance, long duration, long endTime);
    
    @Query("SELECT COUNT(*) FROM records WHERE userId = :userId")
    int getRecordCountByUserId(int userId);
    
    @Query("SELECT SUM(totalDistance) FROM records WHERE userId = :userId")
    float getTotalDistanceByUserId(int userId);
    
    @Query("SELECT SUM(duration) FROM records WHERE userId = :userId")
    long getTotalDurationByUserId(int userId);
    
    @Query("SELECT * FROM records WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    LiveData<List<RecordEntity>> getRecordsByDateRange(int userId, String startDate, String endDate);
    
    @Query("DELETE FROM records WHERE id = :recordId")
    void deleteRecordById(int recordId);
}
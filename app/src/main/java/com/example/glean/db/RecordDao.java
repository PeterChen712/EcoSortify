package com.example.glean.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.RecordEntity;

import java.util.List;

@Dao
public interface RecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RecordEntity record);

    @Update
    void update(RecordEntity record);

    @Delete
    void delete(RecordEntity record);

    @Query("SELECT * FROM records WHERE id = :recordId")
    LiveData<RecordEntity> getRecordById(int recordId);

    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<RecordEntity>> getRecordsByUserId(int userId);

    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    LiveData<List<RecordEntity>> getRecentRecordsByUserId(int userId, int limit);

    @Query("SELECT * FROM records ORDER BY createdAt DESC")
    LiveData<List<RecordEntity>> getAllRecords();

    @Query("SELECT COUNT(*) FROM records WHERE userId = :userId")
    int getRecordCountByUserId(int userId);

    @Query("SELECT SUM(distance) FROM records WHERE userId = :userId")
    float getTotalDistanceByUserId(int userId);

    @Query("SELECT SUM(duration) FROM records WHERE userId = :userId")
    long getTotalDurationByUserId(int userId);

    @Query("SELECT AVG(distance) FROM records WHERE userId = :userId")
    float getAverageDistanceByUserId(int userId);

    @Query("SELECT MAX(distance) FROM records WHERE userId = :userId")
    float getMaxDistanceByUserId(int userId);

    @Query("SELECT * FROM records WHERE userId = :userId AND createdAt >= :startDate AND createdAt <= :endDate")
    LiveData<List<RecordEntity>> getRecordsByUserAndDateRange(int userId, long startDate, long endDate);

    @Query("SELECT * FROM records WHERE createdAt >= :startDate AND createdAt <= :endDate ORDER BY createdAt DESC")
    LiveData<List<RecordEntity>> getRecordsByDateRange(long startDate, long endDate);

    @Query("DELETE FROM records WHERE userId = :userId")
    void deleteRecordsByUserId(int userId);

    @Query("DELETE FROM records WHERE id = :recordId")
    void deleteById(int recordId);

    @Query("SELECT COUNT(*) FROM records WHERE userId = :userId AND createdAt >= :startDate AND createdAt <= :endDate")
    int getRecordCountByUserAndDateRange(int userId, long startDate, long endDate);

    @Query("SELECT SUM(distance) FROM records WHERE userId = :userId AND createdAt >= :startDate AND createdAt <= :endDate")
    float getTotalDistanceByUserAndDateRange(int userId, long startDate, long endDate);
}
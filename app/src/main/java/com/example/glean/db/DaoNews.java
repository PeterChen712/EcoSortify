package com.example.glean.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.NewsEntity;

import java.util.List;

@Dao
public interface DaoNews {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(NewsEntity news);

    @Update
    void update(NewsEntity news);

    @Delete
    void delete(NewsEntity news);

    @Query("DELETE FROM news")
    void deleteAll();

    @Query("SELECT * FROM news ORDER BY date DESC")
    LiveData<List<NewsEntity>> getAllNews();
    
    @Query("SELECT * FROM news WHERE isSaved = 1 ORDER BY date DESC")
    LiveData<List<NewsEntity>> getSavedNews();
    
    @Query("SELECT * FROM news WHERE id = :newsId LIMIT 1")
    LiveData<NewsEntity> getNewsById(int newsId);
    
    @Query("DELETE FROM news WHERE isSaved = 0 AND date < :timestamp")
    void deleteOldNews(long timestamp);
}
package com.example.glean.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.NewsEntity;

import java.util.List;

@Dao
public interface NewsDao {
    
    @Insert
    long insert(NewsEntity news);
    
    @Update
    void update(NewsEntity news);
    
    @Delete
    void delete(NewsEntity news);
    
    @Query("SELECT * FROM news WHERE id = :newsId")
    LiveData<NewsEntity> getNewsById(int newsId);
    
    @Query("SELECT * FROM news WHERE id = :newsId")
    NewsEntity getNewsByIdSync(int newsId);
    
    @Query("SELECT * FROM news ORDER BY createdAt DESC")
    LiveData<List<NewsEntity>> getAllNews();
    
    @Query("SELECT * FROM news ORDER BY createdAt DESC")
    List<NewsEntity> getAllNewsSync();
    
    @Query("DELETE FROM news")
    void deleteAll();
    
    @Query("SELECT * FROM news WHERE category = :category ORDER BY createdAt DESC")
    LiveData<List<NewsEntity>> getNewsByCategory(String category);
    
    @Query("SELECT * FROM news WHERE title LIKE '%' || :query || '%' OR preview LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    LiveData<List<NewsEntity>> searchNews(String query);
    
    @Query("SELECT * FROM news WHERE source = :source ORDER BY createdAt DESC")
    LiveData<List<NewsEntity>> getNewsBySource(String source);
    
    @Query("SELECT DISTINCT category FROM news WHERE category IS NOT NULL AND category != '' ORDER BY category")
    LiveData<List<String>> getAllCategories();
    
    @Query("SELECT DISTINCT source FROM news WHERE source IS NOT NULL AND source != '' ORDER BY source")
    LiveData<List<String>> getAllSources();
    
    @Query("DELETE FROM news WHERE id = :newsId")
    void deleteNewsById(int newsId);
    
    @Query("SELECT COUNT(*) FROM news")
    int getNewsCount();
}
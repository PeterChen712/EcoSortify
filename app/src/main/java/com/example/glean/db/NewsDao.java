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
    
    @Query("SELECT * FROM news WHERE category = :category ORDER BY createdAt DESC")
    List<NewsEntity> getNewsByCategory(String category);
    
    @Insert
    long[] insertNews(List<NewsEntity> news);
    
    @Delete
    void deleteNews(NewsEntity news);
    
    @Query("DELETE FROM news")
    void deleteAllNews();
}
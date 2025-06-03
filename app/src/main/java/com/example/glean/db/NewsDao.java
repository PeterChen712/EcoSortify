package com.example.glean.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.NewsItem;

import java.util.List;

@Dao
public interface NewsDao {
    
    // ===== INSERT OPERATIONS =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNews(NewsItem newsItem);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNewsList(List<NewsItem> newsList);
    
    // ===== UPDATE & DELETE OPERATIONS =====
    @Update
    void updateNews(NewsItem newsItem);
    
    @Delete
    void deleteNews(NewsItem newsItem);
    
    // ===== BASIC QUERY OPERATIONS =====
    @Query("SELECT * FROM news ORDER BY timestamp DESC")
    List<NewsItem> getAllNews();
    
    @Query("SELECT * FROM news WHERE isRead = 0 ORDER BY timestamp DESC")
    List<NewsItem> getUnreadNews();
    
    @Query("SELECT * FROM news WHERE isFavorite = 1 ORDER BY timestamp DESC")
    List<NewsItem> getFavoriteNews();
    
    @Query("SELECT * FROM news WHERE category = :category ORDER BY timestamp DESC")
    List<NewsItem> getNewsByCategory(String category);
    
    // ===== SPECIFIC ITEM QUERIES =====
    @Query("SELECT * FROM news WHERE id = :newsId LIMIT 1")
    NewsItem getNewsById(int newsId);
    
    @Query("SELECT * FROM news WHERE url = :url LIMIT 1")
    NewsItem getNewsByUrl(String url);
    
    // ===== SEARCH OPERATIONS =====
    @Query("SELECT * FROM news WHERE title LIKE '%' || :query || '%' OR preview LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    List<NewsItem> searchNews(String query);
    
    // ===== COUNT OPERATIONS =====
    @Query("SELECT COUNT(*) FROM news WHERE isRead = 0")
    int getUnreadCount();
    
    @Query("SELECT COUNT(*) FROM news")
    int getTotalNewsCount();
    
    // ===== CATEGORY OPERATIONS =====
    @Query("SELECT DISTINCT category FROM news WHERE category IS NOT NULL AND category != '' ORDER BY category ASC")
    List<String> getAllCategories();
    
    // ===== TIME-BASED QUERIES =====
    @Query("SELECT * FROM news WHERE timestamp > :since ORDER BY timestamp DESC")
    List<NewsItem> getNewsSince(long since);
    
    @Query("SELECT * FROM news WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<NewsItem> getNewsBetween(long start, long end);
    
    // ===== OFFLINE OPERATIONS =====
    @Query("SELECT * FROM news WHERE isOfflineAvailable = 1 ORDER BY timestamp DESC")
    List<NewsItem> getOfflineNews();
    
    // ===== DELETE OPERATIONS =====
    @Query("DELETE FROM news WHERE timestamp < :before")
    void deleteOldNews(long before);
    
    @Query("DELETE FROM news")
    void deleteAllNews();
    
    @Query("DELETE FROM news WHERE category = :category")
    void deleteNewsByCategory(String category);
    
    // ===== STATISTICS QUERIES =====
    @Query("SELECT COUNT(*) FROM news WHERE timestamp > :since")
    int getNewsCountSince(long since);
    
    @Query("SELECT COUNT(*) FROM news WHERE isRead = 1 AND timestamp > :since")
    int getReadNewsCountSince(long since);
}

package com.example.glean.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.glean.db.AppDatabase;
import com.example.glean.model.NewsItem;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cache Manager for handling offline news functionality
 * Based on EcoSortify best practices for offline data management
 */
public class NewsCacheManager {
    private static final String TAG = "NewsCacheManager";
    private static final String PREFS_NAME = "news_cache_prefs";
    private static final String KEY_LAST_UPDATE = "last_update_timestamp";
    private static final String KEY_CACHE_SIZE = "cache_size";
    private static final long CACHE_EXPIRY_TIME = 24 * 60 * 60 * 1000; // 24 hours
    private static final int MAX_CACHE_SIZE = 100; // Maximum articles to cache
    
    private final Context context;
    private final AppDatabase database;
    private final SharedPreferences preferences;
    private final ExecutorService executor;
    
    public NewsCacheManager(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Cache news articles to local database
     */
    public void cacheArticles(List<NewsItem> articles, CacheCallback callback) {
        executor.execute(() -> {
            try {
                // Clear old cache if needed
                cleanOldCache();
                
                // Limit articles to cache
                List<NewsItem> articlesToCache = articles.size() > MAX_CACHE_SIZE ? 
                    articles.subList(0, MAX_CACHE_SIZE) : articles;
                
                // Mark articles as offline available
                for (NewsItem article : articlesToCache) {
                    article.setOfflineAvailable(true);
                    database.newsDao().insertNews(article);
                }
                
                // Update cache metadata
                updateCacheMetadata(articlesToCache.size());
                
                Log.d(TAG, "Successfully cached " + articlesToCache.size() + " articles");
                
                if (callback != null) {
                    callback.onCacheSuccess(articlesToCache.size());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error caching articles", e);
                if (callback != null) {
                    callback.onCacheError("Failed to cache articles: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Get cached articles for offline viewing
     */
    public void getCachedArticles(CacheRetrievalCallback callback) {
        executor.execute(() -> {
            try {
                List<NewsItem> cachedArticles = database.newsDao().getOfflineNews();
                
                if (callback != null) {
                    callback.onCachedArticlesRetrieved(cachedArticles);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving cached articles", e);
                if (callback != null) {
                    callback.onRetrievalError("Failed to retrieve cached articles: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Get validated cached articles (removes invalid URLs)
     */
    public void getValidatedCachedArticles(CacheRetrievalCallback callback) {
        executor.execute(() -> {
            try {
                List<NewsItem> allCachedItems = database.newsDao().getAllNews();
                
                if (allCachedItems.isEmpty()) {
                    callback.onCachedArticlesRetrieved(allCachedItems);
                    return;
                }
                
                Log.d(TAG, "Validating " + allCachedItems.size() + " cached articles...");
                
                // Validate cached articles
                NewsValidator.validateCachedArticles(allCachedItems, new NewsValidator.ValidationCallback() {
                    @Override
                    public void onValidationComplete(List<com.example.glean.model.Article> validArticles, NewsValidator.ValidationStats stats) {
                        // Convert back to NewsItems
                        List<NewsItem> validNewsItems = new java.util.ArrayList<>();
                        List<Integer> invalidIds = new java.util.ArrayList<>();
                        
                        // Create a map for quick lookup
                        java.util.Map<String, NewsItem> urlToItem = new java.util.HashMap<>();
                        for (NewsItem item : allCachedItems) {
                            urlToItem.put(item.getUrl(), item);
                        }
                        
                        // Add valid articles
                        for (com.example.glean.model.Article article : validArticles) {
                            NewsItem item = urlToItem.get(article.getUrl());
                            if (item != null) {
                                validNewsItems.add(item);
                            }
                        }
                        
                        // Identify invalid articles for cleanup
                        for (NewsItem item : allCachedItems) {
                            boolean found = false;
                            for (com.example.glean.model.Article article : validArticles) {
                                if (article.getUrl().equals(item.getUrl())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                invalidIds.add(item.getId());
                            }
                        }
                        
                        // Cleanup invalid articles asynchronously
                        if (!invalidIds.isEmpty()) {
                            cleanupInvalidArticles(invalidIds);
                            Log.i(TAG, "Cleaned up " + invalidIds.size() + " invalid cached articles");
                        }
                        
                        Log.i(TAG, "Cache validation: " + stats.getSummary());
                        callback.onCachedArticlesRetrieved(validNewsItems);
                    }

                    @Override
                    public void onValidationProgress(int processed, int total) {
                        // Progress can be logged but not necessarily reported for cache
                        Log.d(TAG, "Cache validation progress: " + processed + "/" + total);
                    }

                    @Override
                    public void onValidationError(String error) {
                        Log.w(TAG, "Cache validation error, returning all cached items: " + error);
                        callback.onCachedArticlesRetrieved(allCachedItems);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error validating cached articles", e);
                callback.onRetrievalError("Failed to validate cache: " + e.getMessage());
            }
        });
    }
    
    /**
     * Cleanup invalid articles from cache
     */
    private void cleanupInvalidArticles(List<Integer> invalidIds) {
        executor.execute(() -> {
            try {
                for (Integer id : invalidIds) {
                    database.newsDao().deleteById(id);
                }
                Log.d(TAG, "Cleaned up " + invalidIds.size() + " invalid articles from cache");
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up invalid articles", e);
            }
        });
    }
    
    /**
     * Check if cache is still valid
     */
    public boolean isCacheValid() {
        long lastUpdate = preferences.getLong(KEY_LAST_UPDATE, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastUpdate) < CACHE_EXPIRY_TIME;
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        long lastUpdate = preferences.getLong(KEY_LAST_UPDATE, 0);
        int cacheSize = preferences.getInt(KEY_CACHE_SIZE, 0);
        boolean isValid = isCacheValid();
        
        return new CacheStats(lastUpdate, cacheSize, isValid);
    }
    
    /**
     * Clear all cached articles
     */
    public void clearCache(CacheCallback callback) {
        executor.execute(() -> {
            try {
                database.newsDao().deleteAllNews();
                
                // Clear cache metadata
                preferences.edit()
                    .remove(KEY_LAST_UPDATE)
                    .remove(KEY_CACHE_SIZE)
                    .apply();
                
                Log.d(TAG, "Cache cleared successfully");
                
                if (callback != null) {
                    callback.onCacheSuccess(0);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error clearing cache", e);
                if (callback != null) {
                    callback.onCacheError("Failed to clear cache: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Clean old cache entries
     */
    private void cleanOldCache() {
        try {
            long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days
            database.newsDao().deleteOldNews(cutoffTime);
            Log.d(TAG, "Old cache entries cleaned");
        } catch (Exception e) {
            Log.w(TAG, "Error cleaning old cache", e);
        }
    }
    
    /**
     * Update cache metadata
     */
    private void updateCacheMetadata(int cacheSize) {
        preferences.edit()
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .putInt(KEY_CACHE_SIZE, cacheSize)
            .apply();
    }
    
    /**
     * Check if device has limited storage
     */
    public boolean hasLimitedStorage() {
        // Simple check - in real implementation, check available storage
        return getCacheStats().getCacheSize() > MAX_CACHE_SIZE;
    }
    
    // Callback interfaces
    public interface CacheCallback {
        void onCacheSuccess(int cachedCount);
        void onCacheError(String error);
    }
    
    public interface CacheRetrievalCallback {
        void onCachedArticlesRetrieved(List<NewsItem> articles);
        void onRetrievalError(String error);
    }
    
    // Cache statistics class
    public static class CacheStats {
        private final long lastUpdateTime;
        private final int cacheSize;
        private final boolean isValid;
        
        public CacheStats(long lastUpdateTime, int cacheSize, boolean isValid) {
            this.lastUpdateTime = lastUpdateTime;
            this.cacheSize = cacheSize;
            this.isValid = isValid;
        }
        
        public long getLastUpdateTime() { return lastUpdateTime; }
        public int getCacheSize() { return cacheSize; }
        public boolean isValid() { return isValid; }
        
        public String getFormattedLastUpdate() {
            if (lastUpdateTime == 0) return "Never";
            
            long timeDiff = System.currentTimeMillis() - lastUpdateTime;
            long hours = timeDiff / (60 * 60 * 1000);
            
            if (hours < 1) {
                return "Less than 1 hour ago";
            } else if (hours < 24) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else {
                long days = hours / 24;
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            }
        }
    }
}

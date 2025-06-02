package com.example.glean.util;

import com.example.glean.BuildConfig;

/**
 * Central class to manage all API keys and configurations
 */
public class ApiConfig {
    
    // API Keys
    public static String getNewsApiKey() {
        // In production, store in BuildConfig or encrypted preferences
        return BuildConfig.NEWS_API_KEY; // Define in build.gradle
    }
    
    public static String getMapsApiKey() {
        return BuildConfig.MAPS_API_KEY;
    }
    
    public static String getBackupApiKey() {
        return BuildConfig.BACKUP_API_KEY;
    }
    
    // API Endpoints
    public static final String NEWS_BASE_URL = "https://newsapi.org/v2/";
    
    // Default configurations
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String DEFAULT_SORT_BY = "publishedAt";
    
    // Cache settings
    public static final long CACHE_MAX_AGE = 60 * 60; // 1 hour
    public static final long CACHE_STALE_AGE = 60 * 60 * 24 * 7; // 1 week
    
    // Request timeouts
    public static final int CONNECT_TIMEOUT = 30; // seconds
    public static final int READ_TIMEOUT = 30; // seconds
    public static final int WRITE_TIMEOUT = 30; // seconds
}
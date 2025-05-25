package com.example.glean.util;

import com.example.glean.BuildConfig;

/**
 * Central class to manage all API keys and configurations
 */
public class ApiConfig {
    
    // Google Maps API
    public static String getMapsApiKey() {
        return BuildConfig.MAPS_API_KEY;
    }
    
    // News API
    public static String getNewsApiKey() {
        return BuildConfig.NEWS_API_KEY;
    }
    
    // Base URLs
    public static final String NEWS_API_BASE_URL = "https://newsapi.org/";
    public static final String DEFAULT_NEWS_QUERY = "environmental OR recycling OR climate";
    
    // Other API constants
    public static final int NEWS_PAGE_SIZE = 20;
    public static final String NEWS_SORT_BY = "publishedAt";
    public static final String NEWS_LANGUAGE = "en";
}
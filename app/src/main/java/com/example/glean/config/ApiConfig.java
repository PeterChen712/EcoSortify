package com.example.glean.config;

import com.example.glean.BuildConfig;

/**
 * Central class to manage all API keys and configurations
 */
public class ApiConfig {
    
    // Maps API Key
    public static String getMapsApiKey() {
        return BuildConfig.MAPS_API_KEY;
    }
    
    // Gemini API Key
    public static String getGeminiApiKey() {
        return BuildConfig.GEMINI_API_KEY;
    }
    
    // News API Key
    public static String getNewsApiKey() {
        return BuildConfig.NEWS_API_KEY;
    }
    
    // Check if API keys are configured
    public static boolean isMapsApiConfigured() {
        String key = getMapsApiKey();
        return key != null && !key.isEmpty() && !key.equals("your_maps_api_key_here");
    }
    
    public static boolean isGeminiApiConfigured() {
        String key = getGeminiApiKey();
        return key != null && !key.isEmpty() && !key.equals("your_gemini_api_key_here");
    }
    
    public static boolean isNewsApiConfigured() {
        String key = getNewsApiKey();
        return key != null && !key.isEmpty() && !key.equals("your_news_api_key_here");
    }
}

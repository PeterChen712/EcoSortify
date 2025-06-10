package com.example.glean.config;

import com.example.glean.BuildConfig;

/**
 * Central class to manage all API keys and configurations
 */
public class ApiConfig {
    
    // NewsAPI.org Configuration
    private static final String NEWS_API_BASE_URL = "https://newsapi.org/v2/";
    private static final String NEWS_API_EVERYTHING_ENDPOINT = "everything";
    private static final String NEWS_API_TOP_HEADLINES_ENDPOINT = "top-headlines";
    
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
    
    // NewsAPI URLs
    public static String getNewsApiBaseUrl() {
        return NEWS_API_BASE_URL;
    }
    
    public static String getNewsEverythingUrl(String query, String language) {
        return NEWS_API_BASE_URL + NEWS_API_EVERYTHING_ENDPOINT + 
               "?q=" + query + 
               "&language=" + language + 
               "&apiKey=" + getNewsApiKey();
    }
    
    public static String getNewsTopHeadlinesUrl(String country, String category) {
        return NEWS_API_BASE_URL + NEWS_API_TOP_HEADLINES_ENDPOINT + 
               "?country=" + country + 
               "&category=" + category + 
               "&apiKey=" + getNewsApiKey();
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
    
    public static boolean isNewsApiKeyAvailable() {
        return isNewsApiConfigured();
    }
      // NewsAPI Parameters
    public static class NewsParams {
        public static final String LANGUAGE_INDONESIAN = "id";
        public static final String LANGUAGE_ENGLISH = "en";
        public static final String SORT_BY_PUBLISHED_AT = "publishedAt";
        public static final String SORT_BY_RELEVANCY = "relevancy";
        public static final String SORT_BY_POPULARITY = "popularity";
        public static final String COUNTRY_INDONESIA = "id";
        public static final String CATEGORY_SCIENCE = "science";
        public static final String CATEGORY_TECHNOLOGY = "technology";
        public static final String CATEGORY_GENERAL = "general";
    }
}
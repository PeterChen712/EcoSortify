package com.example.glean.helper;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * URL Validator for News Articles
 * Validates URL accessibility and filters out 404/invalid links
 */
public class UrlValidator {
    private static final String TAG = "UrlValidator";
    
    // Timeout configurations
    private static final int CONNECT_TIMEOUT = 5000; // 5 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds
    private static final int MAX_REDIRECTS = 3;
    
    // Known problematic domains to skip
    private static final String[] BLOCKED_DOMAINS = {
        "removed.com",
        "deleted.com",
        "unavailable.com",
        "example.com",
        "test.com",
        "placeholder.com"
    };
    
    // Required environmental keywords for content relevance
    private static final String[] ENVIRONMENTAL_KEYWORDS = {
        // Indonesian keywords
        "lingkungan", "sampah", "daur ulang", "limbah", "plastik", "polusi",
        "energi", "konservasi", "iklim", "hijau", "ramah lingkungan", "berkelanjutan",
        "pengelolaan", "pencemaran", "biodiversitas", "ekosistem", "karbon",
        "emisi", "pembakaran", "organik", "kompos", "reduce", "reuse", "recycle",
        
        // English keywords (for international articles)
        "environment", "waste", "recycling", "pollution", "plastic", "green",
        "sustainable", "climate", "conservation", "ecosystem", "carbon", "emission",
        "renewable", "organic", "biodiversity", "eco-friendly", "environmental"
    };

    public interface ValidationCallback {
        void onValidationComplete(List<ValidatedUrl> validUrls);
        void onValidationError(String error);
    }

    public static class ValidatedUrl {
        private final String url;
        private final boolean isValid;
        private final int responseCode;
        private final String finalUrl; // After redirects
        private final String error;

        public ValidatedUrl(String url, boolean isValid, int responseCode, String finalUrl, String error) {
            this.url = url;
            this.isValid = isValid;
            this.responseCode = responseCode;
            this.finalUrl = finalUrl;
            this.error = error;
        }

        // Getters
        public String getUrl() { return url; }
        public boolean isValid() { return isValid; }
        public int getResponseCode() { return responseCode; }
        public String getFinalUrl() { return finalUrl; }
        public String getError() { return error; }
    }

    /**
     * Validate multiple URLs concurrently
     */
    public static void validateUrls(List<String> urls, ValidationCallback callback) {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(urls.size(), 5));
        List<Future<ValidatedUrl>> futures = new ArrayList<>();
        
        try {
            // Submit validation tasks
            for (String url : urls) {
                futures.add(executor.submit(() -> validateSingleUrl(url)));
            }
            
            // Collect results
            List<ValidatedUrl> results = new ArrayList<>();
            for (Future<ValidatedUrl> future : futures) {
                try {
                    ValidatedUrl result = future.get(15, TimeUnit.SECONDS);
                    results.add(result);
                } catch (Exception e) {
                    Log.w(TAG, "Validation timeout for URL", e);
                    results.add(new ValidatedUrl(null, false, 0, null, "Timeout"));
                }
            }
            
            callback.onValidationComplete(results);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during URL validation", e);
            callback.onValidationError(e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Validate single URL
     */
    private static ValidatedUrl validateSingleUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return new ValidatedUrl(urlString, false, 0, null, "Empty URL");
        }

        // Pre-validation checks
        if (!isValidUrlFormat(urlString)) {
            return new ValidatedUrl(urlString, false, 0, null, "Invalid URL format");
        }

        if (isBlockedDomain(urlString)) {
            return new ValidatedUrl(urlString, false, 0, null, "Blocked domain");
        }

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Configure connection
            connection.setRequestMethod("HEAD"); // Faster than GET
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            
            // Set user agent to avoid blocking
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Android) Ecosortify/1.0 NewsValidator");
            
            // Get response
            int responseCode = connection.getResponseCode();
            String finalUrl = connection.getURL().toString();
            
            // Check if response is successful
            boolean isValid = isSuccessfulResponse(responseCode);
            
            connection.disconnect();
            
            return new ValidatedUrl(urlString, isValid, responseCode, finalUrl, 
                isValid ? null : "HTTP " + responseCode);
                
        } catch (MalformedURLException e) {
            return new ValidatedUrl(urlString, false, 0, null, "Malformed URL");
        } catch (IOException e) {
            return new ValidatedUrl(urlString, false, 0, null, "Connection error: " + e.getMessage());
        } catch (Exception e) {
            return new ValidatedUrl(urlString, false, 0, null, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Check if URL format is valid
     */
    private static boolean isValidUrlFormat(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        url = url.trim().toLowerCase();
        
        // Must start with http or https
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        
        // Basic URL structure validation
        if (!url.contains(".") || url.length() < 10) {
            return false;
        }
        
        // Check for invalid characters
        if (url.contains(" ") || url.contains("<") || url.contains(">")) {
            return false;
        }
        
        return true;
    }

    /**
     * Check if domain is in blocked list
     */
    private static boolean isBlockedDomain(String url) {
        String lowerUrl = url.toLowerCase();
        
        for (String blockedDomain : BLOCKED_DOMAINS) {
            if (lowerUrl.contains(blockedDomain)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if HTTP response code indicates success
     */
    private static boolean isSuccessfulResponse(int responseCode) {
        // Accept 2xx success codes
        return responseCode >= 200 && responseCode < 300;
    }

    /**
     * Validate content relevance to environmental topics
     */
    public static boolean isEnvironmentallyRelevant(String title, String description) {
        if (title == null && description == null) {
            return false;
        }
        
        String combinedText = ((title != null ? title : "") + " " + 
                              (description != null ? description : "")).toLowerCase();
        
        // Must contain at least one environmental keyword
        for (String keyword : ENVIRONMENTAL_KEYWORDS) {
            if (combinedText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Calculate relevance score (0-100)
     */
    public static int calculateRelevanceScore(String title, String description) {
        if (title == null && description == null) {
            return 0;
        }
        
        String combinedText = ((title != null ? title : "") + " " + 
                              (description != null ? description : "")).toLowerCase();
        
        int score = 0;
        int keywordMatches = 0;
        
        // Count keyword matches
        for (String keyword : ENVIRONMENTAL_KEYWORDS) {
            if (combinedText.contains(keyword.toLowerCase())) {
                keywordMatches++;
                
                // High priority keywords get more points
                if (isHighPriorityKeyword(keyword)) {
                    score += 15;
                } else {
                    score += 10;
                }
            }
        }
        
        // Bonus for title relevance
        if (title != null) {
            String titleLower = title.toLowerCase();
            for (String keyword : ENVIRONMENTAL_KEYWORDS) {
                if (titleLower.contains(keyword.toLowerCase())) {
                    score += 5; // Title matches are more important
                }
            }
        }
        
        // Cap at 100
        return Math.min(score, 100);
    }

    /**
     * Check if keyword is high priority for environmental content
     */
    private static boolean isHighPriorityKeyword(String keyword) {
        String[] highPriority = {
            "lingkungan", "environment", "sampah", "waste", "daur ulang", "recycling",
            "plastik", "plastic", "polusi", "pollution", "berkelanjutan", "sustainable",
            "iklim", "climate", "hijau", "green"
        };
        
        for (String priority : highPriority) {
            if (keyword.toLowerCase().equals(priority.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Clean and fix URL format
     */
    public static String cleanUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        url = url.trim();
        
        // Add https:// if missing protocol
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        // Remove fragments and unnecessary parameters
        if (url.contains("#")) {
            url = url.substring(0, url.indexOf("#"));
        }
          return url;
    }

    /**
     * Quick check if an article URL is likely to be valid (without making HTTP requests)
     * Used for pre-validation before expensive network operations
     */
    public static boolean isProbablyValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String lowerUrl = url.toLowerCase();
        
        // Check for blocked domains
        for (String domain : BLOCKED_DOMAINS) {
            if (lowerUrl.contains(domain)) {
                return false;
            }
        }
        
        // Common patterns of invalid URLs
        String[] invalidPatterns = {
            "removed", "deleted", "unavailable", "not-found", 
            "error", "404", "missing", "expired", "broken",
            "placeholder", "example", "test"
        };
        
        for (String pattern : invalidPatterns) {
            if (lowerUrl.contains(pattern)) {
                return false;
            }
        }
        
        // Basic URL format validation
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}

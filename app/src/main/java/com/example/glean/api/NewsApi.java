package com.example.glean.api;

import android.util.Log;

import com.example.glean.config.ApiConfig;
import com.example.glean.model.Article;
import com.example.glean.helper.NewsValidator;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * NewsApi implementation based on EcoSortify best practices
 * Handles environmental news from NewsAPI.org with Indonesian language support
 */
public class NewsApi {
    private static final String TAG = "NewsApi";
    
    private final OkHttpClient client;
    private final Gson gson;
    private final SimpleDateFormat inputDateFormat;
    private final SimpleDateFormat outputDateFormat;

    public NewsApi() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        // Format input dari NewsAPI: "2024-05-29T10:30:00Z"
        this.inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        // Format output untuk display: "29 Mei 2024"
        this.outputDateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
    }    public interface NewsCallback {
        void onSuccess(List<Article> articles);
        void onError(String error);
    }

    // Enhanced callback with validation stats
    public interface ValidatedNewsCallback {
        void onSuccess(List<Article> validArticles, NewsValidator.ValidationStats stats);
        void onValidationProgress(int processed, int total);
        void onError(String error);
    }

    // Keep old interface for compatibility
    public interface ArticlesCallback {
        void onSuccess(List<Article> articles);
        void onError(String error);
    }

    public void getEnvironmentalArticles(ArticlesCallback callback) {
        getEnvironmentalArticles(new NewsCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                callback.onSuccess(articles);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getEnvironmentalArticles(NewsCallback callback) {
        new Thread(() -> {
            try {
                // Check if API key is available
                if (!ApiConfig.isNewsApiKeyAvailable()) {
                    callback.onError("News API key not configured");
                    return;
                }
                
                // Build search query for environmental articles in Indonesian
                String query = "daur ulang OR sampah OR lingkungan OR " +
                        "\"pengelolaan sampah\" OR \"limbah plastik\" OR \"pelestarian lingkungan\"";
                
                // URL encode the query
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                
                String url = ApiConfig.getNewsEverythingUrl(encodedQuery, ApiConfig.NewsParams.LANGUAGE_INDONESIAN) +
                        "&sortBy=publishedAt&pageSize=15";

                Log.d(TAG, "Fetching articles from: " + url.replace(ApiConfig.getNewsApiKey(), "***API_KEY***"));

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response received, parsing articles...");
                    List<Article> articles = parseArticlesResponse(responseBody);
                    Log.d(TAG, "Parsed " + articles.size() + " articles");
                    callback.onSuccess(articles);
                } else {
                    String errorMessage = "Failed to fetch articles: " + response.code();
                    if (response.body() != null) {
                        errorMessage += " - " + response.body().string();
                    }
                    Log.e(TAG, errorMessage);
                    callback.onError(errorMessage);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error fetching articles", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error", e);
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }

    private List<Article> parseArticlesResponse(String response) {
        List<Article> articles = new ArrayList<>();
        
        try {
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            String status = jsonObject.get("status").getAsString();
            
            if ("ok".equals(status)) {
                JsonArray articlesJson = jsonObject.getAsJsonArray("articles");
                Log.d(TAG, "Found " + articlesJson.size() + " articles in response");
                
                for (int i = 0; i < articlesJson.size(); i++) {
                    JsonObject articleJson = articlesJson.get(i).getAsJsonObject();
                    
                    String title = getJsonString(articleJson, "title", "No Title");
                    String description = getJsonString(articleJson, "description", "");
                    String url = getJsonString(articleJson, "url", "");
                    String publishedAt = getJsonString(articleJson, "publishedAt", "");
                    
                    // Skip articles with invalid title or URL
                    if (title.equals("No Title") || title.equals("[Removed]") || 
                        url.isEmpty() || url.equals("[Removed]")) {
                        continue;
                    }
                    
                    // Get source
                    JsonObject sourceJson = articleJson.has("source") ? 
                            articleJson.getAsJsonObject("source") : null;
                    String source = "Unknown";
                    if (sourceJson != null && sourceJson.has("name") && !sourceJson.get("name").isJsonNull()) {
                        source = sourceJson.get("name").getAsString();
                    }
                    
                    // Format published date
                    String formattedDate = formatPublishedDate(publishedAt);
                    
                    // Check if the title appears to be in a non-Indonesian language
                    boolean isIndonesian = seemsIndonesian(title);
                    String cleanedTitle = title;
                    String cleanedDescription = description;
                    
                    if (!isIndonesian) {
                        cleanedTitle = "[Terjemahan] " + cleanForeignText(title);
                        if (!description.isEmpty()) {
                            cleanedDescription = "[Terjemahan] " + cleanForeignText(description);
                        }
                    }
                    
                    // Create article using the correct constructor
                    Article article = new Article(cleanedTitle, cleanedDescription, url, source, formattedDate);
                    
                    // Set additional fields
                    article.setPublishedAt(publishedAt);
                    article.setUrlToImage(getJsonString(articleJson, "urlToImage", ""));
                    article.setContent(getJsonString(articleJson, "content", ""));
                    article.setIndonesian(isIndonesian);
                    article.setCleanedTitle(cleanedTitle);
                    article.setCleanedDescription(cleanedDescription);
                    
                    // Set publishedAt Date if needed
                    Date publishedDate = parseDate(publishedAt);
                    if (publishedDate != null) {
                        article.setPublishedDate(publishedDate);
                    }
                    
                    articles.add(article);
                    
                    Log.d(TAG, "Added article: " + cleanedTitle);
                }
            } else {
                Log.w(TAG, "API returned non-OK status: " + status);
                if (jsonObject.has("message")) {
                    Log.w(TAG, "Error message: " + jsonObject.get("message").getAsString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing articles response", e);
        }
        
        return articles;
    }
    
    /**
     * Format published date from NewsAPI format to readable Indonesian format
     */
    private String formatPublishedDate(String publishedAt) {
        if (publishedAt == null || publishedAt.isEmpty()) {
            return "Hari ini";
        }
        
        try {
            Date date = inputDateFormat.parse(publishedAt);
            return outputDateFormat.format(date);
        } catch (ParseException e) {
            Log.w(TAG, "Error parsing date: " + publishedAt, e);
            return "Hari ini";
        }
    }
    
    /**
     * Parse date string to Date object
     */
    private Date parseDate(String publishedAt) {
        if (publishedAt == null || publishedAt.isEmpty()) {
            return new Date(); // Return current date as fallback
        }
        
        try {
            return inputDateFormat.parse(publishedAt);
        } catch (ParseException e) {
            Log.w(TAG, "Error parsing date to Date object: " + publishedAt, e);
            return new Date(); // Return current date as fallback
        }
    }
    
    /**
     * Safely get string value from JsonObject
     */
    private String getJsonString(JsonObject jsonObject, String key, String defaultValue) {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            return jsonObject.get(key).getAsString();
        }
        return defaultValue;
    }
    
    /**
     * Very basic check if text appears to be in Indonesian based on common words
     */
    private boolean seemsIndonesian(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        
        String lowerText = text.toLowerCase();
        
        String[] indonesianMarkers = {"yang", "dari", "untuk", "dan", "dengan", "di", "ini", "itu",
                "pada", "tidak", "adalah", "dalam", "akan", "oleh", "telah", "juga", "sampah", "lingkungan"};
        
        for (String marker : indonesianMarkers) {
            if (lowerText.contains(" " + marker + " ") || 
                lowerText.startsWith(marker + " ") || 
                lowerText.endsWith(" " + marker)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Simple text cleaning for translation
     */
    private String cleanForeignText(String text) {
        String result = text;
        
        String[][] translations = {
            {"waste", "sampah"},
            {"plastic", "plastik"},
            {"recycle", "daur ulang"},
            {"environment", "lingkungan"},
            {"pollution", "polusi"},
            {"management", "pengelolaan"},
            {"garbage", "sampah"},
            {"trash", "sampah"},
            {"hazardous", "berbahaya"},
            {"sustainable", "berkelanjutan"},
            {"biodegradable", "dapat terurai"}
        };
        
        for (String[] pair : translations) {
            result = result.replaceAll("(?i)\\b" + pair[0] + "\\b", pair[1]);
        }
        
        return result;
    }
      /**
     * Get fallback sample articles when API fails or no internet
     * Returns empty list to force proper error handling
     */
    public static List<Article> getSampleArticles() {
        // Return empty list - no dummy data
        return new ArrayList<>();
    }
    
    /**
     * Fetch environmental articles with comprehensive validation
     * This method includes URL validation and content filtering
     */
    public void getValidatedEnvironmentalArticles(ValidatedNewsCallback callback) {
        // First fetch articles normally
        getEnvironmentalArticles(new NewsCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                Log.d(TAG, "Fetched " + articles.size() + " articles, starting validation...");
                
                // Validate articles with comprehensive checks
                NewsValidator.validateArticles(articles, new NewsValidator.ValidationCallback() {
                    @Override
                    public void onValidationComplete(List<Article> validArticles, NewsValidator.ValidationStats stats) {
                        Log.i(TAG, "Validation complete: " + stats.getSummary());
                        
                        // Sort by relevance score
                        List<Article> sortedArticles = NewsValidator.sortByRelevance(validArticles);
                        
                        callback.onSuccess(sortedArticles, stats);
                    }

                    @Override
                    public void onValidationProgress(int processed, int total) {
                        callback.onValidationProgress(processed, total);
                    }

                    @Override
                    public void onValidationError(String error) {
                        Log.e(TAG, "Validation error: " + error);
                        callback.onError("Article validation failed: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching articles: " + error);
                callback.onError(error);
            }
        });
    }

    /**
     * Quick validation without URL checking (for faster loading)
     */
    public void getQuickValidatedArticles(NewsCallback callback) {
        getEnvironmentalArticles(new NewsCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                // Quick validation without URL checking
                List<Article> validArticles = NewsValidator.quickValidate(articles);
                List<Article> sortedArticles = NewsValidator.sortByRelevance(validArticles);
                
                Log.d(TAG, "Quick validation: " + validArticles.size() + "/" + articles.size() + " articles valid");
                callback.onSuccess(sortedArticles);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}

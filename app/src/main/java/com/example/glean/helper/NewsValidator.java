package com.example.glean.helper;

import android.content.Context;
import android.util.Log;

import com.example.glean.model.Article;
import com.example.glean.model.NewsItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced News Validator with URL validation and content filtering
 * Ensures only valid and relevant articles are displayed
 */
public class NewsValidator {
    private static final String TAG = "NewsValidator";
    private static final int MIN_RELEVANCE_SCORE = 20; // Minimum score for relevance
    private static final int MAX_VALIDATION_TIMEOUT = 30; // seconds

    public interface ValidationCallback {
        void onValidationComplete(List<Article> validArticles, ValidationStats stats);
        void onValidationProgress(int processed, int total);
        void onValidationError(String error);
    }

    public static class ValidationStats {
        private final int totalArticles;
        private final int validArticles;
        private final int urlFailures;
        private final int relevanceFailures;
        private final int formatFailures;
        private final long processingTimeMs;

        public ValidationStats(int totalArticles, int validArticles, int urlFailures, 
                             int relevanceFailures, int formatFailures, long processingTimeMs) {
            this.totalArticles = totalArticles;
            this.validArticles = validArticles;
            this.urlFailures = urlFailures;
            this.relevanceFailures = relevanceFailures;
            this.formatFailures = formatFailures;
            this.processingTimeMs = processingTimeMs;
        }

        // Getters
        public int getTotalArticles() { return totalArticles; }
        public int getValidArticles() { return validArticles; }
        public int getUrlFailures() { return urlFailures; }
        public int getRelevanceFailures() { return relevanceFailures; }
        public int getFormatFailures() { return formatFailures; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        
        public String getSummary() {
            return String.format("Validated %d/%d articles (%.1f%% valid) in %.2fs", 
                validArticles, totalArticles, 
                (validArticles * 100.0 / Math.max(totalArticles, 1)),
                processingTimeMs / 1000.0);
        }
    }

    /**
     * Validate articles with comprehensive checks
     */
    public static void validateArticles(List<Article> articles, ValidationCallback callback) {
        if (articles == null || articles.isEmpty()) {
            callback.onValidationComplete(new ArrayList<>(), 
                new ValidationStats(0, 0, 0, 0, 0, 0));
            return;
        }

        long startTime = System.currentTimeMillis();
        
        // Use background thread for validation
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                performValidation(articles, callback, startTime);
            } catch (Exception e) {
                Log.e(TAG, "Error during article validation", e);
                callback.onValidationError("Validation error: " + e.getMessage());
            } finally {
                executor.shutdown();
            }
        });
    }

    private static void performValidation(List<Article> articles, 
                                        ValidationCallback callback, long startTime) {
          List<Article> validArticles = new ArrayList<>();
        List<String> urlsToValidate = new ArrayList<>();
        
        final int[] formatFailures = {0};
        final int[] relevanceFailures = {0};
        int urlFailures = 0;
        
        // Phase 1: Basic validation and content filtering
        List<Article> basicValidArticles = new ArrayList<>();
        
        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            callback.onValidationProgress(i + 1, articles.size());
              // Basic format validation
            if (!article.hasValidTitle() || !article.hasValidUrl()) {
                formatFailures[0]++;
                Log.d(TAG, "Format validation failed for: " + 
                    (article.getTitle() != null ? article.getTitle() : "No title"));
                continue;
            }
            
            // Content relevance validation
            if (!article.isEnvironmentallyRelevant()) {
                relevanceFailures[0]++;
                Log.d(TAG, "Relevance validation failed for: " + article.getTitle());
                continue;
            }
            
            // Comprehensive article validation
            if (!article.isValidArticle()) {
                relevanceFailures[0]++;
                Log.d(TAG, "Article validation failed for: " + article.getTitle());
                continue;
            }
            
            basicValidArticles.add(article);
            urlsToValidate.add(article.getUrl());
        }
        
        // Phase 2: URL validation
        if (!basicValidArticles.isEmpty()) {
            UrlValidator.validateUrls(urlsToValidate, new UrlValidator.ValidationCallback() {
                @Override
                public void onValidationComplete(List<UrlValidator.ValidatedUrl> validatedUrls) {
                    int urlFailureCount = 0;
                    
                    // Match articles with their URL validation results
                    for (int i = 0; i < basicValidArticles.size() && i < validatedUrls.size(); i++) {
                        Article article = basicValidArticles.get(i);
                        UrlValidator.ValidatedUrl validatedUrl = validatedUrls.get(i);
                        
                        if (validatedUrl.isValid()) {
                            // Clean the URL if validation fixed it
                            if (validatedUrl.getFinalUrl() != null && 
                                !validatedUrl.getFinalUrl().equals(article.getUrl())) {
                                article.setUrl(validatedUrl.getFinalUrl());
                            }
                            validArticles.add(article);
                        } else {
                            urlFailureCount++;
                            Log.d(TAG, "URL validation failed for: " + article.getTitle() + 
                                " - " + validatedUrl.getError());
                        }
                    }
                    
                    // Create validation statistics
                    long processingTime = System.currentTimeMillis() - startTime;                    ValidationStats stats = new ValidationStats(
                        articles.size(),
                        validArticles.size(),
                        urlFailureCount,
                        relevanceFailures[0],
                        formatFailures[0],
                        processingTime
                    );
                    
                    Log.i(TAG, "Validation complete: " + stats.getSummary());
                    callback.onValidationComplete(validArticles, stats);
                }

                @Override
                public void onValidationError(String error) {
                    // If URL validation fails, use basic validation results
                    Log.w(TAG, "URL validation error, using basic validation: " + error);
                    
                    long processingTime = System.currentTimeMillis() - startTime;                    ValidationStats stats = new ValidationStats(
                        articles.size(),
                        basicValidArticles.size(),
                        0, // URL failures unknown
                        relevanceFailures[0],
                        formatFailures[0],
                        processingTime
                    );
                    
                    callback.onValidationComplete(basicValidArticles, stats);
                }
            });
        } else {
            // No articles passed basic validation
            long processingTime = System.currentTimeMillis() - startTime;            ValidationStats stats = new ValidationStats(
                articles.size(),
                0,
                urlFailures,
                relevanceFailures[0],
                formatFailures[0],
                processingTime
            );
            
            Log.i(TAG, "No articles passed basic validation: " + stats.getSummary());
            callback.onValidationComplete(new ArrayList<>(), stats);
        }
    }

    /**
     * Validate cached articles (remove expired/invalid URLs)
     */
    public static void validateCachedArticles(List<NewsItem> cachedItems, 
                                            ValidationCallback callback) {
        if (cachedItems == null || cachedItems.isEmpty()) {
            callback.onValidationComplete(new ArrayList<>(), 
                new ValidationStats(0, 0, 0, 0, 0, 0));
            return;
        }

        // Convert NewsItems to Articles for validation
        List<Article> articles = new ArrayList<>();
        for (NewsItem item : cachedItems) {
            Article article = new Article(
                item.getTitle(),
                item.getPreview(),
                item.getUrl(),
                item.getSource(),
                item.getDate()
            );
            articles.add(article);
        }

        validateArticles(articles, callback);
    }

    /**
     * Quick validation without URL checking (for UI responsiveness)
     */
    public static List<Article> quickValidate(List<Article> articles) {
        List<Article> validArticles = new ArrayList<>();
        
        for (Article article : articles) {
            if (article.hasValidTitle() && 
                article.hasValidUrl() && 
                article.isEnvironmentallyRelevant() &&
                article.getRelevanceScore() >= MIN_RELEVANCE_SCORE) {
                validArticles.add(article);
            }
        }
        
        return validArticles;
    }

    /**
     * Check if an article URL is likely to be problematic
     */
    public static boolean isProbablyValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String lowerUrl = url.toLowerCase();
        
        // Common patterns of invalid URLs
        String[] invalidPatterns = {
            "removed", "deleted", "unavailable", "not-found", 
            "error", "404", "missing", "expired", "broken"
        };
        
        for (String pattern : invalidPatterns) {
            if (lowerUrl.contains(pattern)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Sort articles by relevance score
     */
    public static List<Article> sortByRelevance(List<Article> articles) {
        List<Article> sortedArticles = new ArrayList<>(articles);
        sortedArticles.sort((a, b) -> Integer.compare(b.getRelevanceScore(), a.getRelevanceScore()));
        return sortedArticles;
    }

    /**
     * Filter articles by minimum relevance score
     */
    public static List<Article> filterByRelevance(List<Article> articles, int minScore) {
        List<Article> filtered = new ArrayList<>();
        
        for (Article article : articles) {
            if (article.getRelevanceScore() >= minScore) {
                filtered.add(article);
            }
        }
        
        return filtered;
    }
}

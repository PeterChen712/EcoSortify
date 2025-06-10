package com.example.glean.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.adapter.NewsAdapter;
import com.example.glean.api.NewsApi;
import com.example.glean.config.ApiConfig;
import com.example.glean.databinding.FragmentNewsBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.helper.ErrorHandler;
import com.example.glean.helper.NetworkHelper;
import com.example.glean.helper.NewsCacheManager;
import com.example.glean.model.Article;
import com.example.glean.model.NewsItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsFragment extends Fragment implements NewsAdapter.OnNewsItemClickListener, NewsAdapter.OnNewsItemLongClickListener {
    
    private FragmentNewsBinding binding;
    private NewsAdapter newsAdapter;
    private List<NewsItem> newsList;
    private AppDatabase db;
    private ExecutorService executor;
    private NewsApi newsApi;
    private NewsCacheManager cacheManager;
    private boolean isLoading = false;
    private boolean isRefreshing = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        newsApi = new NewsApi();
        cacheManager = new NewsCacheManager(requireContext());
        newsList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRecyclerView();
        setupSwipeRefresh();
        setupFab();
        
        loadNewsFromDatabase();
        checkForUpdates();
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(requireContext(), newsList, this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerViewNews.setLayoutManager(layoutManager);
        binding.recyclerViewNews.setAdapter(newsAdapter);
        binding.recyclerViewNews.setHasFixedSize(true);
        
        // Add scroll animation
        binding.recyclerViewNews.setLayoutAnimation(
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_slide_from_bottom));
        
        // Add scroll listener for FAB
        binding.recyclerViewNews.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (dy > 0) {
                    // Scrolling down - hide FAB
                    binding.fabRefresh.hide();
                } else if (dy < 0) {
                    // Scrolling up - show FAB
                    binding.fabRefresh.show();
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.environmental_green,
            R.color.primary_color,
            R.color.accent_color
        );
        
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isLoading) {
                refreshNews();
            } else {
                binding.swipeRefreshLayout.setRefreshing(false);
                showMessage(getString(R.string.news_loading), false);
            }
        });
    }

    private void setupFab() {
        binding.fabRefresh.setOnClickListener(v -> {
            if (!isLoading) {
                refreshNews();
            } else {
                showMessage(getString(R.string.news_refreshing), false);
            }
        });
        
        // Add floating animation
        binding.fabRefresh.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.fab_scale_in));
    }

    private void loadNewsFromDatabase() {
        showLoading(true);
        
        executor.execute(() -> {
            try {
                List<NewsItem> cachedNews = db.newsDao().getAllNews();
                
                requireActivity().runOnUiThread(() -> {
                    if (cachedNews != null && !cachedNews.isEmpty()) {
                        newsList.clear();
                        newsList.addAll(cachedNews);
                        newsAdapter.updateNewsList(cachedNews);
                        updateUnreadCounter();
                        showEmptyState(false);
                    } else {
                        showEmptyState(true);
                    }
                    showLoading(false);
                });
                
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showEmptyState(true);
                    showMessage(getStringWithFallback(R.string.news_failed_load_cached, "Gagal memuat cache berita"), true);
                });
            }
        });
    }

    private void refreshNews() {
        if (isLoading || isRefreshing) return;
        
        // Check network connectivity
        NetworkHelper.NetworkStatus networkStatus = NetworkHelper.getNetworkStatus(requireContext());
        
        if (!networkStatus.isAvailable()) {
            // No internet - load from cache
            showMessage("📶 " + getString(R.string.no_internet) + ". Memuat artikel dari cache...", true);
            loadCachedNews();
            return;
        }
        
        if (!networkStatus.shouldFetch()) {
            // Metered connection - ask user or load cache
            showMessage("📱 Koneksi terbatas. Memuat dari cache untuk menghemat data.", false);
            loadCachedNews();
            return;
        }
        
        isRefreshing = true;
        binding.swipeRefreshLayout.setRefreshing(true);
        binding.fabRefresh.setImageResource(R.drawable.ic_refresh_animated);
        
        // Show network status
        showMessage(networkStatus.getStatusMessage() + " - Memuat berita...", false);
        
        // Check if API key is configured
        if (!ApiConfig.isNewsApiKeyAvailable()) {
            requireActivity().runOnUiThread(() -> {
                isRefreshing = false;
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.fabRefresh.setImageResource(R.drawable.ic_refresh);
                showMessage(getString(R.string.news_api_not_configured), true);
                loadSampleNews();
            });
            return;
        }
          // Fetch environmental news from NewsAPI.org with validation
        newsApi.getValidatedEnvironmentalArticles(new NewsApi.ValidatedNewsCallback() {
            @Override
            public void onSuccess(List<Article> validArticles, com.example.glean.helper.NewsValidator.ValidationStats stats) {
                executor.execute(() -> {
                    try {
                        List<NewsItem> newsItems = new ArrayList<>();
                        
                        // Convert articles to NewsItems
                        for (Article article : validArticles) {
                            NewsItem newsItem = article.toNewsItem();
                            newsItem.setReadingTimeMinutes(calculateReadingTime(newsItem));
                            newsItems.add(newsItem);
                        }
                        
                        // Sort by timestamp (newest first)
                        newsItems.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        
                        // Limit to 50 articles to avoid overwhelming
                        final List<NewsItem> freshNews = newsItems.size() > 50 ? 
                            new ArrayList<>(newsItems.subList(0, 50)) : 
                            new ArrayList<>(newsItems);
                        
                        // Cache valid articles for offline use
                        if (!freshNews.isEmpty()) {
                            cacheManager.cacheArticles(freshNews, new NewsCacheManager.CacheCallback() {
                                @Override
                                public void onCacheSuccess(int cachedCount) {
                                    Log.d("NewsFragment", "Successfully cached " + cachedCount + " valid articles");
                                }

                                @Override
                                public void onCacheError(String error) {
                                    Log.w("NewsFragment", "Cache error: " + error);
                                }
                            });
                        }
                        
                        requireActivity().runOnUiThread(() -> {
                            isRefreshing = false;
                            binding.swipeRefreshLayout.setRefreshing(false);
                            binding.fabRefresh.setImageResource(R.drawable.ic_refresh);
                            
                            if (!freshNews.isEmpty()) {
                                newsList.clear();
                                newsList.addAll(freshNews);
                                newsAdapter.updateNewsList(freshNews);
                                updateUnreadCounter();
                                showEmptyState(false);
                                
                                // Show validation results
                                String successMessage = getStringWithFallback(
                                    R.string.news_validation_success, 
                                    "✅ %d artikel valid dimuat (%d divalidasi)",
                                    freshNews.size(), stats.getTotalArticles()
                                );
                                showMessage(successMessage, false);
                                
                                // Scroll to top to show new content
                                binding.recyclerViewNews.smoothScrollToPosition(0);
                            } else {
                                showEmptyState(true);
                                showMessage(getString(R.string.news_no_valid_articles_found), true);
                            }
                        });
                        
                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            isRefreshing = false;
                            binding.swipeRefreshLayout.setRefreshing(false);
                            binding.fabRefresh.setImageResource(R.drawable.ic_refresh);
                            
                            // Use ErrorHandler for better error categorization
                            ErrorHandler.ErrorCategory category = ErrorHandler.categorizeError(e);
                            String errorMessage = ErrorHandler.getErrorMessageByCategory(category, e);
                            showMessage(errorMessage, true);
                            
                            // Log error for debugging
                            ErrorHandler.logError("NewsFragment", "Process validated articles", e);
                            
                            // Fallback to cached news on error
                            loadValidatedCachedNews();
                        });
                    }
                });
            }

            @Override
            public void onValidationProgress(int processed, int total) {
                requireActivity().runOnUiThread(() -> {
                    String progressMessage = getStringWithFallback(
                        R.string.news_validation_progress,
                        "🔍 Memvalidasi artikel %d/%d...",
                        processed, total
                    );
                    showMessage(progressMessage, false);
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    isRefreshing = false;
                    binding.swipeRefreshLayout.setRefreshing(false);
                    binding.fabRefresh.setImageResource(R.drawable.ic_refresh);
                    
                    // Check if it's a validation error vs API error
                    if (error.contains("validation")) {
                        showMessage(getString(R.string.news_validation_failed), true);
                        // Try quick validation fallback
                        fetchQuickValidatedNews();
                    } else {
                        // Use ErrorHandler for better error messages
                        String userFriendlyError = ErrorHandler.getApiErrorMessage(error);
                        showMessage(userFriendlyError, true);
                        
                        // Log error for debugging
                        ErrorHandler.logError("NewsFragment", "Fetch validated news from API", new Exception(error));
                        
                        // Fallback to cached news on error
                        showMessage(ErrorHandler.getFallbackMessage("Mengambil berita online", "Memuat dari cache"), false);
                        loadValidatedCachedNews();
                    }
                });
            }
        });
    }
    
    // Helper method to calculate reading time based on content length
    private int calculateReadingTime(NewsItem newsItem) {
        String content = "";
        
        // Combine all available text content
        if (newsItem.getFullContent() != null && !newsItem.getFullContent().isEmpty()) {
            content = newsItem.getFullContent();
        } else if (newsItem.getPreview() != null && !newsItem.getPreview().isEmpty()) {
            content = newsItem.getPreview();
        } else if (newsItem.getTitle() != null) {
            content = newsItem.getTitle();
        }
        
        if (content.isEmpty()) {
            return 3; // Default 3 minutes
        }
        
        // Clean HTML tags and calculate word count
        String cleanContent = content.replaceAll("<[^>]*>", "").trim();
        String[] words = cleanContent.split("\\s+");
        int wordCount = words.length;
        
        // Average reading speed: 200-250 words per minute
        // Using 200 words per minute for conservative estimate
        int readingTime = Math.max(1, wordCount / 200);
        
        // Cap at reasonable maximum
        return Math.min(readingTime, 30);
    }

    private void checkForUpdates() {
        // Check for updates every time fragment is visible
        if (newsList.isEmpty()) {
            refreshNews();
        }
    }

    /**
     * Load sample environmental news articles when API fails or no internet
     */
    private void loadSampleNews() {
        executor.execute(() -> {
            try {
                List<Article> sampleArticles = NewsApi.getSampleArticles();
                List<NewsItem> sampleNewsItems = new ArrayList<>();
                
                for (Article article : sampleArticles) {
                    NewsItem newsItem = article.toNewsItem();
                    newsItem.setReadingTimeMinutes(calculateReadingTime(newsItem));
                    sampleNewsItems.add(newsItem);
                }
                
                requireActivity().runOnUiThread(() -> {
                    if (!sampleNewsItems.isEmpty()) {
                        newsList.clear();
                        newsList.addAll(sampleNewsItems);
                        newsAdapter.updateNewsList(sampleNewsItems);
                        updateUnreadCounter();
                        showEmptyState(false);
                        showMessage(getString(R.string.news_showing_sample_offline), false);
                    } else {
                        showEmptyState(true);
                    }
                });
                
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    showEmptyState(true);
                    showMessage(getString(R.string.news_error_loading_sample, e.getMessage()), true);
                });
            }
        });
    }
      /**
     * Load cached news articles for offline viewing (now with validation)
     */
    private void loadCachedNews() {
        loadValidatedCachedNews();
    }

    /**
     * Fallback method to fetch news with quick validation (no URL checking)
     */
    private void fetchQuickValidatedNews() {
        Log.d("NewsFragment", "Attempting quick validation fallback...");
        
        newsApi.getQuickValidatedArticles(new NewsApi.NewsCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                executor.execute(() -> {
                    try {
                        List<NewsItem> newsItems = new ArrayList<>();
                        
                        for (Article article : articles) {
                            NewsItem newsItem = article.toNewsItem();
                            newsItem.setReadingTimeMinutes(calculateReadingTime(newsItem));
                            newsItems.add(newsItem);
                        }
                        
                        requireActivity().runOnUiThread(() -> {
                            if (!newsItems.isEmpty()) {
                                newsList.clear();
                                newsList.addAll(newsItems);
                                newsAdapter.updateNewsList(newsItems);
                                updateUnreadCounter();
                                showEmptyState(false);
                                
                                showMessage(getString(R.string.news_quick_validation_success, newsItems.size()), false);
                            } else {
                                showEmptyState(true);
                                showMessage(getString(R.string.news_no_valid_articles_found), true);
                            }
                        });
                        
                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            ErrorHandler.logError("NewsFragment", "Quick validation fallback", e);
                            loadValidatedCachedNews();
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Log.e("NewsFragment", "Quick validation failed: " + error);
                    loadValidatedCachedNews();
                });
            }
        });
    }

    /**
     * Load cached news articles with validation
     */
    private void loadValidatedCachedNews() {
        cacheManager.getValidatedCachedArticles(new NewsCacheManager.CacheRetrievalCallback() {
            @Override
            public void onCachedArticlesRetrieved(List<NewsItem> articles) {
                requireActivity().runOnUiThread(() -> {
                    if (articles != null && !articles.isEmpty()) {
                        newsList.clear();
                        newsList.addAll(articles);
                        newsAdapter.updateNewsList(articles);
                        updateUnreadCounter();
                        showEmptyState(false);
                        
                        NewsCacheManager.CacheStats stats = cacheManager.getCacheStats();
                        showMessage(getString(R.string.news_validated_cache_loaded, 
                                  articles.size(), stats.getFormattedLastUpdate()), false);
                    } else {
                        // No valid cached articles available, load sample
                        loadSampleNews();
                    }
                });
            }

            @Override
            public void onRetrievalError(String error) {
                requireActivity().runOnUiThread(() -> {
                    showMessage(getString(R.string.news_error_loading_cached, error), true);
                    loadSampleNews();
                });
            }
        });
    }

    private void updateUnreadCounter() {
        int unreadCount = newsAdapter.getUnreadCount();
        
        if (unreadCount > 0) {
            binding.tvUnreadCounter.setVisibility(View.VISIBLE);
            binding.tvUnreadCounter.setText(String.format("📰 %d unread", unreadCount));
            
            // Add pulsing animation for unread counter
            binding.tvUnreadCounter.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.pulse));
        } else {
            binding.tvUnreadCounter.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean show) {
        isLoading = show;
        
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tvLoadingText.setVisibility(View.VISIBLE);
            binding.tvLoadingText.setText("🌱 " + getString(R.string.news_loading));
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvLoadingText.setVisibility(View.GONE);
        }
    }    private void showEmptyState(boolean show) {
        if (show) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewNews.setVisibility(View.GONE);
            
            binding.tvEmptyTitle.setText("📰 No Valid News Available");
            binding.tvEmptyMessage.setText(getString(R.string.news_empty_state_no_valid_articles));
            binding.btnRetryNews.setOnClickListener(v -> refreshNews());
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewNews.setVisibility(View.VISIBLE);
        }
    }

    private void showMessage(String message, boolean isError) {
        if (getView() != null) {
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG);
            
            if (isError) {
                snackbar.setBackgroundTint(getResources().getColor(R.color.error_color));
            } else {
                snackbar.setBackgroundTint(getResources().getColor(R.color.environmental_green));
            }
            
            snackbar.setTextColor(getResources().getColor(android.R.color.white));
            snackbar.show();
        }
    }
    
    // Helper method to get string resources with fallback
    private String getStringWithFallback(int resId, String fallback) {
        try {
            return getString(resId);
        } catch (Exception e) {
            return fallback;
        }
    }
    
    private String getStringWithFallback(int resId, String fallback, Object... formatArgs) {
        try {
            return getString(resId, formatArgs);
        } catch (Exception e) {
            return String.format(fallback, formatArgs);
        }
    }

    @Override
    public void onNewsItemClick(NewsItem newsItem) {
        try {
            // Validate news item
            if (newsItem == null) {
                showMessage(getString(R.string.news_invalid_data), true);
                return;
            }
            
            // Mark as read
            newsItem.setRead(true);
            
            // Update in database
            executor.execute(() -> {
                db.newsDao().updateNews(newsItem);
            });
            
            // Update counter
            updateUnreadCounter();
              // Navigate to news detail or open in browser
            String url = newsItem.getUrl();
            if (url != null && !url.isEmpty()) {
                // Quick URL validation before opening
                if (!com.example.glean.helper.UrlValidator.isProbablyValidUrl(url)) {
                    showMessage(getString(R.string.news_url_might_be_invalid), true);
                    return;
                }
                
                // Log which news was clicked for debugging
                android.util.Log.d("NewsClick", "Clicked: " + newsItem.getTitle() + " -> " + url);
                
                // Navigate to detail fragment with proper data
                Bundle args = new Bundle();
                args.putInt("NEWS_ID", newsItem.getId());
                args.putString("NEWS_URL", url);
                args.putString("NEWS_TITLE", newsItem.getTitle());
                args.putString("NEWS_PREVIEW", newsItem.getPreview());
                args.putString("NEWS_IMAGE_URL", newsItem.getImageUrl());
                args.putString("NEWS_SOURCE", newsItem.getSource());
                args.putString("NEWS_DATE", newsItem.getDate());
                
                NavController navController = Navigation.findNavController(requireView());
                navController.navigate(R.id.newsDetailFragment, args);
            } else {
                showMessage(getString(R.string.news_no_link_available), true);
            }
            
        } catch (Exception e) {
            android.util.Log.e("NewsClick", "Error handling news click", e);
            showMessage(getString(R.string.news_failed_open_article) + ": " + e.getMessage(), true);
        }
    }

    @Override
    public void onNewsItemLongClick(NewsItem newsItem) {
        // Show options dialog
        String[] options = {
            newsItem.isFavorite() ? "❤️ Remove from Favorites" : "💝 Add to Favorites",
            newsItem.isRead() ? "👁️ Mark as Unread" : "✅ Mark as Read",
            "🔗 Open in Browser",
            "📤 Share Article"
        };
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("📰 " + newsItem.getTitle())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Toggle favorite
                        toggleFavorite(newsItem);
                        break;
                    case 1: // Toggle read status
                        toggleReadStatus(newsItem);
                        break;
                    case 2: // Open in browser
                        openInBrowser(newsItem);
                        break;
                    case 3: // Share
                        shareArticle(newsItem);
                        break;
                }
            })
            .show();
    }

    private void toggleFavorite(NewsItem newsItem) {
        newsItem.setFavorite(!newsItem.isFavorite());
        
        executor.execute(() -> {
            db.newsDao().updateNews(newsItem);
        });
        
        String message = newsItem.isFavorite() ? 
            getString(R.string.news_added_to_favorites) : getString(R.string.news_removed_from_favorites);
        showMessage(message, false);
    }

    private void toggleReadStatus(NewsItem newsItem) {
        newsItem.setRead(!newsItem.isRead());
        
        executor.execute(() -> {
            db.newsDao().updateNews(newsItem);
        });
        
        updateUnreadCounter();
        newsAdapter.notifyDataSetChanged();
        
        String message = newsItem.isRead() ? 
            getString(R.string.news_marked_as_read) : getString(R.string.news_marked_as_unread);
        showMessage(message, false);
    }    private void openInBrowser(NewsItem newsItem) {
        if (newsItem.getUrl() != null && !newsItem.getUrl().isEmpty()) {
            try {
                // Validate URL format
                String url = newsItem.getUrl();
                
                // Clean URL using UrlValidator
                url = com.example.glean.helper.UrlValidator.cleanUrl(url);
                
                if (url == null) {
                    showMessage(getString(R.string.news_invalid_url_format), true);
                    return;
                }
                
                // Check if URL is probably valid
                if (!com.example.glean.helper.UrlValidator.isProbablyValidUrl(url)) {
                    showMessage(getString(R.string.news_url_might_be_broken), true);
                    // Still attempt to open, but warn user
                }
                
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                
            } catch (Exception e) {
                showMessage(getString(R.string.news_cannot_open_link) + ": " + e.getMessage(), true);
                android.util.Log.e("NewsFragment", "Error opening URL: " + newsItem.getUrl(), e);
            }
        } else {
            showMessage(getString(R.string.news_link_not_available), true);
        }
    }

    private void shareArticle(NewsItem newsItem) {
        try {
            String shareText = String.format("🌱 %s\n\n%s\n\n📰 Shared via GleanGo", 
                newsItem.getTitle(), 
                newsItem.getUrl() != null ? newsItem.getUrl() : "Check out this environmental news!");
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Environmental News: " + newsItem.getTitle());
            
            startActivity(Intent.createChooser(shareIntent, "📤 Share Article"));
        } catch (Exception e) {
            showMessage(getString(R.string.news_failed_share_article) + ": " + e.getMessage(), true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}

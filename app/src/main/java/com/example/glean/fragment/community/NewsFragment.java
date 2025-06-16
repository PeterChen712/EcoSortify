package com.example.glean.fragment.community;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.glean.R;
import com.example.glean.adapter.NewsAdapter;
import com.example.glean.api.NewsApi;
import com.example.glean.databinding.FragmentNewsBinding;
import com.example.glean.helper.NetworkHelper;
import com.example.glean.helper.NewsCacheManager;
import com.example.glean.helper.NewsValidator;
import com.example.glean.helper.UrlValidator;
import com.example.glean.model.Article;
import com.example.glean.model.NewsItem;

import java.util.ArrayList;
import java.util.List;

public class NewsFragment extends Fragment implements NewsAdapter.OnNewsItemClickListener {
    
    private static final String TAG = "NewsFragment";
    private FragmentNewsBinding binding;
    private NewsAdapter newsAdapter;
    private List<NewsItem> newsList = new ArrayList<>();
    
    // API and cache components
    private NewsApi newsApi;
    private NewsCacheManager cacheManager;
      @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize API and cache components
        newsApi = new NewsApi();
        cacheManager = new NewsCacheManager(requireContext());
        
        // Debug browser availability
        debugBrowserAvailability();
          setupRecyclerView();
        setupSwipeRefresh();
        loadNews();
    }
      private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(requireContext(), newsList, this);
        binding.recyclerViewNews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewNews.setAdapter(newsAdapter);
    }
      private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadNews);
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary_color);
    }

    private void loadNews() {
        binding.swipeRefreshLayout.setRefreshing(true);
        
        // Check network status
        NetworkHelper.NetworkStatus networkStatus = NetworkHelper.getNetworkStatus(requireContext());
        
        if (networkStatus.isAvailable() && networkStatus.shouldFetch()) {
            // Network available - fetch from API with validation
            loadNewsFromApi();
        } else if (networkStatus.isAvailable()) {
            // Limited network - try quick validation
            loadNewsWithQuickValidation();
        } else {
            // No network - load from cache
            loadNewsFromCache();
        }
    }
    
    private void loadNewsFromApi() {
        android.util.Log.d(TAG, "Loading news from API with full validation...");
        
        newsApi.getValidatedEnvironmentalArticles(new NewsApi.ValidatedNewsCallback() {
            @Override
            public void onSuccess(List<Article> validArticles, NewsValidator.ValidationStats stats) {
                android.util.Log.i(TAG, "API validation success: " + stats.getSummary());
                
                // Convert Articles to NewsItems
                List<NewsItem> newsItems = convertArticlesToNewsItems(validArticles);
                
                // Cache the valid articles
                cacheManager.cacheArticles(newsItems, new NewsCacheManager.CacheCallback() {
                    @Override
                    public void onCacheSuccess(int cachedCount) {
                        android.util.Log.d(TAG, "Cached " + cachedCount + " articles");
                    }

                    @Override
                    public void onCacheError(String error) {
                        android.util.Log.w(TAG, "Cache error: " + error);
                    }
                });
                  // Display filtered results
                displayNewsItems(newsItems);
                showSuccessMessage("Artikel berhasil dimuat dan divalidasi");
            }

            @Override
            public void onValidationProgress(int processed, int total) {
                // Update progress if needed
                android.util.Log.d(TAG, "Validation progress: " + processed + "/" + total);
            }

            @Override            public void onError(String error) {
                android.util.Log.e(TAG, "API validation error: " + error);
                showErrorMessage("Gagal memuat dari API, mencoba alternatif...");
                
                // Fallback to quick validation
                loadNewsWithQuickValidation();
            }
        });
    }
    
    private void loadNewsWithQuickValidation() {
        android.util.Log.d(TAG, "Loading news with quick validation...");
        
        newsApi.getQuickValidatedArticles(new NewsApi.NewsCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                android.util.Log.d(TAG, "Quick validation success: " + articles.size() + " articles");
                  List<NewsItem> newsItems = convertArticlesToNewsItems(articles);
                displayNewsItems(newsItems);
                showSuccessMessage("Artikel dimuat dengan validasi cepat");
            }

            @Override            public void onError(String error) {
                android.util.Log.e(TAG, "Quick validation error: " + error);
                showErrorMessage("Gagal validasi cepat, mencoba cache...");
                
                // Fallback to cache
                loadNewsFromCache();
            }
        });
    }
    
    private void loadNewsFromCache() {
        android.util.Log.d(TAG, "Loading news from cache...");
        
        cacheManager.getValidatedCachedArticles(new NewsCacheManager.CacheRetrievalCallback() {
            @Override
            public void onCachedArticlesRetrieved(List<NewsItem> cachedArticles) {                if (!cachedArticles.isEmpty()) {
                    android.util.Log.d(TAG, "Loaded " + cachedArticles.size() + " articles from cache");
                    displayNewsItems(cachedArticles);
                    showInfoMessage("Memuat " + cachedArticles.size() + " artikel dari cache");
                } else {
                    // Ultimate fallback: sample articles
                    loadSampleArticles();
                }
            }

            @Override
            public void onRetrievalError(String error) {
                android.util.Log.e(TAG, "Cache retrieval error: " + error);
                loadSampleArticles();
            }
        });
    }
      private void loadSampleArticles() {
        android.util.Log.d(TAG, "No sample articles available - showing empty state");
        
        // No dummy data - show empty state with helpful message
        displayNewsItems(new ArrayList<>());
        showInfoMessage("Tidak ada koneksi internet. Silakan periksa koneksi dan coba lagi.");
    }
    
    private List<NewsItem> convertArticlesToNewsItems(List<Article> articles) {
        List<NewsItem> newsItems = new ArrayList<>();
        
        for (Article article : articles) {
            NewsItem newsItem = article.toNewsItem();
            
            // Set additional fields for UI
            if (newsItem.getCategory() == null || newsItem.getCategory().isEmpty()) {
                newsItem.setCategory("environment"); // Default category
            }
            
            // Ensure image URL for better UI
            if (newsItem.getImageUrl() == null || newsItem.getImageUrl().isEmpty()) {
                newsItem.setImageUrl("https://images.unsplash.com/photo-1611273426858-450d8e3c9fce?w=400&h=300&fit=crop");
            }
            
            newsItems.add(newsItem);
        }
        
        return newsItems;
    }
      private void displayNewsItems(List<NewsItem> newsItems) {
        newsList.clear();
        newsList.addAll(newsItems);
        
        // Update UI on main thread
        requireActivity().runOnUiThread(() -> {
            newsAdapter.notifyDataSetChanged();
            binding.swipeRefreshLayout.setRefreshing(false);
            
            // Show/hide empty state
            if (newsList.isEmpty()) {
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewNews.setVisibility(View.GONE);
            } else {
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.recyclerViewNews.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void showSuccessMessage(String message) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void showErrorMessage(String message) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }
    
    private void showInfoMessage(String message) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }@Override
    public void onNewsItemClick(NewsItem news) {
        // Validate news data before navigation
        if (news == null) {
            Toast.makeText(requireContext(), "Data berita tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }
          String url = news.getUrl();
        if (url == null || url.isEmpty()) {
            Toast.makeText(requireContext(), "URL berita tidak tersedia", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate URL format
        if (!isValidUrl(url)) {
            android.util.Log.e(TAG, "Invalid URL format: " + url);
            Toast.makeText(requireContext(), "Format URL tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Track which news was clicked for analytics
            android.util.Log.d(TAG, "User clicked: " + news.getTitle() + " -> " + url);
            
            // Create intent to open URL in browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            
            // Add flags to ensure proper browser behavior
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            
            // List of common browser packages to try
            String[] browserPackages = {
                "com.android.chrome",           // Chrome
                "com.android.browser",          // Default Android browser
                "org.mozilla.firefox",         // Firefox
                "com.opera.browser",            // Opera
                "com.sec.android.app.sbrowser", // Samsung Internet
                "com.microsoft.emmx"            // Edge
            };
            
            boolean browserFound = false;
            
            // Try specific browsers first
            for (String packageName : browserPackages) {
                Intent browserIntent = new Intent(intent);
                browserIntent.setPackage(packageName);
                
                if (browserIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(browserIntent);
                    browserFound = true;
                    android.util.Log.d(TAG, "Opened with browser: " + packageName);
                    break;
                }
            }
            
            // If no specific browser found, try generic intent
            if (!browserFound) {
                intent.setPackage(null); // Remove any package restrictions
                
                // Check if there's any app that can handle the intent
                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(intent);
                    android.util.Log.d(TAG, "Opened with default handler");                } else {
                    // Last resort: try to open as web URL without restrictions
                    Intent genericIntent = new Intent(Intent.ACTION_VIEW);
                    genericIntent.setData(Uri.parse(url));
                    genericIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    try {
                        startActivity(genericIntent);
                        android.util.Log.d(TAG, "Opened with generic intent");
                    } catch (Exception e) {
                        // Ultimate fallback: open in WebView through NewsDetailFragment
                        android.util.Log.w(TAG, "No external browser available, falling back to WebView");
                        openInWebView(news);
                    }
                }
            }
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error opening news: " + e.getMessage());
            Toast.makeText(requireContext(), "Tidak dapat membuka artikel: " + e.getMessage(), Toast.LENGTH_LONG).show();        }
    }
      /**
     * Fallback method to open news in WebView if no external browser is available
     */
    private void openInWebView(NewsItem news) {
        try {
            // For now, just show a message that we would open in WebView
            // In a full implementation, this would navigate to NewsDetailFragment
            Toast.makeText(requireContext(), 
                "Membuka dalam WebView: " + news.getTitle(), 
                Toast.LENGTH_LONG).show();
                    
            android.util.Log.d(TAG, "Would open in WebView: " + news.getTitle() + " -> " + news.getUrl());
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to open in WebView: " + e.getMessage());
            Toast.makeText(requireContext(), "Tidak dapat membuka artikel", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Validate if the URL is properly formatted
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // Check for basic URL patterns
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        
        // Reject example URLs
        if (url.contains("example.com") || url.contains("localhost")) {
            return false;
        }
        
        try {
            Uri uri = Uri.parse(url);
            return uri != null && uri.getHost() != null && !uri.getHost().isEmpty();
        } catch (Exception e) {
            android.util.Log.e(TAG, "URL validation error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Debug method to check available browsers on the device
     */
    private void debugBrowserAvailability() {
        String[] browserPackages = {
            "com.android.chrome",           // Chrome
            "com.android.browser",          // Default Android browser
            "org.mozilla.firefox",         // Firefox
            "com.opera.browser",            // Opera
            "com.sec.android.app.sbrowser", // Samsung Internet
            "com.microsoft.emmx"            // Edge
        };
        
        android.util.Log.d(TAG, "=== Browser Availability Debug ===");
        
        for (String packageName : browserPackages) {
            Intent testIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
            testIntent.setPackage(packageName);
            
            boolean isAvailable = testIntent.resolveActivity(requireContext().getPackageManager()) != null;
            android.util.Log.d(TAG, packageName + ": " + (isAvailable ? "AVAILABLE" : "NOT FOUND"));
        }
        
        // Test generic browser intent
        Intent genericIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
        boolean hasGenericHandler = genericIntent.resolveActivity(requireContext().getPackageManager()) != null;
        android.util.Log.d(TAG, "Generic browser handler: " + (hasGenericHandler ? "AVAILABLE" : "NOT FOUND"));
        
        android.util.Log.d(TAG, "=== End Browser Debug ===");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

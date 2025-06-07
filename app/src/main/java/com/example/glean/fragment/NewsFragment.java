package com.example.glean.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.glean.R;
import com.example.glean.adapter.NewsAdapter;
import com.example.glean.databinding.FragmentNewsBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.helper.RSSHelper;
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
    private RSSHelper rssHelper;
    private boolean isLoading = false;
    private boolean isRefreshing = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        rssHelper = new RSSHelper();
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
                showMessage("Please wait, already loading news...", false);
            }
        });
    }

    private void setupFab() {
        binding.fabRefresh.setOnClickListener(v -> {
            if (!isLoading) {
                refreshNews();
            } else {
                showMessage("Already refreshing news...", false);
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
                    showMessage("Failed to load cached news", true);
                });
            }
        });
    }

    private void refreshNews() {
        if (isLoading || isRefreshing) return;
        
        isRefreshing = true;
        binding.swipeRefreshLayout.setRefreshing(true);
        binding.fabRefresh.setImageResource(R.drawable.ic_refresh_animated);
        
        executor.execute(() -> {
            try {
                // Fetch news from multiple environmental sources
                List<NewsItem> rawNews = new ArrayList<>();
                
                // Environmental news sources
                List<String> sources = getEnvironmentalNewsSources();
                
                for (String source : sources) {
                    try {
                        List<NewsItem> sourceNews = rssHelper.fetchNewsFromRSS(source);
                        if (sourceNews != null && !sourceNews.isEmpty()) {
                            rawNews.addAll(sourceNews);
                        }
                    } catch (Exception e) {
                        // Continue with other sources if one fails
                    }
                }
                
                // Sort by timestamp (newest first)
                rawNews.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                
                // Limit to 50 articles to avoid overwhelming and create final list
                final List<NewsItem> freshNews = rawNews.size() > 50 ? 
                    new ArrayList<>(rawNews.subList(0, 50)) : 
                    new ArrayList<>(rawNews);
                
                // Save to database
                if (!freshNews.isEmpty()) {
                    // Clear old news and insert new
                    db.newsDao().deleteOldNews(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)); // Keep 30 days
                    
                    for (NewsItem news : freshNews) {
                        // Set reading time based on content length
                        news.setReadingTimeMinutes(calculateReadingTime(news));
                        db.newsDao().insertNews(news);
                    }
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
                        
                        showMessage(String.format("🆕 %d new articles loaded!", freshNews.size()), false);
                        
                        // Scroll to top to show new content
                        binding.recyclerViewNews.smoothScrollToPosition(0);
                    } else {
                        showMessage("📡 No new articles available", false);
                    }
                });
                
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    isRefreshing = false;
                    binding.swipeRefreshLayout.setRefreshing(false);
                    binding.fabRefresh.setImageResource(R.drawable.ic_refresh);
                    showMessage("🌐 Check your internet connection", true);
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

    private List<String> getEnvironmentalNewsSources() {
        List<String> sources = new ArrayList<>();
        
        // Updated and verified environmental RSS feeds
        sources.add("https://feeds.feedburner.com/EnvironmentalNews");
        sources.add("https://phys.org/rss-feed/earth-news/environment/");
        sources.add("https://www.sciencedaily.com/rss/earth_climate.xml"); // Updated URL
        sources.add("https://www.treehugger.com/feeds/all"); // TreeHugger environmental news
        sources.add("https://www.climatecentral.org/feeds/all.rss"); // Climate Central
        sources.add("https://www.carbonbrief.org/feed/"); // Carbon Brief
        sources.add("https://www.renewableenergyworld.com/news/rss.xml"); // Renewable Energy World
        sources.add("https://www.environmentalleader.com/feed/"); // Environmental Leader
        sources.add("https://www.ecowatch.com/feed"); // EcoWatch
        sources.add("https://www.greentechmedia.com/rss/all"); // Green Tech Media
        
        return sources;
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
            binding.tvLoadingText.setText("🌱 Loading environmental news...");
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvLoadingText.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewNews.setVisibility(View.GONE);
            
            binding.tvEmptyTitle.setText("📰 No News Available");
            binding.tvEmptyMessage.setText("Pull down to refresh and load environmental news articles");
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

    @Override
    public void onNewsItemClick(NewsItem newsItem) {
        try {
            // Mark as read
            newsItem.setRead(true);
            
            // Update in database
            executor.execute(() -> {
                db.newsDao().updateNews(newsItem);
            });
            
            // Update counter
            updateUnreadCounter();
            
            // Navigate to news detail or open in browser
            if (newsItem.getUrl() != null && !newsItem.getUrl().isEmpty()) {
                // Navigate to detail fragment
                Bundle args = new Bundle();
                args.putInt("NEWS_ID", newsItem.getId());
                args.putString("NEWS_URL", newsItem.getUrl());
                args.putString("NEWS_TITLE", newsItem.getTitle());
                  NavController navController = Navigation.findNavController(requireView());
                navController.navigate(R.id.newsDetailFragment, args);
            } else {
                showMessage("📰 Article content not available", true);
            }
            
        } catch (Exception e) {
            showMessage("Failed to open article", true);
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
            "💝 Added to favorites" : "💔 Removed from favorites";
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
            "✅ Marked as read" : "👁️ Marked as unread";
        showMessage(message, false);
    }

    private void openInBrowser(NewsItem newsItem) {
        if (newsItem.getUrl() != null && !newsItem.getUrl().isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.getUrl()));
                startActivity(intent);
            } catch (Exception e) {
                showMessage("🌐 Cannot open link", true);
            }
        } else {
            showMessage("🔗 No link available", true);
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
            showMessage("📤 Failed to share article", true);
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
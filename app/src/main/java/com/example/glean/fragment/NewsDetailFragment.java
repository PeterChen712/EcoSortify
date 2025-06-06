package com.example.glean.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.glean.R;
import com.example.glean.databinding.FragmentNewsDetailBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.NewsItem;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsDetailFragment extends Fragment {
    
    private FragmentNewsDetailBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private NewsItem currentNews;
    private int newsId = -1;
    private String newsUrl;
    private String newsTitle;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        // Get arguments
        if (getArguments() != null) {
            newsId = getArguments().getInt("NEWS_ID", -1);
            newsUrl = getArguments().getString("NEWS_URL");
            newsTitle = getArguments().getString("NEWS_TITLE");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewsDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupToolbar();
        setupWebView();
        
        if (newsId != -1) {
            loadNewsFromDatabase();
        } else if (newsUrl != null) {
            loadNewsFromUrl();
        }
    }
    
    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigateUp();
        });
        
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                shareArticle();
                return true;
            } else if (item.getItemId() == R.id.action_open_browser) {
                openInBrowser();
                return true;
            } else if (item.getItemId() == R.id.action_favorite) {
                toggleFavorite();
                return true;
            }
            return false;
        });
    }
    
    private void setupWebView() {
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setDomStorageEnabled(true);
        binding.webView.getSettings().setLoadWithOverviewMode(true);
        binding.webView.getSettings().setUseWideViewPort(true);
        binding.webView.getSettings().setBuiltInZoomControls(true);
        binding.webView.getSettings().setDisplayZoomControls(false);
        
        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showLoading(true);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                showLoading(false);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                showLoading(false);
                showError("Failed to load article: " + description);
            }
        });
    }
    
    private void loadNewsFromDatabase() {
        showLoading(true);
        
        executor.execute(() -> {
            try {
                NewsItem news = db.newsDao().getNewsById(newsId);
                
                requireActivity().runOnUiThread(() -> {
                    if (news != null) {
                        currentNews = news;
                        displayNews(news);
                    } else {
                        showError("Article not found");
                    }
                    showLoading(false);
                });
                
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showError("Failed to load article");
                });
            }
        });
    }
    
    private void loadNewsFromUrl() {
        if (newsUrl == null || newsUrl.isEmpty()) {
            showError("No article URL available");
            return;
        }
        
        // Set title if available
        if (newsTitle != null) {
            binding.toolbar.setTitle(newsTitle);
        }
        
        // Load URL in WebView
        binding.webView.loadUrl(newsUrl);
    }
    
    private void displayNews(NewsItem news) {
        // Set title
        binding.toolbar.setTitle(news.getTitle());
        
        // Load header image
        if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
            binding.ivHeaderImage.setVisibility(View.VISIBLE);
            
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_news_loading)
                    .error(R.drawable.ic_news_placeholder)
                    .transform(new CenterCrop(), new RoundedCorners(16))
                    .timeout(10000);
            
            Glide.with(this)
                    .load(news.getImageUrl())
                    .apply(options)
                    .into(binding.ivHeaderImage);
        } else {
            binding.ivHeaderImage.setVisibility(View.GONE);
        }
        
        // Set article info
        binding.tvTitle.setText(news.getTitle());
        binding.tvSource.setText(String.format("🌱 %s • %s", 
            news.getSource() != null ? news.getSource() : "Environmental News",
            news.getFormattedDate()));
        
        // FIXED: Create reading time text from reading time minutes
        String readingTimeText = getReadingTimeText(news.getReadingTimeMinutes());
        binding.tvReadingTime.setText(readingTimeText);
        
        // Set category
        if (news.getCategory() != null && !news.getCategory().isEmpty()) {
            binding.tvCategory.setText(news.getCategory().toUpperCase());
            binding.tvCategory.setVisibility(View.VISIBLE);
        } else {
            binding.tvCategory.setVisibility(View.GONE);
        }
        
        // Load content
        if (news.getFullContent() != null && !news.getFullContent().isEmpty()) {
            displayContent(news.getFullContent());
        } else if (news.getUrl() != null && !news.getUrl().isEmpty()) {
            // Load from URL if no full content
            binding.webView.loadUrl(news.getUrl());
        } else {
            // Show preview only
            displayContent(news.getPreview() != null ? news.getPreview() : "No content available");
        }
        
        // Update favorite button
        updateFavoriteButton();
    }
    
    // ADDED: Helper method to create reading time text
    private String getReadingTimeText(int readingTimeMinutes) {
        if (readingTimeMinutes <= 0) {
            return "Quick read";
        } else if (readingTimeMinutes == 1) {
            return "📖 1 min read";
        } else if (readingTimeMinutes < 60) {
            return String.format("📖 %d min read", readingTimeMinutes);
        } else {
            int hours = readingTimeMinutes / 60;
            int minutes = readingTimeMinutes % 60;
            if (minutes == 0) {
                return String.format("📖 %d hour read", hours);
            } else {
                return String.format("📖 %dh %dm read", hours, minutes);
            }
        }
    }
    
    private void displayContent(String content) {
        // Create HTML content with proper styling
        String htmlContent = createStyledHtmlContent(content);
        binding.webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }
    
    private String createStyledHtmlContent(String content) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { " +
                "  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; " +
                "  margin: 16px; " +
                "  line-height: 1.6; " +
                "  color: #212121; " +
                "  background-color: #ffffff; " +
                "}" +
                "h1, h2, h3 { " +
                "  color: #4CAF50; " +
                "  margin-top: 24px; " +
                "  margin-bottom: 16px; " +
                "}" +
                "p { " +
                "  margin-bottom: 16px; " +
                "  text-align: justify; " +
                "}" +
                "img { " +
                "  max-width: 100%; " +
                "  height: auto; " +
                "  border-radius: 8px; " +
                "  margin: 16px 0; " +
                "}" +
                "a { " +
                "  color: #4CAF50; " +
                "  text-decoration: none; " +
                "}" +
                "a:hover { " +
                "  text-decoration: underline; " +
                "}" +
                "blockquote { " +
                "  border-left: 4px solid #4CAF50; " +
                "  margin: 16px 0; " +
                "  padding: 8px 16px; " +
                "  background-color: #f5f5f5; " +
                "  border-radius: 4px; " +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                content +
                "</body>" +
                "</html>";
    }
    
    private void shareArticle() {
        if (currentNews != null) {
            String shareText = String.format("🌱 %s\n\n%s\n\n📰 Shared via GleanGo", 
                currentNews.getTitle(), 
                currentNews.getUrl() != null ? currentNews.getUrl() : "Check out this environmental news!");
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Environmental News: " + currentNews.getTitle());
            
            startActivity(Intent.createChooser(shareIntent, "📤 Share Article"));
        } else if (newsUrl != null) {
            String shareText = String.format("🌱 %s\n\n%s\n\n📰 Shared via GleanGo", 
                newsTitle != null ? newsTitle : "Environmental News", newsUrl);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            
            startActivity(Intent.createChooser(shareIntent, "📤 Share Article"));
        }
    }
    
    private void openInBrowser() {
        String url = null;
        if (currentNews != null && currentNews.getUrl() != null) {
            url = currentNews.getUrl();
        } else if (newsUrl != null) {
            url = newsUrl;
        }
        
        if (url != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                showError("🌐 Cannot open link in browser");
            }
        } else {
            showError("🔗 No link available");
        }
    }
    
    private void toggleFavorite() {
        if (currentNews != null) {
            currentNews.setFavorite(!currentNews.isFavorite());
            
            executor.execute(() -> {
                db.newsDao().updateNews(currentNews);
            });
            
            updateFavoriteButton();
            
            String message = currentNews.isFavorite() ? 
                "💝 Added to favorites" : "💔 Removed from favorites";
            showMessage(message);
        }
    }
    
    private void updateFavoriteButton() {
        if (currentNews != null && binding.toolbar.getMenu().findItem(R.id.action_favorite) != null) {
            int iconRes = currentNews.isFavorite() ? 
                R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline;
            binding.toolbar.getMenu().findItem(R.id.action_favorite).setIcon(iconRes);
        }
    }
    
    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tvLoadingText.setVisibility(View.VISIBLE);
            binding.tvLoadingText.setText("📰 Loading article...");
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvLoadingText.setVisibility(View.GONE);
        }
    }
    
    private void showError(String message) {
        binding.layoutError.setVisibility(View.VISIBLE);
        binding.webView.setVisibility(View.GONE);
        binding.tvErrorMessage.setText(message);
        
        binding.btnRetry.setOnClickListener(v -> {
            binding.layoutError.setVisibility(View.GONE);
            binding.webView.setVisibility(View.VISIBLE);
            
            if (newsId != -1) {
                loadNewsFromDatabase();
            } else if (newsUrl != null) {
                loadNewsFromUrl();
            }
        });
    }
    
    private void showMessage(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.environmental_green))
                    .setTextColor(getResources().getColor(android.R.color.white))
                    .show();
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
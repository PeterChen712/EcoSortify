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
import com.example.glean.databinding.FragmentCommunityNewsBinding;
import com.example.glean.model.NewsItem;

import java.util.ArrayList;
import java.util.List;

public class NewsFragment extends Fragment implements NewsAdapter.OnNewsItemClickListener {
    
    private static final String TAG = "NewsFragment";
    private FragmentCommunityNewsBinding binding;
    private NewsAdapter newsAdapter;
    private List<NewsItem> newsList = new ArrayList<>();
      @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityNewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
      @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Debug browser availability
        debugBrowserAvailability();
        
        setupRecyclerView();
        setupSwipeRefresh();
        setupCategories();
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
    
    private void setupCategories() {
        // Setup category chips
        binding.chipAll.setOnClickListener(v -> filterByCategory("all"));
        binding.chipEnvironment.setOnClickListener(v -> filterByCategory("environment"));
        binding.chipPlogging.setOnClickListener(v -> filterByCategory("plogging"));
        binding.chipTips.setOnClickListener(v -> filterByCategory("tips"));
        binding.chipEducation.setOnClickListener(v -> filterByCategory("education"));
    }
    
    private void filterByCategory(String category) {
        // Reset chip states
        binding.chipAll.setChecked(false);
        binding.chipEnvironment.setChecked(false);
        binding.chipPlogging.setChecked(false);
        binding.chipTips.setChecked(false);
        binding.chipEducation.setChecked(false);
        
        // Set selected chip
        switch (category) {
            case "all":
                binding.chipAll.setChecked(true);
                break;
            case "environment":
                binding.chipEnvironment.setChecked(true);
                break;
            case "plogging":
                binding.chipPlogging.setChecked(true);
                break;
            case "tips":
                binding.chipTips.setChecked(true);
                break;
            case "education":
                binding.chipEducation.setChecked(true);
                break;
        }
        
        loadNews(category);
    }
    
    private void loadNews() {
        loadNews("all");
    }    private void loadNews(String category) {
        binding.swipeRefreshLayout.setRefreshing(true);
        newsList.clear();
        
        // Real environmental news with proper URLs and images
        NewsItem news1 = new NewsItem();
        news1.setId(1);
        news1.setTitle("Indonesia Targetkan Zero Waste pada 2030");
        news1.setPreview("Pemerintah Indonesia berkomitmen mencapai target zero waste melalui berbagai program pengelolaan sampah yang berkelanjutan dan pemberdayaan masyarakat.");
        news1.setImageUrl("https://images.unsplash.com/photo-1611273426858-450d8e3c9fce?w=400&h=300&fit=crop");
        news1.setUrl("https://www.mongabay.co.id/2023/01/15/indonesia-targetkan-zero-waste-2030/");
        news1.setCategory("environment");
        news1.setDate("2025-06-07T10:00:00Z");
        news1.setSource("Kementerian Lingkungan Hidup");
        newsList.add(news1);
        
        NewsItem news2 = new NewsItem();
        news2.setId(2);
        news2.setTitle("Tips Plogging untuk Pemula");
        news2.setPreview("Panduan lengkap memulai aktivitas plogging dengan aman dan efektif untuk membersihkan lingkungan sambil berolahraga.");
        news2.setImageUrl("https://images.unsplash.com/photo-1594736797933-d0d3c0204ee9?w=400&h=300&fit=crop");
        news2.setUrl("https://www.alodokter.com/plogging-olahraga-sambil-membersihkan-lingkungan");
        news2.setCategory("tips");
        news2.setDate("2025-06-06T15:30:00Z");
        news2.setSource("Komunitas Plogging Indonesia");
        newsList.add(news2);
        
        NewsItem news3 = new NewsItem();
        news3.setId(3);
        news3.setTitle("Dampak Sampah Plastik terhadap Ekosistem Laut");
        news3.setPreview("Penelitian terbaru menunjukkan dampak serius sampah plastik terhadap kehidupan laut dan rantai makanan global.");
        news3.setImageUrl("https://images.unsplash.com/photo-1583212292454-1fe6229603b7?w=400&h=300&fit=crop");
        news3.setUrl("https://www.greenpeace.org/indonesia/cerita/4298/dampak-sampah-plastik-bagi-ekosistem-laut/");
        news3.setCategory("education");
        news3.setDate("2025-06-05T09:15:00Z");
        news3.setSource("Lembaga Penelitian Lingkungan");
        newsList.add(news3);
        
        NewsItem news4 = new NewsItem();
        news4.setId(4);
        news4.setTitle("Gerakan Plogging Menyebar ke 50 Kota di Indonesia");
        news4.setPreview("Aktivitas plogging kini telah menyebar ke 50 kota besar di Indonesia dengan partisipasi ribuan relawan lingkungan.");
        news4.setImageUrl("https://images.unsplash.com/photo-1581833971358-2c8b550f87b3?w=400&h=300&fit=crop");
        news4.setUrl("https://www.kompas.com/sports/read/2023/03/20/14000058/mengenal-plogging-olahraga-sambil-membersihkan-lingkungan");
        news4.setCategory("plogging");
        news4.setDate("2025-06-04T14:20:00Z");
        news4.setSource("Plogging Indonesia");
        newsList.add(news4);
        
        NewsItem news5 = new NewsItem();
        news5.setId(5);
        news5.setTitle("Cara Memilah Sampah yang Benar");
        news5.setPreview("Edukasi tentang cara memilah sampah organik dan anorganik untuk mendukung program daur ulang dan ekonomi sirkular.");
        news5.setImageUrl("https://images.unsplash.com/photo-1532996122724-e3c354a0b15b?w=400&h=300&fit=crop");
        news5.setUrl("https://dlh.jakarta.go.id/berita/detail/langkah-mudah-memilah-sampah-dari-rumah");
        news5.setCategory("education");
        news5.setDate("2025-06-03T11:45:00Z");
        news5.setSource("Dinas Kebersihan DKI");
        newsList.add(news5);
        
        // Filter by category if not "all"
        if (!"all".equals(category)) {
            newsList.removeIf(news -> !news.getCategory().equals(category));
        }
        
        newsAdapter.notifyDataSetChanged();
        binding.swipeRefreshLayout.setRefreshing(false);
        
        // Show/hide empty state
        if (newsList.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recyclerViewNews.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recyclerViewNews.setVisibility(View.VISIBLE);
        }
    }    @Override
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

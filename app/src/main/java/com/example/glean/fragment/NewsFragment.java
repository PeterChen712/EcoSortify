package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.glean.R;
import com.example.glean.adapter.NewsAdapter;
import com.example.glean.api.NewsApiService;
import com.example.glean.databinding.FragmentNewsBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.NewsEntity;
import com.example.glean.model.NewsResponse;
import com.example.glean.model.NewsItem;
import com.example.glean.util.ApiConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NewsFragment extends Fragment implements 
        NewsAdapter.OnNewsItemClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private FragmentNewsBinding binding;
    private NewsAdapter newsAdapter;
    private List<NewsEntity> newsList;   // Keep this for database operations
    private List<NewsItem> newsItemList; // Add this for the adapter
    private AppDatabase db;
    private ExecutorService executor;
    private NewsApiService newsApiService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        newsList = new ArrayList<>();
        newsItemList = new ArrayList<>();  // Initialize the new list
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newFixedThreadPool(2);
        
        // Initialize Retrofit for News API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://newsapi.org/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        newsApiService = retrofit.create(NewsApiService.class);
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
        
        setupUI();
        loadCachedNews();
        
        // Load online news if available
        if (isOnline()) {
            loadOnlineNews();
        }
    }

    private void setupUI() {
        // Setup RecyclerView with newsItemList instead of newsList
        newsAdapter = new NewsAdapter(newsItemList, this);
        binding.rvNews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNews.setAdapter(newsAdapter);
        
        // Setup SwipeRefresh
        binding.swipeRefresh.setOnRefreshListener(this);
        binding.swipeRefresh.setColorSchemeResources(
                R.color.primary,
                R.color.primary_dark,
                R.color.accent
        );
        
        // Setup category filters
        binding.chipAll.setOnClickListener(v -> filterNews("all"));
        binding.chipEnvironment.setOnClickListener(v -> filterNews("environment"));
        binding.chipRecycling.setOnClickListener(v -> filterNews("recycling"));
        binding.chipClimate.setOnClickListener(v -> filterNews("climate"));
    }

    private void loadCachedNews() {
        executor.execute(() -> {
            List<NewsEntity> cachedNews = db.newsDao().getAllNewsSync(); // Using synchronous method
            
            requireActivity().runOnUiThread(() -> {
                if (!cachedNews.isEmpty()) {
                    newsList.clear();
                    newsList.addAll(cachedNews);
                    updateAdapterList();  // Convert entities to items
                    newsAdapter.notifyDataSetChanged();
                    binding.tvEmptyState.setVisibility(View.GONE);
                } else {
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setText("No cached news available.\nPull to refresh when online.");
                }
            });
        });
    }

    private void loadOnlineNews() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Load environmental news
        String query = "environment OR recycling OR sustainability OR climate change OR plastic pollution";
        String apiKey = ApiConfig.getNewsApiKey();
        
        if (apiKey == null || apiKey.isEmpty()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.swipeRefresh.setRefreshing(false);
            Toast.makeText(requireContext(), "News API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<NewsResponse> call = newsApiService.getEnvironmentalNews(
                query, apiKey, "en", "publishedAt", 50, 
                "bbc.co.uk,reuters.com,cnn.com,theguardian.com"
        );

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    List<NewsResponse.Article> articles = response.body().getArticles();
                    if (articles != null && !articles.isEmpty()) {
                        cacheAndDisplayNews(articles);
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load news", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), 
                               Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cacheAndDisplayNews(List<NewsResponse.Article> articles) {
        executor.execute(() -> {
            // Clear old cached news
            db.newsDao().deleteAllNews();
            
            // Convert and cache new articles
            List<NewsEntity> newsEntities = new ArrayList<>();
            for (NewsResponse.Article article : articles) {
                if (article.getTitle() != null && !article.getTitle().equals("[Removed]")) {
                    NewsEntity newsEntity = new NewsEntity();
                    newsEntity.setTitle(article.getTitle());
                    newsEntity.setPreview(article.getDescription());
                    newsEntity.setFullContent(article.getContent());
                    newsEntity.setDate(article.getPublishedAt());
                    newsEntity.setSource(article.getSource() != null ? article.getSource().getName() : "Unknown");
                    newsEntity.setImageUrl(article.getUrlToImage());
                    newsEntity.setUrl(article.getUrl());
                    newsEntity.setCategory(determineCategory(article.getTitle(), article.getDescription()));
                    newsEntity.setCreatedAt(System.currentTimeMillis());
                    
                    newsEntities.add(newsEntity);
                }
            }
            
            // Insert to database
            long[] ids = db.newsDao().insertNews(newsEntities);
            
            // Update IDs
            for (int i = 0; i < newsEntities.size() && i < ids.length; i++) {
                newsEntities.get(i).setId((int) ids[i]);
            }
            
            // Update UI
            requireActivity().runOnUiThread(() -> {
                newsList.clear();
                newsList.addAll(newsEntities);
                updateAdapterList(); // Add this line to convert entities to items
                newsAdapter.notifyDataSetChanged();
                binding.tvEmptyState.setVisibility(View.GONE);
            });
        });
    }

    private String determineCategory(String title, String description) {
        String text = (title + " " + description).toLowerCase();
        
        if (text.contains("recycle") || text.contains("plastic") || text.contains("waste")) {
            return "recycling";
        } else if (text.contains("climate") || text.contains("carbon") || text.contains("emission")) {
            return "climate";
        } else {
            return "environment";
        }
    }

    private void filterNews(String category) {
        // Reset all chips
        binding.chipAll.setChecked(false);
        binding.chipEnvironment.setChecked(false);
        binding.chipRecycling.setChecked(false);
        binding.chipClimate.setChecked(false);
        
        // Set selected chip
        switch (category) {
            case "all":
                binding.chipAll.setChecked(true);
                break;
            case "environment":
                binding.chipEnvironment.setChecked(true);
                break;
            case "recycling":
                binding.chipRecycling.setChecked(true);
                break;
            case "climate":
                binding.chipClimate.setChecked(true);
                break;
        }
        
        executor.execute(() -> {
            List<NewsEntity> filteredNews;
            if ("all".equals(category)) {
                filteredNews = db.newsDao().getAllNewsSync(); // Changed from getAllNews() to getAllNewsSync()
            } else {
                filteredNews = db.newsDao().getNewsByCategory(category);
            }
            
            requireActivity().runOnUiThread(() -> {
                newsList.clear();
                newsList.addAll(filteredNews);
                updateAdapterList(); // Convert entities to items
                newsAdapter.notifyDataSetChanged();
                
                if (filteredNews.isEmpty()) {
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setText("No news found for this category");
                } else {
                    binding.tvEmptyState.setVisibility(View.GONE);
                }
            });
        });
    }

    private boolean isOnline() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onRefresh() {
        if (isOnline()) {
            loadOnlineNews();
        } else {
            binding.swipeRefresh.setRefreshing(false);
            Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    public void onNewsClick(NewsEntity news, int position) {
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putInt("NEWS_ID", news.getId());
        navController.navigate(R.id.action_newsFragment_to_newsDetailFragment, args);
    }

    public void onNewsShare(NewsEntity news, int position) {
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, 
                news.getTitle() + "\n\n" + news.getUrl());
        startActivity(android.content.Intent.createChooser(shareIntent, "Share news"));
    }

    @Override
    public void onNewsItemClick(NewsItem newsItem) {
        // Create equivalent NewsEntity if needed
        NewsEntity newsEntity = new NewsEntity();
        newsEntity.setId(newsItem.getId());
        newsEntity.setTitle(newsItem.getTitle());
        newsEntity.setUrl(newsItem.getUrl()); // Fixed: should be getUrl(), not getImageUrl()
        newsEntity.setImageUrl(newsItem.getImageUrl()); // Add this line
        // Set other properties as needed
        
        // Use existing navigation logic
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putInt("NEWS_ID", newsEntity.getId());
        navController.navigate(R.id.action_newsFragment_to_newsDetailFragment, args);
    }

    // Add this helper method to convert NewsEntity objects to NewsItem objects
    private void updateAdapterList() {
        newsItemList.clear();
        for (NewsEntity entity : newsList) {
            NewsItem item = new NewsItem();
            item.setId(entity.getId());
            item.setTitle(entity.getTitle());
            item.setImageUrl(entity.getImageUrl());
            item.setUrl(entity.getUrl());
            // Copy other properties as needed
            
            newsItemList.add(item);
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
        if (executor != null) {
            executor.shutdown();
        }
    }
}
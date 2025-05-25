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

import com.example.glean.R;
import com.example.glean.adapter.NewsAdapter;
import com.example.glean.api.NewsApiService;
import com.example.glean.util.ApiConfig;  // Fixed import statement
import com.example.glean.databinding.FragmentNewsBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.NewsEntity;
import com.example.glean.model.NewsItem;
import com.example.glean.model.NewsResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NewsFragment extends Fragment implements NewsAdapter.OnNewsItemClickListener {

    private FragmentNewsBinding binding;
    private NewsAdapter adapter;
    private List<NewsItem> newsList = new ArrayList<>();
    private AppDatabase db;
    private ExecutorService executor;
    private NewsApiService newsService;
    private boolean isLoading = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        // Initialize Retrofit with ApiConfig
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiConfig.NEWS_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        newsService = retrofit.create(NewsApiService.class);
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
        
        // Setup RecyclerView
        binding.rvNews.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NewsAdapter(newsList, this);
        binding.rvNews.setAdapter(adapter);
        
        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener(this::fetchNews);
        binding.swipeRefresh.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorPrimaryDark,
                R.color.colorAccent
        );
        
        // Load cached news first, then fetch from API
        loadCachedNews();
    }
    
    private void loadCachedNews() {
        binding.swipeRefresh.setRefreshing(true);
        
        executor.execute(() -> {
            List<NewsEntity> cachedNews = db.newsDao().getAllNews().getValue();
            
            requireActivity().runOnUiThread(() -> {
                if (cachedNews != null && !cachedNews.isEmpty()) {
                    newsList.clear();
                    
                    for (NewsEntity entity : cachedNews) {
                        newsList.add(new NewsItem(
                                entity.getId(),
                                entity.getTitle(),
                                entity.getPreview(),
                                entity.getImageUrl(),
                                entity.getDate(),
                                entity.getSource()
                        ));
                    }
                    
                    adapter.notifyDataSetChanged();
                    binding.swipeRefresh.setRefreshing(false);
                }
                
                // Fetch latest news from API
                fetchNews();
            });
        });
    }
    
    private void fetchNews() {
        if (isLoading) return;
        isLoading = true;
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Use ApiConfig for API key and other parameters
        newsService.getNews(
                ApiConfig.DEFAULT_NEWS_QUERY, 
                ApiConfig.getNewsApiKey(),
                ApiConfig.NEWS_LANGUAGE, 
                ApiConfig.NEWS_SORT_BY, 
                ApiConfig.NEWS_PAGE_SIZE
        ).enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processNewsResponse(response.body());
                } else {
                    onApiError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                onApiError("Network error: " + t.getMessage());
            }
        });
    }
    
    private void processNewsResponse(NewsResponse response) {
        if (response.getArticles() == null || response.getArticles().isEmpty()) {
            onApiError("No news found");
            return;
        }
        
        // Convert API response to our model
        List<NewsItem> apiNews = new ArrayList<>();
        List<NewsEntity> entitiesToSave = new ArrayList<>();
        
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        
        int id = 1;
        for (NewsResponse.Article article : response.getArticles()) {
            if (article.getTitle() == null || article.getDescription() == null) continue;
            
            String dateStr = article.getPublishedAt();
            String formattedDate = dateStr;
            
            try {
                Date date = inputFormat.parse(dateStr);
                formattedDate = outputFormat.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            
            NewsItem item = new NewsItem(
                    id++,
                    article.getTitle(),
                    article.getDescription(),
                    article.getUrlToImage(),
                    formattedDate,
                    article.getSource() != null ? article.getSource().getName() : "Unknown"
            );
            
            apiNews.add(item);
            
            // Create database entity
            NewsEntity entity = new NewsEntity();
            entity.setId(item.getId());
            entity.setTitle(item.getTitle());
            entity.setPreview(item.getContent());
            entity.setImageUrl(item.getImageUrl());
            entity.setDate(item.getDate());
            entity.setSource(item.getCategory());
            entity.setFullContent(article.getContent() != null ? article.getContent() : item.getContent());
            entity.setUrl(article.getUrl());
            
            entitiesToSave.add(entity);
        }
        
        // Update UI with new data
        newsList.clear();
        newsList.addAll(apiNews);
        
        // Cache in database
        executor.execute(() -> {
            db.newsDao().deleteAll();
            for (NewsEntity entity : entitiesToSave) {
                db.newsDao().insert(entity);
            }
        });
        
        requireActivity().runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            binding.swipeRefresh.setRefreshing(false);
            isLoading = false;
        });
    }
    
    private void onApiError(String errorMessage) {
        requireActivity().runOnUiThread(() -> {
            binding.swipeRefresh.setRefreshing(false);
            isLoading = false;
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            
            // If we don't have any cached news, show fallback data
            if (newsList.isEmpty()) {
                loadFallbackNews();
            }
        });
    }
    
    private void loadFallbackNews() {
        // Fallback to static data if API fails and no cache
        newsList.add(new NewsItem(
                1,
                "The Impact of Plogging on Local Communities",
                "Discover how plogging is transforming local communities and reducing waste...",
                "https://example.com/images/news1.jpg",
                "2023-05-01",
                "Environmental News"
        ));
        newsList.add(new NewsItem(
                2,
                "5 Ways to Reduce Plastic Use in Your Daily Life",
                "Simple tips to cut down on plastic consumption and make a difference...",
                "https://example.com/images/news2.jpg",
                "2023-04-25",
                "Tips & Tricks"
        ));
        newsList.add(new NewsItem(
                3,
                "Global Plogging Day: Join the Movement",
                "Mark your calendars! Global Plogging Day is coming up...",
                "https://example.com/images/news3.jpg",
                "2023-04-20",
                "Events"
        ));
        
        adapter.notifyDataSetChanged();
    }
    
    @Override
    public void onNewsItemClick(NewsItem newsItem) {
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putInt("NEWS_ID", newsItem.getId());
        navController.navigate(R.id.action_newsFragment_to_newsDetailFragment, args);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
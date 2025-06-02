package com.example.glean.api;

import com.example.glean.model.NewsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {
    
    @GET("everything")
    Call<NewsResponse> getNews(
            @Query("q") String query,
            @Query("apiKey") String apiKey,
            @Query("language") String language,
            @Query("sortBy") String sortBy,
            @Query("pageSize") int pageSize
    );

    @GET("everything")
    Call<NewsResponse> getEnvironmentalNews(
            @Query("q") String query,
            @Query("apiKey") String apiKey,
            @Query("language") String language,
            @Query("sortBy") String sortBy,
            @Query("pageSize") int pageSize,
            @Query("domains") String domains
    );

    @GET("top-headlines")
    Call<NewsResponse> getTopHeadlines(
            @Query("category") String category,
            @Query("country") String country,
            @Query("apiKey") String apiKey,
            @Query("pageSize") int pageSize
    );
}
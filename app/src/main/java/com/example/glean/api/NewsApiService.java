package com.example.glean.api;

import com.example.glean.model.NewsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {
    
    @GET("v2/everything")
    Call<NewsResponse> getEnvironmentalNews(
            @Query("q") String query,
            @Query("apiKey") String apiKey
    );
    
    @GET("v2/everything")
    Call<NewsResponse> getNews(
            @Query("q") String query,
            @Query("apiKey") String apiKey,
            @Query("language") String language,
            @Query("sortBy") String sortBy,
            @Query("pageSize") int pageSize
    );
}
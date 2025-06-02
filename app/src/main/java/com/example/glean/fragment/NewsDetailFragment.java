package com.example.glean.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.databinding.FragmentNewsDetailBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.NewsEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsDetailFragment extends Fragment {

    private FragmentNewsDetailBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private int newsId = -1;
    private NewsEntity newsItem;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        if (getArguments() != null) {
            newsId = getArguments().getInt("NEWS_ID", -1);
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
        
        // Set click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnOpenBrowser.setOnClickListener(v -> openInBrowser());
        binding.btnShare.setOnClickListener(v -> shareNews());
        
        // Load news data
        loadNewsData();
    }
    
    private void loadNewsData() {
        if (newsId == -1) {
            binding.tvErrorMessage.setVisibility(View.VISIBLE);
            binding.tvErrorMessage.setText("Invalid news ID");
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        executor.execute(() -> {
            newsItem = db.newsDao().getNewsByIdSync(newsId);
            
            requireActivity().runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                
                if (newsItem != null) {
                    updateUI();
                } else {
                    binding.tvErrorMessage.setVisibility(View.VISIBLE);
                    binding.tvErrorMessage.setText("News not found");
                }
            });
        });
    }
    
    private void updateUI() {
        binding.tvTitle.setText(newsItem.getTitle());
        binding.tvDate.setText(newsItem.getDate());
        binding.tvSource.setText(newsItem.getSource());
        
        // Load full content if available, otherwise use preview
        String content = newsItem.getFullContent();
        if (content == null || content.isEmpty()) {
            content = newsItem.getPreview();
        }
        binding.tvContent.setText(content);
        
        // Load image if available using Glide
        if (newsItem.getImageUrl() != null && !newsItem.getImageUrl().isEmpty()) {
            binding.ivNewsImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(newsItem.getImageUrl())
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .centerCrop()
                    .into(binding.ivNewsImage);
        } else {
            binding.ivNewsImage.setVisibility(View.GONE);
        }
        
        // Enable open in browser button if URL is available
        binding.btnOpenBrowser.setEnabled(newsItem.getUrl() != null && !newsItem.getUrl().isEmpty());
    }
    
    private void openInBrowser() {
        if (newsItem != null && newsItem.getUrl() != null && !newsItem.getUrl().isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.getUrl()));
                startActivity(intent);
            } catch (Exception e) {
                // Handle case where no browser is available
                binding.tvErrorMessage.setVisibility(View.VISIBLE);
                binding.tvErrorMessage.setText("No browser app found to open the link");
            }
        }
    }
    
    private void shareNews() {
        if (newsItem == null) return;
        
        String shareText = newsItem.getTitle() + "\n\n";
        if (newsItem.getPreview() != null && !newsItem.getPreview().isEmpty()) {
            shareText += newsItem.getPreview() + "\n\n";
        }
        
        if (newsItem.getUrl() != null && !newsItem.getUrl().isEmpty()) {
            shareText += "Read more: " + newsItem.getUrl() + "\n\n";
        }
        
        shareText += "Shared from Glean - Environmental News & Plogging App";
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, newsItem.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        try {
            startActivity(Intent.createChooser(shareIntent, "Share Environmental News"));
        } catch (Exception e) {
            // Handle case where no sharing apps are available
            binding.tvErrorMessage.setVisibility(View.VISIBLE);
            binding.tvErrorMessage.setText("No sharing apps available");
        }
    }
    
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
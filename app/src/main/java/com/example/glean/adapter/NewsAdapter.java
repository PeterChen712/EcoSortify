package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.databinding.ItemNewsBinding;
import com.example.glean.model.NewsItem;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final List<NewsItem> newsList;
    private final OnNewsItemClickListener listener;

    public interface OnNewsItemClickListener {
        void onNewsItemClick(NewsItem newsItem);
    }

    public NewsAdapter(List<NewsItem> newsList, OnNewsItemClickListener listener) {
        this.newsList = newsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNewsBinding binding = ItemNewsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NewsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem newsItem = newsList.get(position);
        holder.bind(newsItem, listener);
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsBinding binding;

        public NewsViewHolder(ItemNewsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(NewsItem newsItem, OnNewsItemClickListener listener) {
            // Set title
            binding.tvTitle.setText(newsItem.getTitle());
            
            // Set preview/description
            binding.tvPreview.setText(newsItem.getContent());
            
            // Set date
            binding.tvDate.setText(newsItem.getDate());
            
            // Set category/source
            binding.tvCategory.setText(newsItem.getCategory());
            
            // Load image with Glide
            if (newsItem.getImageUrl() != null && !newsItem.getImageUrl().isEmpty()) {
                Glide.with(binding.ivNewsThumbnail.getContext())
                        .load(newsItem.getImageUrl())
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error)
                        .centerCrop()
                        .into(binding.ivNewsThumbnail);
            } else {
                // Set default placeholder when no image - use existing placeholder
                Glide.with(binding.ivNewsThumbnail.getContext())
                        .load(R.drawable.ic_news_placeholder)
                        .placeholder(R.drawable.ic_placeholder)
                        .into(binding.ivNewsThumbnail);
            }
            
            // Set click listener
            binding.cardNews.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNewsItemClick(newsItem);
                }
            });
        }
    }
}
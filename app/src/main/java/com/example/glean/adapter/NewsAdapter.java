package com.example.glean.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.glean.R;
import com.example.glean.databinding.ItemNewsBinding;
import com.example.glean.model.NewsItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final List<NewsItem> newsList;
    private final OnNewsItemClickListener listener;
    private final Context context;

    public interface OnNewsItemClickListener {
        void onNewsItemClick(NewsItem newsItem);
    }

    public interface OnNewsItemLongClickListener extends OnNewsItemClickListener {
        void onNewsItemLongClick(NewsItem newsItem);
    }

    public NewsAdapter(Context context, List<NewsItem> newsList, OnNewsItemClickListener listener) {
        this.context = context;
        this.newsList = newsList != null ? newsList : new ArrayList<>();
        this.listener = listener;
    }

    // Method to update news list with smooth animations
    public void updateNewsList(List<NewsItem> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NewsDiffCallback(this.newsList, newItems));
        this.newsList.clear();
        this.newsList.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    // Method to add single news item with animation
    public void addNewsItem(NewsItem newsItem) {
        newsList.add(0, newsItem);
        notifyItemInserted(0);
    }

    // Method to remove news item
    public void removeNewsItem(int position) {
        if (position >= 0 && position < newsList.size()) {
            newsList.remove(position);
            notifyItemRemoved(position);
        }
    }

    // Method to mark all as read
    public void markAllAsRead() {
        boolean hasChanges = false;
        for (NewsItem item : newsList) {
            if (!item.isRead()) {
                item.setRead(true);
                hasChanges = true;
            }
        }
        if (hasChanges) {
            notifyDataSetChanged();
        }
    }

    // Method to get unread count
    public int getUnreadCount() {
        int count = 0;
        for (NewsItem item : newsList) {
            if (!item.isRead()) {
                count++;
            }
        }
        return count;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNewsBinding binding = ItemNewsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NewsViewHolder(binding);
    }    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem newsItem = newsList.get(position);
        holder.bind(newsItem, listener);
        
        // Add entrance animation - Use view tag to track animation state instead of position
        if (holder.itemView.getTag() == null) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
            holder.itemView.startAnimation(animation);
            holder.itemView.setTag("animated");
        }
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsBinding binding;

        public NewsViewHolder(ItemNewsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }        public void bind(NewsItem newsItem, OnNewsItemClickListener listener) {
            Context context = binding.getRoot().getContext();
            
            // Set title with improved styling
            if (newsItem.getTitle() != null && !newsItem.getTitle().trim().isEmpty()) {
                // Clean HTML tags from title
                String cleanTitle = Html.fromHtml(newsItem.getTitle(), Html.FROM_HTML_MODE_COMPACT).toString();
                binding.tvTitle.setText(cleanTitle);
            } else {
                binding.tvTitle.setText("📰 News Article");
            }
            
            // Set preview/description with HTML support - FIXED: Use getPreview() instead of getShortPreview()
            String preview = newsItem.getPreview();
            if (preview != null && !preview.trim().isEmpty()) {
                // Clean HTML and limit preview length
                String cleanPreview = Html.fromHtml(preview, Html.FROM_HTML_MODE_COMPACT).toString();
                if (cleanPreview.length() > 120) {
                    cleanPreview = cleanPreview.substring(0, 117) + "...";
                }
                binding.tvPreview.setText(cleanPreview);
                binding.tvPreview.setVisibility(View.VISIBLE);
            } else {
                binding.tvPreview.setText("📖 Tap to read more");
                binding.tvPreview.setVisibility(View.VISIBLE);
            }
            
            // Set formatted date with relative time
            String formattedDate = getRelativeTimeString(newsItem.getFormattedDate());
            binding.tvDate.setText(formattedDate);
            binding.tvDate.setVisibility(View.VISIBLE);
            
            // Set category/source with modern styling
            String category = newsItem.getCategory();
            String source = newsItem.getSource();
            
            if (source != null && !source.trim().isEmpty()) {
                String categoryText;
                if (category != null && !category.trim().isEmpty()) {
                    categoryText = String.format("🌱 %s • %s", source, category.toUpperCase());
                } else {
                    categoryText = String.format("🌱 %s", source);
                }
                binding.tvCategory.setText(categoryText);
                binding.tvCategory.setVisibility(View.VISIBLE);
                
                // Set dynamic category background
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(16f);
                
                try {
                    // FIXED: Use a simple color approach since getCategoryColor() might not exist
                    // Set color based on category
                    int colorResId;
                    if (category != null) {
                        switch (category.toLowerCase()) {
                            case "climate":
                            case "environment":
                                colorResId = R.color.environmental_green;
                                break;
                            case "sustainability":
                                colorResId = R.color.sustainability_blue;
                                break;
                            case "renewable":
                                colorResId = R.color.renewable_orange;
                                break;
                            default:
                                colorResId = R.color.environmental_green;
                        }
                    } else {
                        colorResId = R.color.environmental_green;
                    }
                    
                    drawable.setColor(ContextCompat.getColor(context, colorResId));
                    binding.tvCategory.setBackground(drawable);
                    binding.tvCategory.setTextColor(android.graphics.Color.WHITE);
                } catch (Exception e) {
                    // Use default environmental green
                    drawable.setColor(ContextCompat.getColor(context, R.color.environmental_green));
                    binding.tvCategory.setBackground(drawable);
                    binding.tvCategory.setTextColor(android.graphics.Color.WHITE);
                }
                
                binding.tvCategory.setPadding(24, 8, 24, 8);
            } else {
                binding.tvCategory.setVisibility(View.GONE);
            }
            
            // Load image with advanced options
            if (newsItem.getImageUrl() != null && !newsItem.getImageUrl().isEmpty() 
                && !newsItem.getImageUrl().equals("null")) {
                
                binding.ivNewsThumbnail.setVisibility(View.VISIBLE);
                
                RequestOptions options = new RequestOptions()
                        .placeholder(R.drawable.ic_news_loading)
                        .error(R.drawable.ic_news_placeholder)
                        .transform(new CenterCrop(), new RoundedCorners(16))
                        .timeout(10000); // 10 second timeout
                
                Glide.with(context)
                        .load(newsItem.getImageUrl())
                        .apply(options)
                        .thumbnail(0.1f)
                        .into(binding.ivNewsThumbnail);
            } else {
                // Show beautiful default placeholder
                binding.ivNewsThumbnail.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(R.drawable.ic_environmental_news)
                        .apply(new RequestOptions().transform(new RoundedCorners(16)))
                        .into(binding.ivNewsThumbnail);
            }
            
            // Set read state with improved styling
            if (newsItem.isRead()) {
                binding.tvTitle.setAlpha(0.6f);
                binding.tvPreview.setAlpha(0.6f);
                binding.ivNewsThumbnail.setAlpha(0.7f);
                binding.cardNews.setCardElevation(2f);
                
                // Add read indicator
                binding.tvTitle.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.ic_read_check, 0);
            } else {
                binding.tvTitle.setAlpha(1.0f);
                binding.tvPreview.setAlpha(1.0f);
                binding.ivNewsThumbnail.setAlpha(1.0f);
                binding.cardNews.setCardElevation(6f);
                
                // Add unread indicator
                binding.tvTitle.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_unread_dot, 0, 0, 0);
                binding.tvTitle.setCompoundDrawablePadding(12);
            }
            
            // Add modern card styling
            binding.cardNews.setCardBackgroundColor(
                ContextCompat.getColor(context, newsItem.isRead() ? 
                    R.color.card_background_read : R.color.card_background_unread));
            
            binding.cardNews.setRadius(16f);
            binding.cardNews.setUseCompatPadding(true);
            
            // Set click listener with enhanced feedback
            binding.cardNews.setOnClickListener(v -> {
                if (listener != null) {
                    // Add scale animation
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start();
                        })
                        .start();
                      // Mark as read when clicked
                    boolean wasRead = newsItem.isRead();
                    newsItem.setRead(true);
                    if (!wasRead) {
                        bind(newsItem, listener); // Refresh styling
                    }
                    
                    // Add haptic feedback
                    try {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    } catch (Exception e) {
                        // Ignore if haptic feedback not available
                    }
                    
                    listener.onNewsItemClick(newsItem);
                }
            });
            
            // Add long click for additional options
            binding.cardNews.setOnLongClickListener(v -> {
                if (listener instanceof NewsAdapter.OnNewsItemLongClickListener) {
                    try {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    } catch (Exception e) {
                        // Ignore if haptic feedback not available
                    }
                    
                    // Add visual feedback for long press
                    v.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(150)
                        .withEndAction(() -> {
                            v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(150)
                                .start();
                        })
                        .start();
                    
                    ((NewsAdapter.OnNewsItemLongClickListener) listener).onNewsItemLongClick(newsItem);
                    return true;
                }
                return false;
            });
        }
        
        private String getRelativeTimeString(String dateString) {
            if (dateString == null || dateString.equals("Unknown date")) {
                return "📅 Recently";
            }
            
            // Add time emoji based on relative time
            if (dateString.contains("hour") || dateString.contains("minute")) {
                return "🕒 " + dateString;
            } else if (dateString.contains("day")) {
                return "📅 " + dateString;
            } else if (dateString.contains("week")) {
                return "📆 " + dateString;
            } else {
                return "🗓️ " + dateString;
            }
        }
    }

    // DiffUtil callback for efficient list updates
    private static class NewsDiffCallback extends DiffUtil.Callback {
        private final List<NewsItem> oldList;
        private final List<NewsItem> newList;

        public NewsDiffCallback(List<NewsItem> oldList, List<NewsItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }
        
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            NewsItem oldItem = oldList.get(oldItemPosition);
            NewsItem newItem = newList.get(newItemPosition);
            
            // Compare by URL or title as unique identifier
            if (oldItem.getUrl() != null && newItem.getUrl() != null) {
                return oldItem.getUrl().equals(newItem.getUrl());
            }
            if (oldItem.getTitle() != null && newItem.getTitle() != null) {
                return oldItem.getTitle().equals(newItem.getTitle());
            }
            return false;
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            NewsItem oldItem = oldList.get(oldItemPosition);
            NewsItem newItem = newList.get(newItemPosition);
            
            // Safe comparison with null checks
            boolean titleEquals = (oldItem.getTitle() == null && newItem.getTitle() == null) ||
                                (oldItem.getTitle() != null && oldItem.getTitle().equals(newItem.getTitle()));
            
            boolean dateEquals = (oldItem.getFormattedDate() == null && newItem.getFormattedDate() == null) ||
                                (oldItem.getFormattedDate() != null && oldItem.getFormattedDate().equals(newItem.getFormattedDate()));
            
            return titleEquals &&
                   oldItem.isRead() == newItem.isRead() &&
                   dateEquals &&
                   oldItem.isFavorite() == newItem.isFavorite();
        }
    }
}

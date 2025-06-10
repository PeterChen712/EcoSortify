package com.example.glean.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.glean.R;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.PostEntity;
import com.example.glean.model.UserEntity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    
    private List<PostEntity> posts;
    private OnPostClickListener listener;
    private AppDatabase database;
    private ExecutorService executor;
    
    public interface OnPostClickListener {
        void onPostClick(PostEntity post);
        void onLikeClick(PostEntity post);
        void onCommentClick(PostEntity post);
        void onShareClick(PostEntity post);
    }
    
    public PostAdapter(List<PostEntity> posts, OnPostClickListener listener) {
        this.posts = posts;
        this.listener = listener;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        
        // Initialize database if not already done
        if (database == null) {
            database = AppDatabase.getInstance(parent.getContext());
        }
        
        return new PostViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostEntity post = posts.get(position);
        holder.bind(post);
    }
    
    @Override
    public int getItemCount() {
        return posts.size();
    }
    
    class PostViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUsername, tvContent, tvTimestamp, tvLikeCount, tvCommentCount;
        private ImageView ivUserAvatar, ivPostImage, ivLike, ivComment, ivShare;
          public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUserName);
            tvContent = itemView.findViewById(R.id.tvCaption);
            tvTimestamp = itemView.findViewById(R.id.tvPostTime);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            ivUserAvatar = itemView.findViewById(R.id.ivUserProfile);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivLike = itemView.findViewById(R.id.ivLike);
            ivComment = itemView.findViewById(R.id.ivComment);
            ivShare = itemView.findViewById(R.id.ivShare);
        }        public void bind(PostEntity post) {
            tvUsername.setText(post.getUsername());
            tvContent.setText(post.getContent());
            tvLikeCount.setText(String.valueOf(post.getLikeCount()));
            tvCommentCount.setText(String.valueOf(post.getCommentCount()));
            
            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm • dd MMM", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(post.getTimestamp())));
            
            // Always load the latest profile image from database based on userId
            // This ensures photo synchronization when user updates their profile picture
            loadLatestUserProfileImage(post.getUserId());

            // Load post image using Glide
            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                Log.d("PostAdapter", "Loading image for post: " + post.getImageUrl());
                ivPostImage.setVisibility(View.VISIBLE);
                
                RequestOptions requestOptions = new RequestOptions()
                        .placeholder(R.drawable.ic_empty_posts)
                        .error(R.drawable.ic_empty_posts)
                        .transform(new RoundedCorners(16));
                
                Glide.with(itemView.getContext())
                        .load(post.getImageUrl())
                        .apply(requestOptions)
                        .into(ivPostImage);
            } else {
                Log.d("PostAdapter", "No image URL for post, hiding image view");
                ivPostImage.setVisibility(View.GONE);
            }
            
            // Set like state
            ivLike.setImageResource(post.isLiked() ? 
                R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            
            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPostClick(post);
            });
            
            ivLike.setOnClickListener(v -> {
                if (listener != null) listener.onLikeClick(post);
            });
            
            ivComment.setOnClickListener(v -> {
                if (listener != null) listener.onCommentClick(post);
            });
              ivShare.setOnClickListener(v -> {
                if (listener != null) listener.onShareClick(post);
            });
        }
        
        private void loadLatestUserProfileImage(int userId) {
            // Load the latest user profile image from database
            executor.execute(() -> {
                try {
                    UserEntity user = database.userDao().getUserByIdSync(userId);
                    String latestProfilePath = (user != null) ? user.getProfileImagePath() : null;
                    
                    // Update UI on main thread
                    itemView.post(() -> {
                        if (latestProfilePath != null && !latestProfilePath.isEmpty()) {
                            File imageFile = new File(latestProfilePath);
                            if (imageFile.exists()) {
                                Glide.with(ivUserAvatar.getContext())
                                        .load(imageFile)
                                        .placeholder(R.drawable.profile_placeholder)
                                        .error(R.drawable.profile_placeholder)
                                        .circleCrop()
                                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache to always get fresh image
                                        .skipMemoryCache(true) // Skip memory cache for fresh image
                                        .signature(new com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis())) // Force refresh
                                        .into(ivUserAvatar);
                            } else {
                                // File doesn't exist, load default
                                ivUserAvatar.setImageResource(R.drawable.profile_placeholder);
                            }
                        } else {
                            // No profile image set, load default
                            ivUserAvatar.setImageResource(R.drawable.profile_placeholder);
                        }
                    });
                } catch (Exception e) {
                    // Handle error and load default image
                    itemView.post(() -> {
                        ivUserAvatar.setImageResource(R.drawable.profile_placeholder);
                    });
                }
            });
        }
    }
    
    // Method to refresh all profile images (useful when coming back from profile edit)
    public void refreshProfileImages() {
        notifyDataSetChanged();
    }
    
    // Clean up resources
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}

package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.glean.R;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.CommentEntity;
import com.example.glean.model.UserEntity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    
    private List<CommentEntity> comments;
    private AppDatabase database;
    private ExecutorService executor;
    
    public CommentAdapter(List<CommentEntity> comments) {
        this.comments = comments;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        
        // Initialize database if not already done
        if (database == null) {
            database = AppDatabase.getInstance(parent.getContext());
        }
        
        return new CommentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommentEntity comment = comments.get(position);
        holder.bind(comment);
    }
      @Override
    public int getItemCount() {
        return comments.size();
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
    class CommentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUsername, tvContent, tvTimestamp;
        private ImageView ivUserAvatar;
        
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvCommentUsername);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvTimestamp = itemView.findViewById(R.id.tvCommentTime);
            ivUserAvatar = itemView.findViewById(R.id.ivCommentUserAvatar);
        }        public void bind(CommentEntity comment) {
            tvUsername.setText(comment.getUsername());
            tvContent.setText(comment.getContent());
            
            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm • dd MMM", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(comment.getTimestamp())));
            
            // Always load the latest profile image from database based on userId
            // This ensures photo synchronization when user updates their profile picture
            loadLatestUserProfileImage(comment.getUserId());
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
                                Glide.with(itemView.getContext())
                                        .load(imageFile)
                                        .placeholder(R.drawable.ic_user_placeholder)
                                        .error(R.drawable.ic_user_placeholder)
                                        .circleCrop()
                                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache to always get fresh image
                                        .skipMemoryCache(true) // Skip memory cache for fresh image
                                        .signature(new com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis())) // Force refresh
                                        .into(ivUserAvatar);
                            } else {
                                // File doesn't exist, load default
                                ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder);
                            }
                        } else {
                            // No profile image set, load default
                            ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder);
                        }
                    });
                } catch (Exception e) {
                    // Handle error and load default image
                    itemView.post(() -> {
                        ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder);
                    });
                }
            });
        }
    }
}
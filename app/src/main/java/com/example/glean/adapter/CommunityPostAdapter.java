package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.databinding.ItemCommunityPostBinding;
import com.example.glean.model.CommunityPostModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityPostAdapter extends RecyclerView.Adapter<CommunityPostAdapter.PostViewHolder> {

    private final List<CommunityPostModel> postList;
    private final OnPostInteractionListener listener;

    public interface OnPostInteractionListener {
        void onLikeClick(CommunityPostModel post, int position);
        void onCommentClick(CommunityPostModel post, int position);
        void onShareClick(CommunityPostModel post, int position);
        void onUserClick(CommunityPostModel post);
    }

    public CommunityPostAdapter(List<CommunityPostModel> postList, OnPostInteractionListener listener) {
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommunityPostBinding binding = ItemCommunityPostBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(postList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommunityPostBinding binding;

        public PostViewHolder(ItemCommunityPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CommunityPostModel post, int position) {
            // User info
            binding.tvUserName.setText(post.getUserName());
              // Profile image
            if (post.getUserProfileUrl() != null && !post.getUserProfileUrl().isEmpty()) {
                // Load from file path
                File imageFile = new File(post.getUserProfileUrl());
                if (imageFile.exists()) {
                    Glide.with(binding.ivUserProfile.getContext())
                            .load(imageFile)
                            .placeholder(R.drawable.profile_placeholder)
                            .error(R.drawable.profile_placeholder)
                            .circleCrop()
                            .into(binding.ivUserProfile);
                } else {
                    // File doesn't exist, load default
                    binding.ivUserProfile.setImageResource(R.drawable.profile_placeholder);
                }
            } else {
                // No profile image set, load default
                binding.ivUserProfile.setImageResource(R.drawable.profile_placeholder);
            }

            // Post content
            binding.tvContent.setText(post.getContent());
            
            // Post image
            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                binding.ivPostImage.setVisibility(View.VISIBLE);
                Glide.with(binding.ivPostImage.getContext())
                        .load(post.getImageUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(binding.ivPostImage);
            } else {
                binding.ivPostImage.setVisibility(View.GONE);
            }

            // Timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            binding.tvTimestamp.setText(sdf.format(new Date(post.getTimestamp())));

            // Location
            if (post.getLocation() != null && !post.getLocation().isEmpty()) {
                binding.tvLocation.setVisibility(View.VISIBLE);
                binding.tvLocation.setText("📍 " + post.getLocation());
            } else {
                binding.tvLocation.setVisibility(View.GONE);
            }

            // Category badge
            if (post.getCategory() != null) {
                binding.tvCategory.setVisibility(View.VISIBLE);
                binding.tvCategory.setText(post.getCategory().toUpperCase());
                
                // Set category color
                switch (post.getCategory().toLowerCase()) {
                    case "plogging":
                        binding.tvCategory.setBackgroundResource(R.drawable.bg_category_plogging);
                        break;
                    case "achievement":
                        binding.tvCategory.setBackgroundResource(R.drawable.bg_category_achievement);
                        break;
                    case "tips":
                        binding.tvCategory.setBackgroundResource(R.drawable.bg_category_tips);
                        break;
                    default:
                        binding.tvCategory.setBackgroundResource(R.drawable.bg_category_default);
                }
            } else {
                binding.tvCategory.setVisibility(View.GONE);
            }

            // Interaction counts
            binding.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
            binding.tvCommentCount.setText(String.valueOf(post.getCommentCount()));

            // Click listeners
            binding.layoutUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(post);
                }
            });

            binding.btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(post, position);
                }
            });

            binding.btnComment.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentClick(post, position);
                }
            });

            binding.btnShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClick(post, position);
                }
            });
        }
    }
}
package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.databinding.ItemCommentBinding;
import com.example.glean.model.CommentModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<CommentModel> commentList;
    private final OnCommentInteractionListener listener;

    public interface OnCommentInteractionListener {
        void onCommentLike(CommentModel comment, int position);
        void onCommentReply(CommentModel comment, int position);
        void onUserClick(CommentModel comment);
    }

    public CommentAdapter(List<CommentModel> commentList, OnCommentInteractionListener listener) {
        this.commentList = commentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommentBinding binding = ItemCommentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CommentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(commentList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommentBinding binding;

        public CommentViewHolder(ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CommentModel comment, int position) {
            // User name
            binding.tvUserName.setText(comment.getUserName());
            
            // Profile image
            if (comment.getUserProfileUrl() != null && !comment.getUserProfileUrl().isEmpty()) {
                Glide.with(binding.ivUserProfile.getContext())
                        .load(comment.getUserProfileUrl())
                        .placeholder(R.drawable.profile_placeholder)
                        .circleCrop()
                        .into(binding.ivUserProfile);
            } else {
                binding.ivUserProfile.setImageResource(R.drawable.profile_placeholder);
            }

            // Comment content
            binding.tvComment.setText(comment.getContent());
            
            // Timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            binding.tvTimestamp.setText(sdf.format(new Date(comment.getTimestamp())));

            // Like count
            if (comment.getLikeCount() > 0) {
                binding.tvLikeCount.setVisibility(View.VISIBLE);
                binding.tvLikeCount.setText(String.valueOf(comment.getLikeCount()));
            } else {
                binding.tvLikeCount.setVisibility(View.GONE);
            }

            // Click listeners
            binding.layoutUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(comment);
                }
            });

            binding.btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentLike(comment, position);
                }
            });

            binding.btnReply.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentReply(comment, position);
                }
            });
        }
    }
}
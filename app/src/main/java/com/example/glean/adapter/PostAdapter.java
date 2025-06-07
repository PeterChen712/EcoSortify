package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.PostEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    
    private List<PostEntity> posts;
    private OnPostClickListener listener;
    
    public interface OnPostClickListener {
        void onPostClick(PostEntity post);
        void onLikeClick(PostEntity post);
        void onCommentClick(PostEntity post);
        void onShareClick(PostEntity post);
    }
    
    public PostAdapter(List<PostEntity> posts, OnPostClickListener listener) {
        this.posts = posts;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
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
        }
        
        public void bind(PostEntity post) {
            tvUsername.setText(post.getUsername());
            tvContent.setText(post.getContent());
            tvLikeCount.setText(String.valueOf(post.getLikeCount()));
            tvCommentCount.setText(String.valueOf(post.getCommentCount()));
            
            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm • dd MMM", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(post.getTimestamp())));
            
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
    }
}

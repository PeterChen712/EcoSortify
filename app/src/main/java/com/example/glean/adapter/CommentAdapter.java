package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.model.CommentEntity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    
    private List<CommentEntity> comments;
    
    public CommentAdapter(List<CommentEntity> comments) {
        this.comments = comments;
    }
    
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
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
    class CommentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUsername, tvContent, tvTimestamp;
        private ImageView ivUserAvatar;
        
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvCommentUsername);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvTimestamp = itemView.findViewById(R.id.tvCommentTime);
            ivUserAvatar = itemView.findViewById(R.id.ivCommentUserAvatar);
        }
          public void bind(CommentEntity comment) {
            tvUsername.setText(comment.getUsername());
            tvContent.setText(comment.getContent());
            
            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm • dd MMM", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(comment.getTimestamp())));
            
            // Load profile image from file path
            if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
                File imageFile = new File(comment.getUserAvatar());
                if (imageFile.exists()) {
                    Glide.with(itemView.getContext())
                            .load(imageFile)
                            .placeholder(R.drawable.ic_user_placeholder)
                            .error(R.drawable.ic_user_placeholder)
                            .circleCrop()
                            .into(ivUserAvatar);
                } else {
                    // File doesn't exist, load default
                    ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder);
                }
            } else {
                // No profile image set, load default
                ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder);
            }
        }
    }
}
package com.example.glean.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.adapter.CommentAdapter;
import com.example.glean.databinding.ActivityPostDetailBinding;
import com.example.glean.model.CommentEntity;
import com.example.glean.model.PostEntity;
import com.example.glean.repository.CommunityRepository;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {
    
    private ActivityPostDetailBinding binding;
    private PostEntity post;
    private CommentAdapter commentAdapter;
    private List<CommentEntity> comments = new ArrayList<>();
    private CommunityRepository repository;
      @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        repository = new CommunityRepository(this);
        
        // Get post data from intent
        int postId = getIntent().getIntExtra("post_id", 0);
        int userId = getIntent().getIntExtra("user_id", 0);
        String username = getIntent().getStringExtra("username");
        String content = getIntent().getStringExtra("content");
        int likeCount = getIntent().getIntExtra("like_count", 0);
        int commentCount = getIntent().getIntExtra("comment_count", 0);
        long timestamp = getIntent().getLongExtra("timestamp", System.currentTimeMillis());
        boolean isLiked = getIntent().getBooleanExtra("is_liked", false);
        boolean focusComment = getIntent().getBooleanExtra("focus_comment", false);
        // Set avatar and post image from intent extras
        String userAvatar = getIntent().getStringExtra("user_avatar");
        String imageUrl = getIntent().getStringExtra("image_url");
        
        // Create post object
        post = new PostEntity();
        post.setId(postId);
        post.setUserId(userId);
        post.setUsername(username);
        post.setContent(content);
        post.setLikeCount(likeCount);
        post.setCommentCount(commentCount);
        post.setTimestamp(timestamp);
        post.setLiked(isLiked);
        post.setUserAvatar(userAvatar);
        post.setImageUrl(imageUrl);
        
        setupToolbar();
        setupPostData();
        setupRecyclerView();
        setupClickListeners();
        loadComments();
        
        // Focus on comment input if requested
        if (focusComment) {
            binding.etComment.requestFocus();
            // You can also show keyboard here if needed
        }
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detail Post");
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    
    private void setupPostData() {
        binding.tvUsername.setText(post.getUsername());
        binding.tvContent.setText(post.getContent());
        binding.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        binding.tvCommentCount.setText(String.valueOf(post.getCommentCount()));
        
        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm • dd MMM yyyy", Locale.getDefault());
        binding.tvTimestamp.setText(sdf.format(new Date(post.getTimestamp())));
        
        // Set like state
        updateLikeButton();
        // Load profile image
        if (post.getUserAvatar() != null && !post.getUserAvatar().isEmpty()) {
            File avatarFile = new File(post.getUserAvatar());
            if (avatarFile.exists()) {
                Glide.with(this)
                        .load(avatarFile)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .circleCrop()
                        .into(binding.ivUserProfile);
            } else {
                binding.ivUserProfile.setImageResource(R.drawable.profile_placeholder);
            }
        } else {
            binding.ivUserProfile.setImageResource(R.drawable.profile_placeholder);
        }
        // Load post image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            binding.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(post.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivPostImage);
        } else {
            binding.ivPostImage.setVisibility(View.GONE);
        }
    }
    
    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter(comments);
        binding.recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewComments.setAdapter(commentAdapter);
    }
    
    private void setupClickListeners() {
        binding.btnLike.setOnClickListener(v -> {
            toggleLike();
        });
        
        binding.btnComment.setOnClickListener(v -> {
            binding.etComment.requestFocus();
        });
        
        binding.btnShare.setOnClickListener(v -> {
            sharePost();
        });
        
        binding.btnSendComment.setOnClickListener(v -> {
            sendComment();
        });
    }
      private void toggleLike() {
        post.setLiked(!post.isLiked());
        if (post.isLiked()) {
            post.setLikeCount(post.getLikeCount() + 1);
        } else {
            post.setLikeCount(post.getLikeCount() - 1);
        }
        
        // Update in database
        repository.updatePostLike(post.getId(), post.getLikeCount(), post.isLiked());
        
        updateLikeButton();
        binding.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        
        // Send result back to update the list
        Intent resultIntent = new Intent();
        resultIntent.putExtra("post_id", post.getId());
        resultIntent.putExtra("like_count", post.getLikeCount());
        resultIntent.putExtra("is_liked", post.isLiked());
        resultIntent.putExtra("comment_count", post.getCommentCount());
        setResult(RESULT_OK, resultIntent);
    }
    
    private void updateLikeButton() {
        if (post.isLiked()) {
            binding.btnLike.setImageResource(R.drawable.ic_heart_filled);
            binding.btnLike.setColorFilter(getResources().getColor(R.color.red_heart));
        } else {
            binding.btnLike.setImageResource(R.drawable.ic_heart_outline);
            binding.btnLike.setColorFilter(getResources().getColor(R.color.text_secondary));
        }
    }
    
    private void sharePost() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareText = post.getUsername() + ": " + post.getContent() + "\n\n#PloggingChallenge #Glean";
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Bagikan Post"));
    }
      private void sendComment() {
        String commentText = binding.etComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Tulis komentar terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create new comment
        CommentEntity comment = new CommentEntity();
        comment.setPostId(post.getId());
        comment.setUserId(1); // In real app, get from user session
        comment.setUsername("You"); // In real app, get from user session
        comment.setContent(commentText);
        comment.setTimestamp(System.currentTimeMillis());
        
        // Save to database
        repository.insertComment(comment, new CommunityRepository.OnCommentInsertedListener() {
            @Override
            public void onCommentInserted(CommentEntity insertedComment) {
                runOnUiThread(() -> {
                    // Add to list and update UI
                    comments.add(0, insertedComment);
                    commentAdapter.notifyItemInserted(0);
                    binding.recyclerViewComments.scrollToPosition(0);
                    
                    // Update comment count
                    post.setCommentCount(post.getCommentCount() + 1);
                    binding.tvCommentCount.setText(String.valueOf(post.getCommentCount()));
                    
                    // Clear input
                    binding.etComment.setText("");
                    
                    // Send result back
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("post_id", post.getId());
                    resultIntent.putExtra("like_count", post.getLikeCount());
                    resultIntent.putExtra("is_liked", post.isLiked());
                    resultIntent.putExtra("comment_count", post.getCommentCount());
                    setResult(RESULT_OK, resultIntent);
                    
                    Toast.makeText(PostDetailActivity.this, "Komentar berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
      private void loadComments() {
        // Load comments from database
        repository.getCommentsByPostId(post.getId()).observe(this, new Observer<List<CommentEntity>>() {
            @Override
            public void onChanged(List<CommentEntity> commentEntities) {                comments.clear();
                if (commentEntities != null && !commentEntities.isEmpty()) {
                    comments.addAll(commentEntities);
                } else {
                    // No sample comments - let the post show 0 comments as intended
                    // initializeSampleComments(); // REMOVED: This was adding unwanted sample comments
                }
                commentAdapter.notifyDataSetChanged();
            }
        });
    }
    
    private void initializeSampleComments() {
        CommentEntity comment1 = new CommentEntity();
        comment1.setPostId(post.getId());
        comment1.setUserId(2);
        comment1.setUsername("EcoFriend");
        comment1.setContent("Keren banget! Aku juga mau ikut plogging");
        comment1.setTimestamp(System.currentTimeMillis() - 1800000); // 30 min ago
        repository.insertComment(comment1, null);
        
        CommentEntity comment2 = new CommentEntity();
        comment2.setPostId(post.getId());
        comment2.setUserId(3);
        comment2.setUsername("GreenLover");
        comment2.setContent("Semangat terus! Bumi butuh lebih banyak orang seperti kamu 🌍");
        comment2.setTimestamp(System.currentTimeMillis() - 3600000); // 1 hour ago
        repository.insertComment(comment2, null);
    }
}

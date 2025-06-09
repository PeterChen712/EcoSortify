package com.example.glean.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.adapter.CommentAdapter;
import com.example.glean.databinding.FragmentPostDetailBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.CommentEntity;
import com.example.glean.model.PostEntity;
import com.example.glean.model.UserEntity;
import com.example.glean.repository.CommunityRepository;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostDetailFragment extends Fragment {

    private FragmentPostDetailBinding binding;
    private AppDatabase database;
    private CommunityRepository repository;
    private CommentAdapter commentAdapter;
    private List<CommentEntity> commentList;
    private int postId;
    private PostEntity currentPost;
    private ExecutorService executor;
    private int currentUserId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = AppDatabase.getInstance(requireContext());
        repository = new CommunityRepository(requireContext());
        commentList = new ArrayList<>();
        executor = Executors.newSingleThreadExecutor();
        
        // Get current user ID from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        currentUserId = prefs.getInt("USER_ID", -1);
        
        // Get post ID from arguments
        if (getArguments() != null) {
            postId = getArguments().getInt("POST_ID", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPostDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupUI();
        loadPostDetails();
        loadComments();
    }    private void setupUI() {
        // Setup RecyclerView for comments
        commentAdapter = new CommentAdapter(new ArrayList<>());
        binding.rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvComments.setAdapter(commentAdapter);
        
        // Setup click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnSendComment.setOnClickListener(v -> sendComment());
        binding.btnLike.setOnClickListener(v -> toggleLike());
        binding.btnShare.setOnClickListener(v -> sharePost());
        
        // Enable/disable comment input based on login status
        if (currentUserId == -1) {
            binding.etComment.setEnabled(false);
            binding.btnSendComment.setEnabled(false);
            binding.etComment.setHint("Sign in to comment");
        }
    }    private void loadPostDetails() {
        if (postId == -1) {
            Toast.makeText(requireContext(), "Invalid post", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Load post from local database
        repository.getPostById(postId).observe(getViewLifecycleOwner(), post -> {
            binding.progressBar.setVisibility(View.GONE);
            if (post != null) {
                currentPost = post;
                displayPostDetails(post);
            } else {
                Toast.makeText(requireContext(), "Post not found", Toast.LENGTH_SHORT).show();
                navigateBack();
            }
        });
    }    private void displayPostDetails(PostEntity post) {
        // User info
        binding.tvUserName.setText(post.getUsername());
          // Profile image
        if (post.getUserAvatar() != null && !post.getUserAvatar().isEmpty()) {
            // Load from file path
            File imageFile = new File(post.getUserAvatar());
            if (imageFile.exists()) {
                Glide.with(this)
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
            Glide.with(this)
                    .load(post.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivPostImage);
        } else {
            binding.ivPostImage.setVisibility(View.GONE);
        }

        // Timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' HH:mm", Locale.getDefault());
        binding.tvTimestamp.setText(sdf.format(new Date(post.getTimestamp())));

        // Location
        if (post.getLocation() != null && !post.getLocation().isEmpty()) {
            binding.tvLocation.setVisibility(View.VISIBLE);
            binding.tvLocation.setText("📍 " + post.getLocation());
        } else {
            binding.tvLocation.setVisibility(View.GONE);
        }

        // Category - since PostEntity doesn't have category, we'll hide it
        binding.tvCategory.setVisibility(View.GONE);

        // Interaction counts
        binding.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        binding.tvCommentCount.setText(String.valueOf(post.getCommentCount()));

        // Check if current user liked this post
        binding.btnLike.setImageResource(post.isLiked() ? 
            android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
    }    private void loadComments() {
        if (postId == -1) return;

        repository.getCommentsByPostId(postId).observe(getViewLifecycleOwner(), comments -> {
            commentList.clear();
            if (comments != null) {
                commentList.addAll(comments);
            }
            commentAdapter.notifyDataSetChanged();
            
            if (commentList.isEmpty()) {
                binding.tvNoComments.setVisibility(View.VISIBLE);
            } else {
                binding.tvNoComments.setVisibility(View.GONE);
            }
        });
    }    private void sendComment() {
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Please sign in to comment", Toast.LENGTH_SHORT).show();
            return;
        }

        String commentText = binding.etComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            binding.etComment.setError("Comment cannot be empty");
            return;
        }

        binding.btnSendComment.setEnabled(false);
        
        // Get current user data
        executor.execute(() -> {
            UserEntity user = database.userDao().getUserByIdSync(currentUserId);
            if (user == null) {
                requireActivity().runOnUiThread(() -> {
                    binding.btnSendComment.setEnabled(true);
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            CommentEntity comment = new CommentEntity();
            comment.setPostId(postId);
            comment.setUserId(currentUserId);
            comment.setUsername(user.getUsername() != null ? user.getUsername() : "Anonymous");
            comment.setContent(commentText);
            comment.setTimestamp(System.currentTimeMillis());
            comment.setUserAvatar(user.getProfileImagePath());

            repository.insertComment(comment, insertedComment -> {
                requireActivity().runOnUiThread(() -> {
                    binding.btnSendComment.setEnabled(true);
                    binding.etComment.setText("");
                    
                    // Add comment to local list
                    commentList.add(insertedComment);
                    commentAdapter.notifyItemInserted(commentList.size() - 1);
                    
                    // Update comment count
                    if (currentPost != null) {
                        currentPost.setCommentCount(currentPost.getCommentCount() + 1);
                        binding.tvCommentCount.setText(String.valueOf(currentPost.getCommentCount()));
                    }
                    
                    binding.tvNoComments.setVisibility(View.GONE);
                    
                    // Scroll to bottom to show new comment
                    binding.rvComments.scrollToPosition(commentList.size() - 1);
                });
            });
        });
    }    private void toggleLike() {
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Please sign in to like posts", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPost == null) return;

        // Toggle like state locally
        boolean newLikedState = !currentPost.isLiked();
        currentPost.setLiked(newLikedState);
        
        // Update like count
        if (newLikedState) {
            currentPost.setLikeCount(currentPost.getLikeCount() + 1);
            binding.btnLike.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            currentPost.setLikeCount(currentPost.getLikeCount() - 1);
            binding.btnLike.setImageResource(android.R.drawable.btn_star_big_off);
        }
        
        binding.tvLikeCount.setText(String.valueOf(currentPost.getLikeCount()));
        
        // Update in database
        repository.updatePostLike(postId, currentPost.getLikeCount(), newLikedState);
    }private void sharePost() {
        if (currentPost == null) return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        
        String shareText = "Check out this post from " + currentPost.getUsername() + ":\n\n" +
                          currentPost.getContent() + "\n\n" +
                          "Shared via GleanGo - Make Your World Clean! 🌱";
        
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "GleanGo Community Post");
        
        startActivity(Intent.createChooser(shareIntent, "Share post"));
    }

    private void navigateBack() {        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        
        // Shutdown executor service to prevent memory leaks
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
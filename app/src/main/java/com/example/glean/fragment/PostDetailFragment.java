package com.example.glean.fragment;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.glean.helper.FirebaseHelper;
import com.example.glean.model.CommentModel;
import com.example.glean.model.CommunityPostModel;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostDetailFragment extends Fragment implements CommentAdapter.OnCommentInteractionListener {

    private FragmentPostDetailBinding binding;
    private FirebaseHelper firebaseHelper;
    private CommentAdapter commentAdapter;
    private List<CommentModel> commentList;
    private String postId;
    private CommunityPostModel currentPost;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        commentList = new ArrayList<>();
        
        // Get post ID from arguments
        if (getArguments() != null) {
            postId = getArguments().getString("POST_ID");
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
    }

    private void setupUI() {
        // Setup RecyclerView for comments
        commentAdapter = new CommentAdapter(commentList, this);
        binding.rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvComments.setAdapter(commentAdapter);
        
        // Setup click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnSendComment.setOnClickListener(v -> sendComment());
        binding.btnLike.setOnClickListener(v -> toggleLike());
        binding.btnShare.setOnClickListener(v -> sharePost());
        
        // Enable/disable comment input based on login status
        if (!firebaseHelper.isUserLoggedIn()) {
            binding.etComment.setEnabled(false);
            binding.btnSendComment.setEnabled(false);
            binding.etComment.setHint("Sign in to comment");
        }
    }

    private void loadPostDetails() {
        if (postId == null) {
            Toast.makeText(requireContext(), "Invalid post", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Load post from Firestore
        firebaseHelper.getPost(postId,
                post -> {
                    binding.progressBar.setVisibility(View.GONE);
                    currentPost = post;
                    displayPostDetails(post);
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error loading post: " + error.getMessage(), 
                                   Toast.LENGTH_SHORT).show();
                    navigateBack();
                });
    }

    private void displayPostDetails(CommunityPostModel post) {
        // User info
        binding.tvUserName.setText(post.getUserName());
        
        // Profile image
        if (post.getUserProfileUrl() != null && !post.getUserProfileUrl().isEmpty()) {
            Glide.with(this)
                    .load(post.getUserProfileUrl())
                    .placeholder(R.drawable.profile_placeholder)
                    .circleCrop()
                    .into(binding.ivUserProfile);
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

        // Category
        if (post.getCategory() != null) {
            binding.tvCategory.setVisibility(View.VISIBLE);
            binding.tvCategory.setText(post.getCategory().toUpperCase());
        }

        // Interaction counts
        binding.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        binding.tvCommentCount.setText(String.valueOf(post.getCommentCount()));

        // Check if current user liked this post
        checkUserLikedPost();
    }

    private void loadComments() {
        if (postId == null) return;

        firebaseHelper.getComments(postId,
                comments -> {
                    commentList.clear();
                    commentList.addAll(comments);
                    commentAdapter.notifyDataSetChanged();
                    
                    if (comments.isEmpty()) {
                        binding.tvNoComments.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvNoComments.setVisibility(View.GONE);
                    }
                },
                error -> Toast.makeText(requireContext(), "Error loading comments: " + error.getMessage(), 
                                        Toast.LENGTH_SHORT).show());
    }

    private void sendComment() {
        if (!firebaseHelper.isUserLoggedIn()) {
            Toast.makeText(requireContext(), "Please sign in to comment", Toast.LENGTH_SHORT).show();
            return;
        }

        String commentText = binding.etComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            binding.etComment.setError("Comment cannot be empty");
            return;
        }

        FirebaseUser user = firebaseHelper.getCurrentUser();
        if (user == null) return;

        CommentModel comment = new CommentModel(postId, user.getUid(), 
                user.getDisplayName() != null ? user.getDisplayName() : "Anonymous", 
                commentText);
        comment.setUserProfileUrl(user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);

        binding.btnSendComment.setEnabled(false);
        
        firebaseHelper.addComment(comment,
                commentId -> {
                    binding.btnSendComment.setEnabled(true);
                    binding.etComment.setText("");
                    
                    // Add comment to local list
                    comment.setId(commentId);
                    commentList.add(comment);
                    commentAdapter.notifyItemInserted(commentList.size() - 1);
                    
                    // Update comment count
                    if (currentPost != null) {
                        currentPost.setCommentCount(currentPost.getCommentCount() + 1);
                        binding.tvCommentCount.setText(String.valueOf(currentPost.getCommentCount()));
                    }
                    
                    binding.tvNoComments.setVisibility(View.GONE);
                    
                    // Scroll to bottom to show new comment
                    binding.rvComments.scrollToPosition(commentList.size() - 1);
                },
                error -> {
                    binding.btnSendComment.setEnabled(true);
                    Toast.makeText(requireContext(), "Error posting comment: " + error.getMessage(), 
                                   Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleLike() {
        if (!firebaseHelper.isUserLoggedIn()) {
            Toast.makeText(requireContext(), "Please sign in to like posts", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPost == null) return;

        firebaseHelper.toggleLike(postId,
                isLiked -> {
                    if (isLiked) {
                        currentPost.setLikeCount(currentPost.getLikeCount() + 1);
                        binding.btnLike.setImageResource(android.R.drawable.btn_star_big_on); // Use built-in filled star
                    } else {
                        currentPost.setLikeCount(currentPost.getLikeCount() - 1);
                        binding.btnLike.setImageResource(android.R.drawable.btn_star_big_off); // Use built-in outline star
                    }
                    binding.tvLikeCount.setText(String.valueOf(currentPost.getLikeCount()));
                },
                error -> Toast.makeText(requireContext(), "Error: " + error.getMessage(), 
                                    Toast.LENGTH_SHORT).show());
    }

    private void checkUserLikedPost() {
        if (!firebaseHelper.isUserLoggedIn() || currentPost == null) {
            binding.btnLike.setImageResource(android.R.drawable.btn_star_big_off);
            return;
        }

        // Check if user liked this post
        firebaseHelper.isPostLikedByUser(postId,
                isLiked -> {
                    if (isLiked) {
                        binding.btnLike.setImageResource(android.R.drawable.btn_star_big_on);
                    } else {
                        binding.btnLike.setImageResource(android.R.drawable.btn_star_big_off);
                    }
                },
                error -> binding.btnLike.setImageResource(android.R.drawable.btn_star_big_off));
    }

    private void sharePost() {
        if (currentPost == null) return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        
        String shareText = "Check out this post from " + currentPost.getUserName() + ":\n\n" +
                          currentPost.getContent() + "\n\n" +
                          "Shared via GleanGo - Make Your World Clean! 🌱";
        
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "GleanGo Community Post");
        
        startActivity(Intent.createChooser(shareIntent, "Share post"));
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onCommentLike(CommentModel comment, int position) {
        if (!firebaseHelper.isUserLoggedIn()) {
            Toast.makeText(requireContext(), "Please sign in to like comments", Toast.LENGTH_SHORT).show();
            return;
        }

        // Implement comment like functionality if needed
        Toast.makeText(requireContext(), "Comment like feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCommentReply(CommentModel comment, int position) {
        // Pre-fill comment field with reply format
        binding.etComment.setText("@" + comment.getUserName() + " ");
        binding.etComment.setSelection(binding.etComment.getText().length());
        binding.etComment.requestFocus();
    }

    @Override
    public void onUserClick(CommentModel comment) {
        // Navigate to user profile or show user info
        Toast.makeText(requireContext(), "User: " + comment.getUserName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
package com.example.glean.fragment.community;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.glean.R;
import com.example.glean.activity.PostDetailActivity;
import com.example.glean.adapter.PostAdapter;
import com.example.glean.databinding.FragmentSosialBinding;
import com.example.glean.model.PostEntity;
import com.example.glean.repository.CommunityRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SosialFragment extends Fragment implements PostAdapter.OnPostClickListener {
    
    private FragmentSosialBinding binding;
    private PostAdapter postAdapter;
    private List<PostEntity> posts = new ArrayList<>();
    private CommunityRepository repository;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSosialBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
      @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new CommunityRepository(requireContext());
        setupRecyclerView();
        setupSwipeRefresh();
        setupFab();
        loadPosts();
    }
    
    private void setupRecyclerView() {
        postAdapter = new PostAdapter(posts, this);
        binding.recyclerViewPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewPosts.setAdapter(postAdapter);
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadPosts);
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary_color);
    }
    
    private void setupFab() {
        binding.fabCreatePost.setOnClickListener(v -> {
            // Navigate to create post screen
            // This would typically open CreatePostFragment or Activity
        });
    }    private void loadPosts() {
        binding.swipeRefreshLayout.setRefreshing(true);
        Log.d("SosialFragment", "Loading posts from database...");
        
        // Load posts from database
        repository.getAllPosts().observe(getViewLifecycleOwner(), new Observer<List<PostEntity>>() {            @Override
            public void onChanged(List<PostEntity> postEntities) {
                Log.d("SosialFragment", "Posts loaded from database: " + (postEntities != null ? postEntities.size() : 0));
                posts.clear();
                if (postEntities != null && !postEntities.isEmpty()) {
                    posts.addAll(postEntities);
                    Log.d("SosialFragment", "Using database posts");
                } else {
                    Log.d("SosialFragment", "No posts found in database, seeder should have run automatically");
                }
                
                postAdapter.notifyDataSetChanged();
                binding.swipeRefreshLayout.setRefreshing(false);
                
                // Show/hide empty state
                if (posts.isEmpty()) {
                    binding.emptyStateLayout.setVisibility(View.VISIBLE);
                    binding.recyclerViewPosts.setVisibility(View.GONE);
                } else {
                    binding.emptyStateLayout.setVisibility(View.GONE);
                    binding.recyclerViewPosts.setVisibility(View.VISIBLE);
                }
            }        });
    }
    
    @Override
    public void onPostClick(PostEntity post) {
        // Navigate to post detail
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        intent.putExtra("post_id", post.getId());
        intent.putExtra("user_id", post.getUserId());
        intent.putExtra("username", post.getUsername());
        intent.putExtra("content", post.getContent());
        intent.putExtra("like_count", post.getLikeCount());
        intent.putExtra("comment_count", post.getCommentCount());
        intent.putExtra("timestamp", post.getTimestamp());
        intent.putExtra("is_liked", post.isLiked());
        startActivityForResult(intent, 1001);
    }
    
    @Override
    public void onLikeClick(PostEntity post) {
        // Toggle like state
        post.setLiked(!post.isLiked());
        if (post.isLiked()) {
            post.setLikeCount(post.getLikeCount() + 1);
        } else {
            post.setLikeCount(post.getLikeCount() - 1);
        }
        
        // Update in database
        repository.updatePostLike(post.getId(), post.getLikeCount(), post.isLiked());
        postAdapter.notifyDataSetChanged();
    }    @Override
    public void onCommentClick(PostEntity post) {
        // Navigate to post detail with focus on comments
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        intent.putExtra("post_id", post.getId());
        intent.putExtra("user_id", post.getUserId());
        intent.putExtra("username", post.getUsername());
        intent.putExtra("content", post.getContent());
        intent.putExtra("like_count", post.getLikeCount());
        intent.putExtra("comment_count", post.getCommentCount());
        intent.putExtra("timestamp", post.getTimestamp());
        intent.putExtra("is_liked", post.isLiked());
        intent.putExtra("focus_comment", true); // Focus on comment input
        startActivityForResult(intent, 1001);
    }
    
    @Override
    public void onShareClick(PostEntity post) {
        // Handle share action
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, post.getContent());
        startActivity(Intent.createChooser(shareIntent, "Bagikan Post"));
    }
      @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK && data != null) {
            // Update post data from detail activity
            int postId = data.getIntExtra("post_id", 0);
            int likeCount = data.getIntExtra("like_count", 0);
            boolean isLiked = data.getBooleanExtra("is_liked", false);
            int commentCount = data.getIntExtra("comment_count", 0);
            
            // Find and update the post in the list
            for (PostEntity post : posts) {
                if (post.getId() == postId) {
                    post.setLikeCount(likeCount);
                    post.setLiked(isLiked);
                    post.setCommentCount(commentCount);
                    break;
                }
            }
            postAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

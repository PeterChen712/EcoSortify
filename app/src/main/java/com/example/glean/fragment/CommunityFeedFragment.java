package com.example.glean.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.glean.R;
import com.example.glean.adapter.CommunityPostAdapter;
import com.example.glean.databinding.FragmentCommunityFeedBinding;
import com.example.glean.model.CommunityPostModel;
import com.example.glean.model.PostEntity;
import com.example.glean.repository.CommunityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityFeedFragment extends Fragment implements 
        CommunityPostAdapter.OnPostInteractionListener,
        SwipeRefreshLayout.OnRefreshListener {

    private FragmentCommunityFeedBinding binding;
    private CommunityPostAdapter adapter;
    private List<CommunityPostModel> postList;
    private CommunityRepository communityRepository;
    private ExecutorService executor;
    private int currentUserId;    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postList = new ArrayList<>();
        communityRepository = new CommunityRepository(requireContext());
        executor = Executors.newFixedThreadPool(4);
        
        // Get current user ID from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", requireContext().MODE_PRIVATE);
        currentUserId = prefs.getInt("current_user_id", -1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityFeedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupUI();
        checkUserStatus();
    }

    private void setupUI() {
        // Setup RecyclerView
        adapter = new CommunityPostAdapter(postList, this);
        binding.rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPosts.setAdapter(adapter);
        
        // Setup SwipeRefresh
        binding.swipeRefresh.setOnRefreshListener(this);
        binding.swipeRefresh.setColorSchemeResources(
                R.color.primary,
                R.color.primary_dark,
                R.color.accent
        );
          // Setup FAB
        binding.fabCreatePost.setOnClickListener(v -> {
            if (currentUserId != -1) {
                navigateToCreatePost();
            } else {
                showLoginMessage();
            }
        });
    }    private void checkUserStatus() {
        if (currentUserId != -1) {
            showCommunityContent();
            loadLocalPosts();
        } else {
            showOfflineContent();
        }    }

    private void showCommunityContent() {
        binding.layoutLogin.setVisibility(View.GONE);
        binding.layoutCommunity.setVisibility(View.VISIBLE);
    }    private void showOfflineContent() {
        binding.layoutCommunity.setVisibility(View.VISIBLE);
        binding.fabCreatePost.setVisibility(View.GONE);
        
        // Show message about offline mode
        binding.tvOfflineMessage.setVisibility(View.VISIBLE);
        binding.tvOfflineMessage.setText("Community features require user account");
        
        // Show empty state
        showEmptyState();
    }

    private void showLoginMessage() {
        Toast.makeText(requireContext(), "Please create a user profile to post content", Toast.LENGTH_SHORT).show();
    }

    private void loadLocalPosts() {
        communityRepository.getAllPosts().observe(getViewLifecycleOwner(), new Observer<List<PostEntity>>() {
            @Override
            public void onChanged(List<PostEntity> postEntities) {
                if (binding != null) {
                    binding.swipeRefresh.setRefreshing(false);
                    
                    if (postEntities != null && !postEntities.isEmpty()) {
                        postList.clear();
                        for (PostEntity postEntity : postEntities) {
                            postList.add(convertToModel(postEntity));
                        }
                        adapter.notifyDataSetChanged();
                        binding.tvEmptyState.setVisibility(View.GONE);
                    } else {
                        showEmptyState();
                    }
                }
            }
        });
    }

    private CommunityPostModel convertToModel(PostEntity entity) {
        CommunityPostModel model = new CommunityPostModel();
        model.setId(String.valueOf(entity.getId()));
        model.setUserId(String.valueOf(entity.getUserId()));
        model.setUserName(entity.getUsername());
        model.setUserProfileUrl(entity.getUserAvatar());
        model.setContent(entity.getContent());
        model.setImageUrl(entity.getImageUrl());
        model.setLocation(entity.getLocation());
        model.setTimestamp(entity.getTimestamp());
        model.setLikeCount(entity.getLikeCount());
        model.setCommentCount(entity.getCommentCount());
        model.setCategory("plogging"); // Default category
        model.setPublic(true);
        return model;
    }

    private void showEmptyState() {
        binding.tvEmptyState.setVisibility(View.VISIBLE);
        postList.clear();
        adapter.notifyDataSetChanged();
    }

    private void navigateToCreatePost() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_communityFeedFragment_to_createPostFragment);
    }    @Override
    public void onRefresh() {
        if (currentUserId != -1) {
            loadLocalPosts();
        } else {
            binding.swipeRefresh.setRefreshing(false);
            showOfflineContent();
        }
    }

    @Override
    public void onLikeClick(CommunityPostModel post, int position) {
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Please sign in to like posts", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                int postId = Integer.parseInt(post.getId());
                int newLikeCount = post.getLikeCount() + 1;
                communityRepository.updatePostLike(postId, newLikeCount, true);
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        post.setLikeCount(newLikeCount);
                        adapter.notifyItemChanged(position);
                    });
                }
            } catch (NumberFormatException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "Error updating like", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }    @Override
    public void onCommentClick(CommunityPostModel post, int position) {
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putString("POST_ID", post.getId());
        navController.navigate(R.id.action_communityFeedFragment_to_postDetailFragment, args);
    }

    @Override
    public void onShareClick(CommunityPostModel post, int position) {
        // Implement share functionality
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "Check out this plogging achievement: " + post.getContent());
        startActivity(Intent.createChooser(shareIntent, "Share post"));
    }

    @Override
    public void onUserClick(CommunityPostModel post) {
        // Navigate to user profile
        Toast.makeText(requireContext(), "User profile: " + post.getUserName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        binding = null;
    }
}
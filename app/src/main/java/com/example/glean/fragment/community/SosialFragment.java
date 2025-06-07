package com.example.glean.fragment.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.glean.R;
import com.example.glean.activity.PostDetailActivity;
import com.example.glean.adapter.PostAdapter;
import com.example.glean.databinding.FragmentSosialBinding;
import com.example.glean.model.PostEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SosialFragment extends Fragment implements PostAdapter.OnPostClickListener {
    
    private FragmentSosialBinding binding;
    private PostAdapter postAdapter;
    private List<PostEntity> posts = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSosialBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
    }
    
    private void loadPosts() {
        binding.swipeRefreshLayout.setRefreshing(true);
        
        // Simulate loading posts - replace with actual API call
        // For now, add some sample posts
        posts.clear();
        
        // Sample posts
        PostEntity post1 = new PostEntity();
        post1.setId("1");
        post1.setUsername("EcoWarrior");
        post1.setContent("Hari ini berhasil mengumpulkan 5kg sampah plastik di pantai! 🌊♻️ #PloggingChallenge");
        post1.setImageUrl("sample_image_1");
        post1.setLikeCount(24);
        post1.setCommentCount(8);
        post1.setTimestamp(System.currentTimeMillis() - 3600000); // 1 hour ago
        posts.add(post1);
        
        PostEntity post2 = new PostEntity();
        post2.setId("2");
        post2.setUsername("GreenHero");
        post2.setContent("Plogging pagi ini di taman kota. Setiap langkah untuk bumi yang lebih bersih! 🏃‍♂️🌱");
        post2.setImageUrl("sample_image_2");
        post2.setLikeCount(18);
        post2.setCommentCount(5);
        post2.setTimestamp(System.currentTimeMillis() - 7200000); // 2 hours ago
        posts.add(post2);
        
        PostEntity post3 = new PostEntity();
        post3.setId("3");
        post3.setUsername("CleanTeam");
        post3.setContent("Tim kami berhasil membersihkan area seluas 2 hektar! Terima kasih untuk semua volunteer 💪");
        post3.setImageUrl("sample_image_3");
        post3.setLikeCount(45);
        post3.setCommentCount(12);
        post3.setTimestamp(System.currentTimeMillis() - 14400000); // 4 hours ago
        posts.add(post3);
        
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
    }
      @Override
    public void onPostClick(PostEntity post) {
        // Navigate to post detail
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        intent.putExtra("post_id", post.getId());
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
        postAdapter.notifyDataSetChanged();
    }
      @Override
    public void onCommentClick(PostEntity post) {
        // Navigate to post detail with focus on comments
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        intent.putExtra("post_id", post.getId());
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
            String postId = data.getStringExtra("post_id");
            int likeCount = data.getIntExtra("like_count", 0);
            boolean isLiked = data.getBooleanExtra("is_liked", false);
            int commentCount = data.getIntExtra("comment_count", 0);
            
            // Find and update the post in the list
            for (PostEntity post : posts) {
                if (post.getId().equals(postId)) {
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

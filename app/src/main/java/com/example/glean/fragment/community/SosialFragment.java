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
        // Implementation depends on your navigation setup
    }
    
    @Override
    public void onLikeClick(PostEntity post) {
        // Handle like action
        post.setLikeCount(post.getLikeCount() + 1);
        postAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onCommentClick(PostEntity post) {
        // Navigate to comments
        // Implementation depends on your navigation setup
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

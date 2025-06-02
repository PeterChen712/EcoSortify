package com.example.glean.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.glean.R;
import com.example.glean.adapter.CommunityPostAdapter;
import com.example.glean.databinding.FragmentCommunityFeedBinding;
import com.example.glean.helper.FirebaseHelper;
import com.example.glean.model.CommunityPostModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;

public class CommunityFeedFragment extends Fragment implements 
        CommunityPostAdapter.OnPostInteractionListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final int RC_SIGN_IN = 9001;

    private FragmentCommunityFeedBinding binding;
    private FirebaseHelper firebaseHelper;
    private CommunityPostAdapter adapter;
    private List<CommunityPostModel> postList;
    private GoogleSignInClient googleSignInClient;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        postList = new ArrayList<>();
        
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
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
            if (firebaseHelper.isUserLoggedIn()) {
                navigateToCreatePost();
            } else {
                showLoginPrompt();
            }
        });
    }

    private void checkUserStatus() {
        if (firebaseHelper.isUserLoggedIn()) {
            showCommunityContent();
            loadCommunityPosts();
        } else {
            showLoginPrompt();
        }
    }

    private void showLoginPrompt() {
        binding.layoutLogin.setVisibility(View.VISIBLE);
        binding.layoutCommunity.setVisibility(View.GONE);
        
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.btnOfflineMode.setOnClickListener(v -> {
            // Show offline content
            showOfflineContent();
        });
    }

    private void showCommunityContent() {
        binding.layoutLogin.setVisibility(View.GONE);
        binding.layoutCommunity.setVisibility(View.VISIBLE);
    }

    private void showOfflineContent() {
        binding.layoutLogin.setVisibility(View.GONE);
        binding.layoutCommunity.setVisibility(View.VISIBLE);
        binding.fabCreatePost.setVisibility(View.GONE);
        
        // Show message about offline mode
        binding.tvOfflineMessage.setVisibility(View.VISIBLE);
        binding.tvOfflineMessage.setText("Offline Mode: Community features not available");
        
        // Load cached posts if any
        loadCachedPosts();
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(requireContext(), "Google sign in failed: " + e.getMessage(), 
                               Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Signed in successfully!", Toast.LENGTH_SHORT).show();
                        checkUserStatus();
                    } else {
                        Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadCommunityPosts() {
        if (!firebaseHelper.isOnline()) {
            loadCachedPosts();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        
        firebaseHelper.getCommunityPosts(20, 
                posts -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    
                    postList.clear();
                    postList.addAll(posts);
                    adapter.notifyDataSetChanged();
                    
                    if (posts.isEmpty()) {
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvEmptyState.setVisibility(View.GONE);
                    }
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), "Error loading posts: " + error.getMessage(), 
                                   Toast.LENGTH_SHORT).show();
                    loadCachedPosts();
                });
    }

    private void loadCachedPosts() {
        // Load cached posts from local database
        // This would be implemented based on your local caching strategy
        binding.tvEmptyState.setVisibility(View.VISIBLE);
        
        // Find the TextView inside the LinearLayout
        TextView emptyStateText = binding.tvEmptyState.findViewById(R.id.tv_empty_state_text);
        if (emptyStateText != null) {
            emptyStateText.setText("No cached posts available");
        }
    }

    private void navigateToCreatePost() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_communityFeedFragment_to_createPostFragment);
    }

    @Override
    public void onRefresh() {
        loadCommunityPosts();
    }

    @Override
    public void onLikeClick(CommunityPostModel post, int position) {
        if (!firebaseHelper.isUserLoggedIn()) {
            Toast.makeText(requireContext(), "Please sign in to like posts", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseHelper.toggleLike(post.getId(),
                isLiked -> {
                    // Update local post data
                    if (isLiked) {
                        post.setLikeCount(post.getLikeCount() + 1);
                    } else {
                        post.setLikeCount(post.getLikeCount() - 1);
                    }
                    adapter.notifyItemChanged(position);
                },
                error -> Toast.makeText(requireContext(), "Error: " + error.getMessage(), 
                                        Toast.LENGTH_SHORT).show());
    }

    @Override
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
        binding = null;
    }
}
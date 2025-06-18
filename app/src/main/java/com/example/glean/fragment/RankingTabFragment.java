package com.example.glean.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.adapter.RankingAdapter;
import com.example.glean.auth.FirebaseAuthManager;
import com.example.glean.databinding.FragmentRankingTabBinding;
import com.example.glean.model.RankingUser;
import com.example.glean.util.NetworkUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class RankingTabFragment extends Fragment {
    
    private static final String TAG = "RankingTabFragment";
    private static final String ARG_IS_POINTS_RANKING = "is_points_ranking";
    
    private FragmentRankingTabBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuthManager authManager;
    private RankingAdapter adapter;
    private List<RankingUser> rankingList = new ArrayList<>();
    private boolean isPointsRanking;
    private String currentUserId;
    
    public static RankingTabFragment newInstance(boolean isPointsRanking) {
        RankingTabFragment fragment = new RankingTabFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_POINTS_RANKING, isPointsRanking);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isPointsRanking = getArguments().getBoolean(ARG_IS_POINTS_RANKING, true);
        }
        
        firestore = FirebaseFirestore.getInstance();
        authManager = FirebaseAuthManager.getInstance(requireContext());
        
        // Get current user ID
        SharedPreferences prefs = requireContext().getSharedPreferences("USER_PREFS", 0);
        currentUserId = prefs.getString("FIREBASE_USER_ID", "");
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRankingTabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
      @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRecyclerView();
        
        // Check network connectivity before loading data
        if (!NetworkUtil.isNetworkAvailable(requireContext())) {
            showNetworkError();
            return;
        }
        
        // Check authentication before loading data
        if (!authManager.isLoggedIn()) {
            showAuthenticationError();
            return;
        }
        
        loadRankingData();
    }
    
    private void showNetworkError() {
        showLoading(false);
        // Show network error message in UI
        binding.recyclerViewRanking.setVisibility(View.GONE);
        // You might want to add an error view to the layout
        Toast.makeText(requireContext(), "Fitur ranking membutuhkan koneksi internet.", Toast.LENGTH_LONG).show();
    }
    
    private void showAuthenticationError() {
        showLoading(false);
        binding.recyclerViewRanking.setVisibility(View.GONE);
        Toast.makeText(requireContext(), "Silakan login untuk melihat ranking.", Toast.LENGTH_LONG).show();
    }
    
    private void setupRecyclerView() {
        adapter = new RankingAdapter(requireContext(), rankingList, isPointsRanking, currentUserId);
        binding.recyclerViewRanking.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewRanking.setAdapter(adapter);
    }
    
    private void loadRankingData() {
        showLoading(true);
        
        // Determine which field to order by
        String orderByField = isPointsRanking ? "totalPoints" : "totalDistance";
        
        firestore.collection("users")
                .orderBy(orderByField, Query.Direction.DESCENDING)
                .limit(100) // Top 100 users
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    rankingList.clear();
                    
                    int position = 1;
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        try {
                            RankingUser user = createRankingUserFromDocument(document, position);
                            if (user != null) {
                                rankingList.add(user);
                                position++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing user document: " + document.getId(), e);
                        }
                    }
                    
                    updateUI();
                    loadCurrentUserRanking();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading ranking data", e);
                    showError("Gagal memuat data ranking");
                });
    }
    
    private RankingUser createRankingUserFromDocument(DocumentSnapshot document, int position) {
        try {
            String userId = document.getId();
            String username = document.getString("username");
            String profileImageUrl = document.getString("profileImageUrl");
            
            // Handle different data types for points and distance
            int totalPoints = 0;
            Object pointsObj = document.get("totalPoints");
            if (pointsObj instanceof Number) {
                totalPoints = ((Number) pointsObj).intValue();
            }
            
            double totalDistance = 0.0;
            Object distanceObj = document.get("totalDistance");
            if (distanceObj instanceof Number) {
                totalDistance = ((Number) distanceObj).doubleValue();
            }
            
            int trashCount = 0;
            Object trashObj = document.get("totalTrashCollected");
            if (trashObj instanceof Number) {
                trashCount = ((Number) trashObj).intValue();
            }
            
            int badgeCount = 0;
            Object badgeObj = document.get("badgeCount");
            if (badgeObj instanceof Number) {
                badgeCount = ((Number) badgeObj).intValue();
            }
            
            RankingUser user = new RankingUser(userId, username, profileImageUrl, 
                    totalPoints, totalDistance, trashCount, badgeCount);
            user.setPosition(position);
            
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error creating RankingUser from document", e);
            return null;
        }
    }
    
    private void loadCurrentUserRanking() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            // Hide current user card if not logged in
            binding.myRankingCard.setVisibility(View.GONE);
            return;
        }
        
        // Find current user in ranking list
        RankingUser currentUser = null;
        int userPosition = -1;
        
        for (int i = 0; i < rankingList.size(); i++) {
            if (rankingList.get(i).getUserId().equals(currentUserId)) {
                currentUser = rankingList.get(i);
                userPosition = i + 1;
                break;
            }
        }
        
        if (currentUser != null) {
            updateCurrentUserCard(currentUser, userPosition);
        } else {
            // User not in top 100, get their actual ranking
            loadUserActualRanking();
        }
    }
    
    private void loadUserActualRanking() {
        String orderByField = isPointsRanking ? "totalPoints" : "totalDistance";
        
        firestore.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        RankingUser currentUser = createRankingUserFromDocument(userDoc, 0);
                        if (currentUser != null) {
                            // Count users with higher score to get actual position
                            Object userScore = userDoc.get(orderByField);
                            double userScoreValue = userScore instanceof Number ? ((Number) userScore).doubleValue() : 0;
                            
                            firestore.collection("users")
                                    .whereGreaterThan(orderByField, userScoreValue)
                                    .get()
                                    .addOnSuccessListener(higherScoreQuery -> {
                                        int userPosition = higherScoreQuery.size() + 1;
                                        updateCurrentUserCard(currentUser, userPosition);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error getting user position", e);
                                        updateCurrentUserCard(currentUser, -1);
                                    });
                        }
                    } else {
                        binding.myRankingCard.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading current user data", e);
                    binding.myRankingCard.setVisibility(View.GONE);
                });
    }
    
    private void updateCurrentUserCard(RankingUser user, int position) {
        binding.myRankingCard.setVisibility(View.VISIBLE);
        
        if (position > 0) {
            binding.tvMyPosition.setText("#" + position);
        } else {
            binding.tvMyPosition.setText("#--");
        }
        
        binding.tvMyUsername.setText(user.getUsername() != null ? user.getUsername() : "You");
        
        if (isPointsRanking) {
            binding.tvMyScore.setText(user.getTotalPoints() + " poin");
        } else {
            binding.tvMyScore.setText(String.format("%.1f km", user.getTotalDistance() / 1000.0));
        }
        
        binding.tvMyStats.setText(user.getFormattedStats());
        
        // Load profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_user_avatar)
                    .error(R.drawable.ic_user_avatar)
                    .into(binding.ivMyProfileImage);
        } else {
            binding.ivMyProfileImage.setImageResource(R.drawable.ic_user_avatar);
        }
    }
    
    private void updateUI() {
        showLoading(false);
        
        if (rankingList.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
            adapter.updateData(rankingList);
        }
    }
    
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewRanking.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);
    }
    
    private void showEmpty(boolean show) {
        binding.layoutEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewRanking.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
    }
    
    private void showError(String message) {
        showLoading(false);
        showEmpty(true);
        // You can customize error display here
        Log.e(TAG, message);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

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
import com.example.glean.util.AvatarManager;
import com.example.glean.util.NetworkUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.glean.service.FirebaseDataManager;

import java.util.ArrayList;
import java.util.List;

public class RankingTabFragment extends Fragment {
    
    private static final String TAG = "RankingTabFragment";
    private static final String ARG_IS_POINTS_RANKING = "is_points_ranking";
      private FragmentRankingTabBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuthManager authManager;
    private FirebaseDataManager dataManager;
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
        dataManager = FirebaseDataManager.getInstance(requireContext());
        
        // Get current user ID - prioritize Firebase UID for Firebase users
        if (authManager.isLoggedIn() && authManager.getCurrentUserId() != null) {
            currentUserId = authManager.getCurrentUserId(); // Firebase UID
            Log.d(TAG, "Using Firebase UID for current user: " + currentUserId);
        } else {
            // Fallback to SharedPreferences for local users
            SharedPreferences prefs = requireContext().getSharedPreferences("USER_PREFS", 0);
            currentUserId = prefs.getString("FIREBASE_USER_ID", "");
            Log.d(TAG, "Using SharedPreferences user ID: " + currentUserId);
        }
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
        
        loadRankingDataWithFirebase();
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
          // Load profile image using activeAvatar (local assets only)
        String activeAvatar = user.getActiveAvatar();
        if (activeAvatar != null && !activeAvatar.trim().isEmpty()) {
            // Use AvatarManager to load local avatar
            AvatarManager.loadAvatarIntoImageView(requireContext(), binding.ivMyProfileImage, activeAvatar);
        } else {
            // Fallback to default avatar when activeAvatar is missing
            binding.ivMyProfileImage.setImageResource(R.drawable.avatar_default);
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
      private void loadRankingDataWithFirebase() {
        showLoading(true);
        
        Log.d(TAG, "🏆 Starting Firebase ranking data load for " + (isPointsRanking ? "points" : "distance") + " ranking");        
        // Subscribe to real-time ranking updates
        dataManager.subscribeToRanking(new FirebaseDataManager.RankingDataCallback() {
            @Override
            public void onRankingLoaded(List<RankingUser> ranking) {
                requireActivity().runOnUiThread(() -> {
                    Log.d(TAG, "🏆 Received ranking data: " + ranking.size() + " users");
                      // Convert Firebase ranking data to local ranking data
                    List<RankingUser> localRanking = convertFirebaseRankingToLocal(ranking);
                    
                    // Sort based on current tab
                    if (isPointsRanking) {
                        localRanking.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));
                        Log.d(TAG, "🏆 Sorted by points (descending)");
                    } else {
                        localRanking.sort((a, b) -> Double.compare(b.getTotalDistance(), a.getTotalDistance()));
                        Log.d(TAG, "🏆 Sorted by distance (descending)");
                    }
                    
                    // Update position numbers after sorting
                    for (int i = 0; i < localRanking.size(); i++) {
                        localRanking.get(i).setPosition(i + 1);
                    }
                    
                    rankingList.clear();
                    rankingList.addAll(localRanking);
                    adapter.notifyDataSetChanged();
                    
                    showLoading(false);
                    
                    if (rankingList.isEmpty()) {
                        showEmpty(true);
                        Log.w(TAG, "⚠️ No ranking data to display");
                    } else {
                        showEmpty(false);
                        Log.d(TAG, "✅ Ranking UI updated with " + rankingList.size() + " users");
                    }
                    
                    // Update current user position
                    updateCurrentUserPosition();
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "❌ Firebase ranking error: " + error);
                    showLoading(false);
                    showEmpty(true);
                    Toast.makeText(requireContext(), 
                        "Gagal memuat data ranking: " + error, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // Skip sync for Firebase users to prevent data overwrite
        if (authManager.getCurrentUserId() == null) {
            Log.d(TAG, "⚠️ No current user ID - skipping data sync");
            return;
        }        
        Log.d(TAG, "🔄 Current user is Firebase user - skipping local data sync to prevent overwriting Firebase data");
    }
    
    private List<RankingUser> convertFirebaseRankingToLocal(List<RankingUser> firebaseRanking) {
        List<RankingUser> localRanking = new ArrayList<>();
        
        Log.d(TAG, "🔄 Converting " + firebaseRanking.size() + " Firebase users to local ranking format");
        
        for (RankingUser fbUser : firebaseRanking) {
            try {
                // Enhanced logging to debug the conversion issue
                Log.d(TAG, "🔄 Processing Firebase user: " + fbUser.getUserId());
                Log.d(TAG, "   - Username: " + fbUser.getUsername());
                Log.d(TAG, "   - FullName: " + fbUser.getFullName());
                Log.d(TAG, "   - Points: " + fbUser.getTotalPoints());
                Log.d(TAG, "   - Distance: " + fbUser.getTotalDistance());
                Log.d(TAG, "   - Trash: " + fbUser.getTotalTrashCollected());                Log.d(TAG, "   - PhotoURL: " + fbUser.getPhotoURL());
                Log.d(TAG, "   - ActiveAvatar: " + fbUser.getActiveAvatar());
                
                // Use the best display name - prioritize fullName (nama) over username
                String displayName = fbUser.getFullName();
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = fbUser.getUsername();
                }
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = "User " + fbUser.getUserId().substring(0, Math.min(8, fbUser.getUserId().length()));
                }
                
                // Get photo URL and activeAvatar
                String photoURL = fbUser.getPhotoURL();
                if (photoURL == null) photoURL = "";
                
                String activeAvatar = fbUser.getActiveAvatar();
                if (activeAvatar == null || activeAvatar.trim().isEmpty()) {
                    activeAvatar = "default"; // Default avatar if not set
                }
                  RankingUser localUser = new RankingUser(
                    fbUser.getUserId(),
                    displayName,  // Use the best name (nama field preferred)
                    photoURL,     // Use photoURL from Firebase
                    fbUser.getTotalPoints(),
                    fbUser.getTotalDistance(),
                    fbUser.getTotalTrashCollected(),
                    0 // badge count - will be simplified in display
                );
                
                // Set activeAvatar for local assets
                localUser.setActiveAvatar(activeAvatar);
                
                localRanking.add(localUser);
                
                Log.v(TAG, "✅ Converted user: " + displayName + " (Points: " + fbUser.getTotalPoints() + ", Distance: " + fbUser.getTotalDistance() + ", Photo: " + photoURL + ")");
            } catch (Exception e) {
                Log.w(TAG, "⚠️ Error converting Firebase user to local format: " + fbUser.getUserId(), e);
            }
        }
        
        Log.d(TAG, "✅ Conversion complete: " + localRanking.size() + " users converted successfully");
        return localRanking;
    }
      private void updateCurrentUserPosition() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.w(TAG, "⚠️ Cannot update current user position - user ID is null or empty");
            binding.myRankingCard.setVisibility(View.GONE);
            return;
        }
        
        Log.d(TAG, "🔍 Looking for current user in ranking list: " + currentUserId);
        
        // Find current user position in ranking
        int currentUserPosition = -1;
        RankingUser currentRankingUser = null;
        
        for (int i = 0; i < rankingList.size(); i++) {
            RankingUser user = rankingList.get(i);
            if (currentUserId.equals(user.getUserId())) {
                currentUserPosition = i + 1; // Position is 1-based
                currentRankingUser = user;
                Log.d(TAG, "✅ Found current user at position " + currentUserPosition);
                break;
            }        }
        
        // Update UI with current user position
        if (currentUserPosition > 0 && currentRankingUser != null) {
            binding.myRankingCard.setVisibility(View.VISIBLE);
            binding.tvMyPosition.setText("#" + currentUserPosition);
            
            // Use the same display name logic as in the ranking list (prioritize nama field)
            String displayName = currentRankingUser.getUsername();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "You";
            }
            binding.tvMyUsername.setText(displayName);
            
            // Display score based on current tab
            if (isPointsRanking) {
                binding.tvMyScore.setText(currentRankingUser.getTotalPoints() + " poin");
            } else {
                binding.tvMyScore.setText(String.format("%.1f km", currentRankingUser.getTotalDistance() / 1000.0));
            }
            
            // Display simplified stats (only KM and points, no badges)
            binding.tvMyStats.setText(currentRankingUser.getFormattedStats());
              // Load profile image using activeAvatar (local assets only)
            String activeAvatar = currentRankingUser.getActiveAvatar();
            if (activeAvatar != null && !activeAvatar.trim().isEmpty()) {
                // Use AvatarManager to load local avatar
                AvatarManager.loadAvatarIntoImageView(requireContext(), binding.ivMyProfileImage, activeAvatar);
            } else {
                // Fallback to default avatar when activeAvatar is missing
                binding.ivMyProfileImage.setImageResource(R.drawable.avatar_default);
            }
            
            Log.d(TAG, "✅ Current user UI updated - Position: #" + currentUserPosition + 
                ", Points: " + currentRankingUser.getTotalPoints() + 
                ", Distance: " + currentRankingUser.getTotalDistance());
        } else {
            // User not found in top ranking - fetch their actual position from Firebase
            Log.w(TAG, "⚠️ Current user not found in top ranking list, will fetch actual position");
            binding.tvMyPosition.setText("#--");
            binding.tvMyUsername.setText("You");
            binding.tvMyScore.setText("-- " + (isPointsRanking ? "poin" : "km"));
            binding.myRankingCard.setVisibility(View.VISIBLE);
            
            // Try to fetch current user's actual stats from Firebase
            fetchCurrentUserActualRanking();
        }
    }
      private void fetchCurrentUserActualRanking() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "🔍 Fetching actual ranking for current user from Firebase: " + currentUserId);
        
        // Get user profile data first
        firestore.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {                    if (userDoc.exists()) {
                        Log.d(TAG, "✅ Found current user data in Firebase");
                        
                        // Get name from users collection (prioritize "nama" field)
                        String tempUsername = userDoc.getString("nama");
                        if (tempUsername == null || tempUsername.trim().isEmpty()) {
                            tempUsername = userDoc.getString("fullName");
                            if (tempUsername == null) tempUsername = userDoc.getString("firstName");
                            if (tempUsername == null) tempUsername = userDoc.getString("displayName");
                            if (tempUsername == null) tempUsername = userDoc.getString("email");
                            if (tempUsername == null) tempUsername = "You";
                        }
                        final String username = tempUsername; // Make effectively final for lambda
                          // Get profile photo and activeAvatar
                        final String photoURL = userDoc.getString("photoURL");
                        final String activeAvatar = userDoc.getString("activeAvatar");
                        
                        // Now get stats from user_stats collection
                        firestore.collection("user_stats")
                                .document(currentUserId)
                                .get()
                                .addOnSuccessListener(statsDoc -> {
                                    int userPoints = 0;
                                    double userDistance = 0.0;
                                    
                                    if (statsDoc.exists()) {
                                        // Get stats from user_stats collection (preferred)
                                        Object pointsObj = statsDoc.get("totalPoints");
                                        if (pointsObj == null) pointsObj = statsDoc.get("currentPoints");
                                        if (pointsObj instanceof Number) {
                                            userPoints = ((Number) pointsObj).intValue();
                                        }
                                        
                                        Object distanceObj = statsDoc.get("totalDistance");
                                        if (distanceObj == null) distanceObj = statsDoc.get("totalKm");
                                        if (distanceObj instanceof Number) {
                                            userDistance = ((Number) distanceObj).doubleValue();
                                        }
                                        
                                        Log.d(TAG, "📊 Stats from user_stats - Points: " + userPoints + ", Distance: " + userDistance);
                                    } else {
                                        // Fallback to users collection
                                        Object pointsObj = userDoc.get("totalPoints");
                                        if (pointsObj == null) pointsObj = userDoc.get("currentPoints");
                                        if (pointsObj instanceof Number) {
                                            userPoints = ((Number) pointsObj).intValue();
                                        }
                                        
                                        Object distanceObj = userDoc.get("totalKm");
                                        if (distanceObj == null) distanceObj = userDoc.get("totalDistance");
                                        if (distanceObj instanceof Number) {
                                            userDistance = ((Number) distanceObj).doubleValue();
                                        }
                                        
                                        Log.d(TAG, "📊 Stats from users (fallback) - Points: " + userPoints + ", Distance: " + userDistance);
                                    }
                                    
                                    // Update UI with user stats
                                    binding.tvMyUsername.setText(username);
                                    if (isPointsRanking) {
                                        binding.tvMyScore.setText(userPoints + " poin");
                                    } else {
                                        binding.tvMyScore.setText(String.format("%.1f km", userDistance / 1000.0));
                                    }
                                    
                                    // Display simplified stats (only KM and points)
                                    binding.tvMyStats.setText(String.format("%.1f km • %d poin", userDistance / 1000.0, userPoints));
                                      // Load profile image using activeAvatar if available
                                    if (activeAvatar != null && !activeAvatar.trim().isEmpty()) {
                                        // Use AvatarManager to load local avatar
                                        AvatarManager.loadAvatarIntoImageView(RankingTabFragment.this.requireContext(), binding.ivMyProfileImage, activeAvatar);
                                    } else {
                                        // Fallback to default avatar when activeAvatar is missing
                                        binding.ivMyProfileImage.setImageResource(R.drawable.avatar_default);
                                    }
                                    
                                    Log.d(TAG, "✅ Current user stats updated - Points: " + userPoints + ", Distance: " + userDistance + ", Name: " + username);
                                })
                                .addOnFailureListener(statsError -> {
                                    Log.e(TAG, "❌ Error fetching user_stats, using fallback data", statsError);
                                    // Continue with user data only
                                    binding.tvMyUsername.setText(username);
                                    binding.tvMyScore.setText("-- " + (isPointsRanking ? "poin" : "km"));
                                    binding.tvMyStats.setText("-- km • -- poin");
                                });
                    } else {
                        Log.w(TAG, "⚠️ Current user document not found in Firebase");
                        binding.myRankingCard.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error fetching current user data from Firebase", e);
                    binding.myRankingCard.setVisibility(View.GONE);                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Stop Firebase listeners to prevent memory leaks
        if (dataManager != null) {
            dataManager.stopAllListeners();
        }
        
        binding = null;
    }
}

package com.example.glean.fragment.community;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.glean.R;
import com.example.glean.adapter.RankingAdapter;
import com.example.glean.databinding.FragmentRankingBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RankingEntity;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.TrashEntity;
import com.example.glean.model.UserEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RankingFragment extends Fragment {
    
    private static final String TAG = "RankingFragment";
    
    private FragmentRankingBinding binding;
    private RankingAdapter rankingAdapter;
    private List<RankingEntity> rankings = new ArrayList<>();
    private String currentFilter = "weekly";
    
    // Database components
    private AppDatabase db;
    private ExecutorService executor;
    private int currentUserId = -1;
      @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRankingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize database and executor
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        // Get current user ID
        getCurrentUserId();
        
        setupSpinner();
        setupRecyclerView();
        loadRankings();    }
    
    private void getCurrentUserId() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        currentUserId = prefs.getInt("USER_ID", -1);
        
        // Fallback to USER_PREFS if not found
        if (currentUserId == -1) {
            SharedPreferences userPrefs = requireActivity().getSharedPreferences("USER_PREFS", 0);
            currentUserId = userPrefs.getInt("USER_ID", -1);
        }
        
        Log.d(TAG, "Current User ID: " + currentUserId);
    }
      private void setupSpinner() {
        // Removed time filter spinner - showing only global ranking
        currentFilter = "all_time";
    }
    
    private void setupRecyclerView() {
        rankingAdapter = new RankingAdapter(rankings);
        binding.recyclerViewRanking.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewRanking.setAdapter(rankingAdapter);
    }    private void loadRankings() {
        rankings.clear();
        
        // Load rankings from database in background thread
        executor.execute(() -> {
            try {                // Note: Automatic test data generation disabled
                // Use only real users from database
                int userCount = db.userDao().getUserCount();
                Log.d(TAG, "Current user count in database: " + userCount);
                
                // Calculate time range based on current filter
                long[] timeRange = getTimeRange(currentFilter);
                long startTime = timeRange[0];
                long endTime = timeRange[1];
                
                List<UserEntity> topUsers = db.userDao().getTopUsersSync(50); // Get top 50 users
                Log.d(TAG, "Found " + topUsers.size() + " users in database for filter: " + currentFilter);
                
                List<RankingEntity> tempRankings = new ArrayList<>();
                
                for (UserEntity user : topUsers) {
                    RankingEntity ranking = new RankingEntity();
                    
                    ranking.setUsername(user.getUsername());
                    ranking.setAvatar(user.getProfileImagePath() != null ? user.getProfileImagePath() : "avatar_default");
                    
                    // Calculate time-filtered statistics
                    UserStatistics stats = calculateUserStatisticsForPeriod(user.getId(), startTime, endTime);
                    
                    // For time-filtered views, use calculated points from activities, otherwise use total points
                    if ("all_time".equals(currentFilter)) {
                        ranking.setPoints(user.getPoints());
                    } else {
                        ranking.setPoints(stats.timeFilteredPoints);
                    }
                    
                    ranking.setTrashCollected(stats.totalTrashWeight);
                    ranking.setDistance(stats.totalDistance);
                    ranking.setBadges(stats.badgeCount);
                    
                    // Only include users with activity in the time period for filtered views
                    if ("all_time".equals(currentFilter) || stats.timeFilteredPoints > 0) {
                        tempRankings.add(ranking);
                    }
                }
                
                // Sort by points (highest first)
                tempRankings.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));
                
                // Set positions
                for (int i = 0; i < tempRankings.size(); i++) {
                    tempRankings.get(i).setPosition(i + 1);
                }
                  // Update UI on main thread
                getActivity().runOnUiThread(() -> {
                    rankings.clear();
                    rankings.addAll(tempRankings);
                    rankingAdapter.notifyDataSetChanged();
                    updateMyRanking();
                    
                    // Show message if no rankings found
                    if (rankings.isEmpty()) {
                        Toast.makeText(getContext(), "Belum ada data ranking. Mulai aktivitas plogging untuk muncul di ranking!", Toast.LENGTH_LONG).show();
                    }
                    
                    Log.d(TAG, "Rankings updated with " + rankings.size() + " users for " + currentFilter);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading rankings from database", e);
                // Fall back to showing empty list or error message
                getActivity().runOnUiThread(() -> {
                    rankings.clear();
                    rankingAdapter.notifyDataSetChanged();
                });
            }
        });
    }
    
    private long[] getTimeRange(String filter) {
        Calendar calendar = Calendar.getInstance();
        long endTime = System.currentTimeMillis();
        long startTime = 0;
        
        switch (filter) {
            case "weekly":
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                startTime = calendar.getTimeInMillis();
                break;
                
            case "monthly":
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                startTime = calendar.getTimeInMillis();
                break;
                
            case "all_time":
            default:
                startTime = 0; // Beginning of time
                break;
        }
        
        return new long[]{startTime, endTime};
    }    // Helper class to store user statistics
    private static class UserStatistics {
        float totalTrashWeight = 0.0f;
        float totalDistance = 0.0f;
        int badgeCount = 0;
        int timeFilteredPoints = 0;
    }
    
    private UserStatistics calculateUserStatistics(int userId) {
        return calculateUserStatisticsForPeriod(userId, 0, System.currentTimeMillis());
    }
    
    private UserStatistics calculateUserStatisticsForPeriod(int userId, long startTime, long endTime) {
        UserStatistics stats = new UserStatistics();
        
        try {
            if (startTime == 0) {
                // All-time statistics
                float totalDistance = db.recordDao().getTotalDistanceByUserId(userId);
                stats.totalDistance = totalDistance / 1000.0f; // Convert to km if stored in meters
                
                // Get all trash data for this user
                List<TrashEntity> userTrash = db.trashDao().getTrashByUserIdSync(userId);
                stats.totalTrashWeight = userTrash.size() * 0.5f; // Approximate 0.5kg per trash item
                
                // Use total points from user entity
                UserEntity user = db.userDao().getUserByIdSync(userId);
                stats.timeFilteredPoints = user != null ? user.getPoints() : 0;
                
            } else {
                // Time-filtered statistics - would need additional DAO methods for date ranges
                // For now, approximate by getting all records and filtering
                List<RecordEntity> allRecords = db.recordDao().getRecordsByUserIdSync(userId);
                float filteredDistance = 0;
                int filteredPoints = 0;
                
                for (RecordEntity record : allRecords) {
                    if (record.getCreatedAt() >= startTime && record.getCreatedAt() <= endTime) {
                        filteredDistance += record.getDistance();
                        filteredPoints += record.getPoints();
                    }
                }
                
                stats.totalDistance = filteredDistance / 1000.0f;
                stats.timeFilteredPoints = filteredPoints;
                
                // Count trash in time period
                List<TrashEntity> allTrash = db.trashDao().getTrashByUserIdSync(userId);
                int trashCount = 0;
                for (TrashEntity trash : allTrash) {
                    if (trash.getTimestamp() >= startTime && trash.getTimestamp() <= endTime) {
                        trashCount++;
                    }
                }
                stats.totalTrashWeight = trashCount * 0.5f;
            }
            
            // Calculate badges based on achievements
            stats.badgeCount = calculateBadgeCount(userId, stats.totalDistance, stats.totalTrashWeight);
            
            Log.d(TAG, "User " + userId + " stats - Distance: " + stats.totalDistance + "km, Trash: " + stats.totalTrashWeight + "kg, Badges: " + stats.badgeCount + ", Points: " + stats.timeFilteredPoints);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating stats for user " + userId, e);
        }
        
        return stats;
    }
    
    private int calculateBadgeCount(int userId, float totalDistance, float totalTrashWeight) {
        int badges = 0;
        
        // Distance-based badges
        if (totalDistance >= 100) badges++; // 100km badge
        if (totalDistance >= 50) badges++;  // 50km badge
        if (totalDistance >= 20) badges++;  // 20km badge
        if (totalDistance >= 10) badges++;  // 10km badge
        
        // Trash collection badges
        if (totalTrashWeight >= 100) badges++; // 100kg badge
        if (totalTrashWeight >= 50) badges++;  // 50kg badge
        if (totalTrashWeight >= 20) badges++;  // 20kg badge
        if (totalTrashWeight >= 10) badges++;  // 10kg badge
        
        // Activity badges
        int recordCount = db.recordDao().getRecordCountByUserId(userId);
        if (recordCount >= 100) badges++; // 100 activities badge
        if (recordCount >= 50) badges++;  // 50 activities badge
        if (recordCount >= 20) badges++;  // 20 activities badge
        if (recordCount >= 10) badges++;  // 10 activities badge
        
        return badges;
    }
    
    private void updateMyRanking() {
        if (currentUserId == -1) {
            binding.myRankingCard.setVisibility(View.GONE);
            return;
        }
        
        executor.execute(() -> {
            try {
                UserEntity currentUser = db.userDao().getUserByIdSync(currentUserId);
                if (currentUser == null) {
                    getActivity().runOnUiThread(() -> binding.myRankingCard.setVisibility(View.GONE));
                    return;
                }
                
                // Find current user's position in rankings
                int position = -1;
                for (int i = 0; i < rankings.size(); i++) {
                    if (rankings.get(i).getUsername().equals(currentUser.getUsername())) {
                        position = i + 1;
                        break;
                    }
                }
                
                // If not in top rankings, calculate position by counting users with higher points
                if (position == -1) {
                    List<UserEntity> allUsers = db.userDao().getAllUsersSync();
                    int higherPointsCount = 0;
                    for (UserEntity user : allUsers) {
                        if (user.getPoints() > currentUser.getPoints()) {
                            higherPointsCount++;
                        }
                    }
                    position = higherPointsCount + 1;
                }
                
                // Calculate current user's statistics
                UserStatistics myStats = calculateUserStatistics(currentUserId);
                
                final int finalPosition = position;
                getActivity().runOnUiThread(() -> {
                    binding.myRankingCard.setVisibility(View.VISIBLE);
                    binding.tvMyPosition.setText("#" + finalPosition);
                    binding.tvMyUsername.setText(currentUser.getUsername());
                    binding.tvMyPoints.setText(currentUser.getPoints() + " poin");
                    binding.tvMyStats.setText(String.format("%.1fkg • %.1fkm • %d badge", 
                        myStats.totalTrashWeight, myStats.totalDistance, myStats.badgeCount));
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating my ranking", e);
                getActivity().runOnUiThread(() -> binding.myRankingCard.setVisibility(View.GONE));
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

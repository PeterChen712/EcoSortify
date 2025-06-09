package com.example.glean.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glean.databinding.FragmentStatsBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsFragment extends Fragment {

    private FragmentStatsBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private int userId = -1;
    private UserEntity user;
    private List<RecordEntity> recordList = new ArrayList<>();
    private boolean dataLoaded = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        // Get user ID from SharedPreferences using correct method
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        userId = prefs.getInt("USER_ID", -1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Load data
        loadData();
    }

    private void loadData() {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        
        if (userId != -1) {
            // Load user data
            db.userDao().getUserById(userId).observe(getViewLifecycleOwner(), userEntity -> {
                if (userEntity != null) {
                    user = userEntity;
                }
            });
            
            // Load record data
            db.recordDao().getRecordsByUserId(userId).observe(getViewLifecycleOwner(), records -> {
                binding.progressIndicator.setVisibility(View.GONE);
                
                if (records != null && !records.isEmpty()) {
                    recordList = records;
                    updateUserStats();
                    dataLoaded = true;
                } else {
                    recordList = new ArrayList<>();
                    updateUserStats();
                }
            });
        } else {
            binding.progressIndicator.setVisibility(View.GONE);
            updateUserStats();
        }
    }

    private void updateUserStats() {
        // Calculate statistics from recordList
        int totalRuns = recordList.size();
        int totalPoints = user != null ? user.getPoints() : 0;
        
        // Calculate total distance
        float totalDistance = 0;
        long totalDuration = 0;
        
        for (RecordEntity record : recordList) {
            totalDistance += record.getDistance();
            totalDuration += record.getDuration();
        }
        
        // Update UI with real user data
        binding.tvTotalRuns.setText(String.valueOf(totalRuns));
        binding.tvTotalPoints.setText(String.valueOf(totalPoints));
        binding.tvTotalDistance.setText(totalDistance > 0 ? 
            String.format("%.1f", totalDistance / 1000) : "0.0");
        
        // Calculate and display achievements/badges count based on points
        int badgeCount = calculateBadgeCount(totalPoints);
        binding.tvAchievements.setText(String.valueOf(badgeCount));
        
        // Calculate average run time
        if (!recordList.isEmpty()) {
            long avgDuration = totalDuration / recordList.size();
            binding.tvAverageTime.setText(formatDuration(avgDuration / 1000)); // Convert to seconds
        } else {
            binding.tvAverageTime.setText("0 menit");
        }
    }
    
    private int calculateBadgeCount(int points) {
        // Calculate badges based on point thresholds
        if (points >= 1000) return 5;
        if (points >= 500) return 4;
        if (points >= 200) return 3;
        if (points >= 100) return 2;
        if (points >= 50) return 1;
        return 0;
    }
    
    private String formatDuration(long seconds) {
        long minutes = seconds / 60;
        if (minutes > 0) {
            return minutes + " menit";
        } else {
            return seconds + " detik";
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}
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
import androidx.lifecycle.LiveData;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.glean.R;
import com.example.glean.adapter.RecordAdapter;
import com.example.glean.databinding.FragmentHomeBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment implements RecordAdapter.OnRecordClickListener {

    private FragmentHomeBinding binding;
    private AppDatabase db;
    private int userId;
    private ExecutorService executor;
    private RecordAdapter recentActivitiesAdapter;
    private List<RecordEntity> recentActivities = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        userId = prefs.getInt("USER_ID", -1);
        executor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set click listener for starting plogging activity
        binding.btnStartPlogging.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_homeFragment_to_ploggingFragment);
        });

        // Setup recent activities RecyclerView
        setupRecentActivities();
        
        // Load user data
        loadUserData();
        
        // Load recent activities
        loadRecentActivities();
    }

    private void setupRecentActivities() {
        binding.rvRecentActivity.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentActivitiesAdapter = new RecordAdapter(recentActivities, this);
        binding.rvRecentActivity.setAdapter(recentActivitiesAdapter);
    }

    private void loadUserData() {
        if (userId != -1) {
            LiveData<UserEntity> userLiveData = db.userDao().getUserById(userId);
            userLiveData.observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    // Use firstName and lastName instead of getName()
                    String displayName = getDisplayName(user);
                    binding.tvWelcome.setText("Welcome, " + displayName + "!");
                    
                    // Load user statistics
                    loadUserStats();
                }
            });
        }
    }
    
    private String getDisplayName(UserEntity user) {
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                return user.getFirstName() + " " + user.getLastName();
            }
            return user.getFirstName();
        } else if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            return user.getUsername();
        } else if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            return user.getEmail().split("@")[0]; // Use email prefix as display name
        }
        return "User";
    }
    
    private void loadUserStats() {
        if (userId != -1) {
            executor.execute(() -> {
                // Get user statistics from database
                int totalRecords = db.recordDao().getRecordCountByUserId(userId);
                float totalDistance = db.recordDao().getTotalDistanceByUserId(userId);
                long totalDuration = db.recordDao().getTotalDurationByUserId(userId);
                
                // Convert distance to km and duration to hours
                float distanceKm = totalDistance / 1000f;
                float durationHours = totalDuration / (1000f * 60f * 60f);
                
                requireActivity().runOnUiThread(() -> {
                    String statsText = String.format("Sessions: %d | Distance: %.1f km | Time: %.1f hrs", 
                            totalRecords, distanceKm, durationHours);
                    binding.tvStats.setText(statsText);
                });
            });
        }
    }

    private void loadRecentActivities() {
        if (userId != -1) {
            executor.execute(() -> {
                // Get recent activities (limit to 5 most recent)
                List<RecordEntity> records = db.recordDao().getRecordsByUserIdSync(userId);
                List<RecordEntity> recentRecords = new ArrayList<>();
                
                // Get only the first 5 records (most recent)
                int limit = Math.min(records.size(), 5);
                for (int i = 0; i < limit; i++) {
                    recentRecords.add(records.get(i));
                }
                
                requireActivity().runOnUiThread(() -> {
                    recentActivities.clear();
                    recentActivities.addAll(recentRecords);
                    recentActivitiesAdapter.notifyDataSetChanged();
                });
            });
        }
    }
    
    @Override
    public void onRecordClick(RecordEntity record) {
        // Navigate to record details or summary
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putInt("RECORD_ID", record.getId());
        
        // Use the correct navigation action that exists in your navigation graph
        try {
            navController.navigate(R.id.historyFragment, args);
        } catch (Exception e) {
            // Fallback: just log the record click or show a toast
            android.util.Log.d("HomeFragment", "Record clicked: " + record.getId());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
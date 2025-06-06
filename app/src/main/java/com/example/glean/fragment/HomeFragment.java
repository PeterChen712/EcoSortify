package com.example.glean.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.glean.R;
import com.example.glean.activity.MainActivity;
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
    private ExecutorService executor;
    private List<RecordEntity> recentActivities = new ArrayList<>();
    private RecordAdapter recentActivitiesAdapter;    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            db = AppDatabase.getInstance(requireContext());
            executor = Executors.newSingleThreadExecutor();
        } catch (Exception e) {
        }
    }    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            binding = FragmentHomeBinding.inflate(inflater, container, false);
            return binding.getRoot();
        } catch (Exception e) {
            return null;
        }
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            initializeDashboardContent();
            setupStartPloggingButton();
            setupQuickActionButtons();
            setupRecentActivities();
            loadUserData();
            loadRecentActivities();
            updateDashboardStats();
        } catch (Exception e) {
            showErrorMessage("Error loading home screen: " + e.getMessage());
        }
    }    private void setupStartPloggingButton() {
        if (binding.btnStartPlogging != null) {
            binding.btnStartPlogging.setVisibility(View.VISIBLE);
            binding.btnStartPlogging.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToPlgging();
                } else {
                    Toast.makeText(requireContext(), "Navigation not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }    private void initializeDashboardContent() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            
            if (binding.tvWelcomeMessage != null) {
                String welcomeMsg = prefs.getString("GREETING_MESSAGE", "🌱 Selamat datang di GleanGo!");
                binding.tvWelcomeMessage.setText(welcomeMsg);
                binding.tvWelcomeMessage.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvCurrentDate != null) {
                String currentDate = prefs.getString("CURRENT_DATE", "📅 " + new java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date()));
                binding.tvCurrentDate.setText(currentDate);
                binding.tvCurrentDate.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvMotivation != null) {
                String motivation = prefs.getString("MOTIVATION_MESSAGE", "💚 Mari bersihkan dunia hari ini!");
                binding.tvMotivation.setText(motivation);
                binding.tvMotivation.setVisibility(View.VISIBLE);
            }

            updateQuickStatsUI(prefs);
            updateWeeklyChallengeUI(prefs);

            if (binding.tvDailyTip != null) {
                String dailyTip = prefs.getString("DAILY_TIP", "💡 Tip: Mulai plogging hari ini untuk hidup lebih sehat!");
                binding.tvDailyTip.setText(dailyTip);
                binding.tvDailyTip.setVisibility(View.VISIBLE);
            }
            
        } catch (Exception e) {
        }
    }    private void updateQuickStatsUI(SharedPreferences prefs) {
        try {
            if (binding.tvQuickStatsTitle != null) {
                binding.tvQuickStatsTitle.setText(prefs.getString("QUICK_STATS_TITLE", "📊 STATISTIK MINGGU INI"));
                binding.tvQuickStatsTitle.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvQuickStatsDistance != null) {
                binding.tvQuickStatsDistance.setText(prefs.getString("QUICK_STATS_DISTANCE", "🏃 Total Lari: 0 km"));
                binding.tvQuickStatsDistance.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvQuickStatsTrash != null) {
                binding.tvQuickStatsTrash.setText(prefs.getString("QUICK_STATS_TRASH", "🗑️ Sampah Terkumpul: 0"));
                binding.tvQuickStatsTrash.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvQuickStatsPoints != null) {
                binding.tvQuickStatsPoints.setText(prefs.getString("QUICK_STATS_POINTS", "⭐ Poin: 0"));
                binding.tvQuickStatsPoints.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvQuickStatsBadge != null) {
                binding.tvQuickStatsBadge.setText(prefs.getString("QUICK_STATS_BADGE", "🏆 Badge: Eco Beginner"));
                binding.tvQuickStatsBadge.setVisibility(View.VISIBLE);
            }
            
        } catch (Exception e) {
        }
    }    private void updateWeeklyChallengeUI(SharedPreferences prefs) {
        try {
            if (binding.tvChallengeTitle != null) {
                binding.tvChallengeTitle.setText(prefs.getString("CHALLENGE_TITLE", "🎯 CHALLENGE MINGGU INI"));
                binding.tvChallengeTitle.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvChallengeTarget != null) {
                binding.tvChallengeTarget.setText(prefs.getString("CHALLENGE_TARGET", "Target: Kumpulkan 50 sampah"));
                binding.tvChallengeTarget.setVisibility(View.VISIBLE);
            }
            
            // Update progress bar and text
            int challengeProgress = prefs.getInt("CHALLENGE_PROGRESS", 0);
            int challengeTarget = 50; // Default target
            
            if (binding.progressChallenge != null) {
                binding.progressChallenge.setMax(challengeTarget);
                binding.progressChallenge.setProgress(challengeProgress);
                binding.progressChallenge.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvChallengeProgress != null) {
                int percentage = challengeTarget > 0 ? (challengeProgress * 100) / challengeTarget : 0;
                String progressText = String.format("Progress: %d/%d (%d%%)", challengeProgress, challengeTarget, percentage);
                binding.tvChallengeProgress.setText(progressText);
                binding.tvChallengeProgress.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvChallengeRemaining != null) {
                binding.tvChallengeRemaining.setText(prefs.getString("CHALLENGE_REMAINING", "Sisa: 7 hari lagi"));
                binding.tvChallengeRemaining.setVisibility(View.VISIBLE);
            }
            
        } catch (Exception e) {
        }
    }private void setupQuickActionButtons() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            
            if (binding.btnQuickAction1 != null) {
                binding.btnQuickAction1.setText(prefs.getString("QUICK_ACTION_1", "🏃‍♂️ Mulai Plogging"));
                binding.btnQuickAction1.setVisibility(View.VISIBLE);
                binding.btnQuickAction1.setOnClickListener(v -> {
                    try {
                        NavController navController = Navigation.findNavController(requireView());
                        navController.navigate(R.id.action_homeFragment_to_ploggingFragment);
                    } catch (Exception e) {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateToPlgging();
                        }
                    }
                });
            }

            if (binding.btnQuickAction2 != null) {
                binding.btnQuickAction2.setText(prefs.getString("QUICK_ACTION_2", "📊 Lihat Statistik"));
                binding.btnQuickAction2.setVisibility(View.VISIBLE);
                binding.btnQuickAction2.setOnClickListener(v -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToStats();
                    }
                });
            }

            if (binding.btnQuickAction3 != null) {
                binding.btnQuickAction3.setText(prefs.getString("QUICK_ACTION_3", "📰 Berita Lingkungan"));
                binding.btnQuickAction3.setVisibility(View.VISIBLE);
                binding.btnQuickAction3.setOnClickListener(v -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToNews();
                    }
                });
            }

            if (binding.btnQuickAction4 != null) {
                binding.btnQuickAction4.setText(prefs.getString("QUICK_ACTION_4", "🗺️ Peta Sampah"));
                binding.btnQuickAction4.setVisibility(View.VISIBLE);
                binding.btnQuickAction4.setOnClickListener(v -> {
                    try {
                        NavController navController = Navigation.findNavController(requireView());
                        navController.navigate(R.id.action_homeFragment_to_trashMapFragment);
                    } catch (Exception e) {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateToTrashMap();
                        }
                    }
                });
            }
            
        } catch (Exception e) {
        }
    }    private void setupRecentActivities() {
        try {
            if (binding.rvRecentActivity != null) {
                binding.rvRecentActivity.setLayoutManager(new LinearLayoutManager(requireContext()));
                recentActivitiesAdapter = new RecordAdapter(recentActivities, this);
                binding.rvRecentActivity.setAdapter(recentActivitiesAdapter);
                binding.rvRecentActivity.setVisibility(View.VISIBLE);
            }

            if (binding.btnViewAllHistory != null) {
                binding.btnViewAllHistory.setVisibility(View.VISIBLE);
                binding.btnViewAllHistory.setOnClickListener(v -> {
                    try {
                        NavController navController = Navigation.findNavController(requireView());
                        navController.navigate(R.id.action_homeFragment_to_historyFragment);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Fitur riwayat sedang dimuat...", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
        } catch (Exception e) {
        }
    }    private void loadUserData() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            int userId = prefs.getInt("USER_ID", -1);
            
            if (userId != -1) {
                LiveData<UserEntity> userLiveData = db.userDao().getUserById(userId);
                userLiveData.observe(getViewLifecycleOwner(), user -> {
                    if (user != null) {
                        String displayName = getDisplayName(user);
                        
                        String greetingWithName = "🌱 " + getTimeGreeting() + ", " + displayName + "!";
                        if (binding.tvWelcomeMessage != null) {
                            binding.tvWelcomeMessage.setText(greetingWithName);
                        }
                        
                        SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        prefs2.edit().putString("USERNAME", displayName).apply();
                        
                        loadUserStats(userId);
                    }
                });
            } else {
                if (binding.tvWelcomeMessage != null) {
                    binding.tvWelcomeMessage.setText("🌱 " + getTimeGreeting() + ", Welcome!");
                }
            }
        } catch (Exception e) {
        }
    }
    
    private String getTimeGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 12) {
            return "Selamat Pagi";
        } else if (hour >= 12 && hour < 17) {
            return "Selamat Siang";
        } else if (hour >= 17 && hour < 20) {
            return "Selamat Sore";
        } else {
            return "Selamat Malam";
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
      private void loadUserStats(int userId) {
        executor.execute(() -> {
            try {
                int totalRecords = db.recordDao().getRecordCountByUserId(userId);
                float totalDistance = db.recordDao().getTotalDistanceByUserId(userId);
                long totalDuration = db.recordDao().getTotalDurationByUserId(userId);
                
                float distanceKm = totalDistance / 1000f;
                float durationHours = totalDuration / (1000f * 60f * 60f);
                
                requireActivity().runOnUiThread(() -> {
                    String statsText = String.format("Sessions: %d | Distance: %.1f km | Time: %.1f hrs", 
                            totalRecords, distanceKm, durationHours);
                    
                    if (binding.tvMotivation != null) {
                        String combinedText = "💚 Mari bersihkan dunia!\n📊 " + statsText;
                        binding.tvMotivation.setText(combinedText);
                    }
                });
            } catch (Exception e) {
            }
        });
    }    private void updateDashboardStats() {
        executor.execute(() -> {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                int userId = prefs.getInt("USER_ID", -1);
                
                if (userId != -1) {
                    long weekStart = getWeekStartTime();
                    long weekEnd = System.currentTimeMillis();
                    
                    List<RecordEntity> allRecords = db.recordDao().getRecordsByUserIdSync(userId);
                    List<RecordEntity> weeklyRecords = new ArrayList<>();
                    
                    for (RecordEntity record : allRecords) {
                        if (record.getCreatedAt() >= weekStart && record.getCreatedAt() <= weekEnd) {
                            weeklyRecords.add(record);
                        }
                    }
                    
                    float totalDistance = 0f;
                    int totalTrash = 0;
                    int totalPoints = 0;
                    
                    for (RecordEntity record : weeklyRecords) {
                        totalDistance += record.getDistance();
                        totalPoints += record.getPoints();
                        List<com.example.glean.model.TrashEntity> trashList = db.trashDao().getTrashByRecordIdSync(record.getId());
                        totalTrash += trashList.size();
                    }
                    
                    prefs.edit()
                        .putFloat("WEEKLY_DISTANCE", totalDistance)
                        .putInt("WEEKLY_TRASH", totalTrash)
                        .putInt("WEEKLY_POINTS", totalPoints)
                        .putInt("CHALLENGE_PROGRESS", totalTrash)
                        .apply();
                    
                    requireActivity().runOnUiThread(() -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).updateStatsFromDatabase();
                            initializeDashboardContent();
                        }
                    });
                }
            } catch (Exception e) {
            }
        });
    }

    private long getWeekStartTime() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }    private void loadRecentActivities() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int userId = prefs.getInt("USER_ID", -1);
        
        if (userId != -1) {
            executor.execute(() -> {
                try {
                    List<RecordEntity> records = db.recordDao().getRecordsByUserIdSync(userId);
                    List<RecordEntity> recentRecords = new ArrayList<>();
                    
                    int limit = Math.min(records.size(), 5);
                    for (int i = 0; i < limit; i++) {
                        recentRecords.add(records.get(i));
                    }
                    
                    requireActivity().runOnUiThread(() -> {
                        if (recentActivitiesAdapter != null) {
                            recentActivities.clear();
                            recentActivities.addAll(recentRecords);
                            recentActivitiesAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {
                }
            });
        }
    }
      @Override
    public void onRecordClick(RecordEntity record) {
        try {
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putInt("RECORD_ID", record.getId());
            
            navController.navigate(R.id.historyFragment, args);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Membuka detail aktivitas...", Toast.LENGTH_SHORT).show();
        }
    }

    private void showErrorMessage(String message) {
        try {
            if (getContext() != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
            
            if (binding != null && binding.tvMotivation != null) {
                binding.tvMotivation.setText("❌ Error: " + message);
                binding.tvMotivation.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        try {
            updateDashboardStats();
            loadRecentActivities();
        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
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
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment implements RecordAdapter.OnRecordClickListener {
    private FragmentHomeBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private List<RecordEntity> recentActivities = new ArrayList<>();
    private RecordAdapter recentActivitiesAdapter;
    private int currentTimeFilter = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            db = AppDatabase.getInstance(requireContext());
            executor = Executors.newSingleThreadExecutor();
        } catch (Exception e) {
            // Handle error silently
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            binding = FragmentHomeBinding.inflate(inflater, container, false);
            return binding.getRoot();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
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
    }

    private void setupStartPloggingButton() {
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
    }

    private void initializeDashboardContent() {
        try {
            // Set current date
            if (binding.tvCurrentDate != null) {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, d MMMM yyyy", 
                    new java.util.Locale("id", "ID"));
                String currentDate = dateFormat.format(new java.util.Date());
                binding.tvCurrentDate.setText(currentDate);
                binding.tvCurrentDate.setVisibility(View.VISIBLE);
            }
            
            // Set motivation message
            if (binding.tvMotivation != null) {
                binding.tvMotivation.setText("Mari bersihkan dunia hari ini!");
                binding.tvMotivation.setVisibility(View.VISIBLE);
            }

            // Set daily tip
            if (binding.tvDailyTip != null) {
                String dailyTip = getDailyTip();
                binding.tvDailyTip.setText(dailyTip);
                binding.tvDailyTip.setVisibility(View.VISIBLE);
            }
            
            updateQuickStatsUI();
            updateWeeklyChallengeUI();
            
        } catch (Exception e) {
            // Handle error silently
        }
    }
    
    private String getDailyTip() {
        String[] ecoTips = {
            "Bawa botol minum sendiri untuk mengurangi sampah plastik!",
            "Pilah sampah organik dan anorganik saat plogging!",
            "1 botol plastik butuh 450 tahun untuk terurai di alam!",
            "Setiap langkah kecil membuat perubahan besar untuk bumi!",
            "Plogging membakar 300-400 kalori per 30 menit!",
            "Gunakan sarung tangan saat memungut sampah untuk keamanan!",
            "Foto sampah sebelum dipungut untuk dokumentasi yang baik!"
        };
        
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return ecoTips[dayOfWeek % ecoTips.length];
    }

    private void updateQuickStatsUI() {
        try {
            if (binding.tvQuickStatsTitle != null) {
                binding.tvQuickStatsTitle.setText("Statistik Keseluruhan");
                binding.tvQuickStatsTitle.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvQuickStatsDistance != null) {
                binding.tvQuickStatsDistance.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvQuickStatsTrash != null) {
                binding.tvQuickStatsTrash.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvQuickStatsPoints != null) {
                binding.tvQuickStatsPoints.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvQuickStatsBadge != null) {
                binding.tvQuickStatsBadge.setVisibility(View.VISIBLE);
            }
            
        } catch (Exception e) {
            // Handle error silently
        }
    }

    private void updateWeeklyChallengeUI() {
        try {
            if (binding.tvChallengeTitle != null) {
                String challengeTitle = "CHALLENGE GLOBAL";
                binding.tvChallengeTitle.setText(challengeTitle);
                binding.tvChallengeTitle.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvChallengeTarget != null) {
                binding.tvChallengeTarget.setVisibility(View.VISIBLE);
            }
            
            if (binding.progressChallenge != null) {
                binding.progressChallenge.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvChallengeProgress != null) {
                binding.tvChallengeProgress.setVisibility(View.VISIBLE);
            }
            
            if (binding.tvChallengeRemaining != null) {
                String remainingText = "Target: Semua waktu";
                binding.tvChallengeRemaining.setText(remainingText);
                binding.tvChallengeRemaining.setVisibility(View.VISIBLE);
            }
            
        } catch (Exception e) {
            // Handle error silently
        }
    }

    private void setupQuickActionButtons() {
        try {
            if (binding.btnQuickAction1 != null) {
                binding.btnQuickAction1.setText("Mulai Plogging");
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
                binding.btnQuickAction2.setText("Lihat Statistik");
                binding.btnQuickAction2.setVisibility(View.VISIBLE);
                binding.btnQuickAction2.setOnClickListener(v -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToStats();
                    }
                });
            }

            if (binding.btnQuickAction3 != null) {
                binding.btnQuickAction3.setText("Berita");
                binding.btnQuickAction3.setVisibility(View.VISIBLE);
                binding.btnQuickAction3.setOnClickListener(v -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToNews();
                    }
                });
            }

            if (binding.btnQuickAction4 != null) {
                binding.btnQuickAction4.setText("Peta Sampah");
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
            // Handle error silently
        }
    }

    private void setupRecentActivities() {
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
            // Handle error silently
        }
    }

    private void loadUserData() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            int userId = prefs.getInt("USER_ID", -1);
            
            if (userId != -1) {
                LiveData<UserEntity> userLiveData = db.userDao().getUserById(userId);
                userLiveData.observe(getViewLifecycleOwner(), user -> {
                    if (user != null) {
                        String displayName = getDisplayName(user);
                        String greetingWithName = getTimeGreeting() + ", " + displayName + "!";
                        if (binding.tvWelcomeMessage != null) {
                            binding.tvWelcomeMessage.setText(greetingWithName);
                        }
                        
                        loadUserStats(userId);
                    }
                });
            } else {
                if (binding.tvWelcomeMessage != null) {
                    binding.tvWelcomeMessage.setText(getTimeGreeting() + ", Welcome!");
                }
            }
        } catch (Exception e) {
            // Handle error silently
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
            return user.getEmail().split("@")[0];
        }
        return "User";
    }
    
    private List<RecordEntity> getAllRecords(List<RecordEntity> allRecords) {
        return allRecords;
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
                    String combinedText = String.format("Total: %d aktivitas, %.1f km, %.1f jam", 
                        totalRecords, distanceKm, durationHours);
                    if (binding.tvMotivation != null) {
                        binding.tvMotivation.setText(combinedText);
                    }
                });
            } catch (Exception e) {
                // Handle error silently
            }
        });
    }

    private void updateDashboardStats() {
        executor.execute(() -> {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                int userId = prefs.getInt("USER_ID", -1);
                
                if (userId != -1) {
                    List<RecordEntity> allRecords = db.recordDao().getRecordsByUserIdSync(userId);
                    List<RecordEntity> recordsToProcess = getAllRecords(allRecords);
                    
                    float totalDistance = 0f;
                    int totalTrash = 0;
                    int totalPoints = 0;
                    
                    for (RecordEntity record : recordsToProcess) {
                        totalDistance += record.getDistance();
                        totalPoints += record.getPoints();
                        List<com.example.glean.model.TrashEntity> trashList = db.trashDao().getTrashByRecordIdSync(record.getId());
                        totalTrash += trashList.size();
                    }
                    
                    String badge = getBadge(totalPoints);
                    
                    final float finalTotalDistance = totalDistance;
                    final int finalTotalTrash = totalTrash;
                    final int finalTotalPoints = totalPoints;
                    
                    requireActivity().runOnUiThread(() -> {
                        updateStatsDisplay(finalTotalDistance, finalTotalTrash, finalTotalPoints, badge);
                        updateChallengeProgress(finalTotalTrash);
                    });
                }
            } catch (Exception e) {
                // Handle error silently
            }
        });
    }
    
    private void updateStatsDisplay(float totalDistance, int totalTrash, int totalPoints, String badge) {
        try {
            if (binding.tvQuickStatsDistance != null) {
                binding.tvQuickStatsDistance.setText(String.format("Total Lari: %.1f km", totalDistance / 1000f));
            }
            
            if (binding.tvQuickStatsTrash != null) {
                binding.tvQuickStatsTrash.setText("Sampah Terkumpul: " + totalTrash);
            }
            
            if (binding.tvQuickStatsPoints != null) {
                binding.tvQuickStatsPoints.setText("Poin: " + totalPoints);
            }
            
            if (binding.tvQuickStatsBadge != null) {
                binding.tvQuickStatsBadge.setText("Badge: " + badge);
            }
        } catch (Exception e) {
            // Handle error silently
        }
    }
    
    private void updateChallengeProgress(int totalTrash) {
        try {
            int challengeTarget = 50;
            
            if (binding.progressChallenge != null) {
                binding.progressChallenge.setMax(challengeTarget);
                binding.progressChallenge.setProgress(Math.min(totalTrash, challengeTarget));
            }
            
            if (binding.tvChallengeProgress != null) {
                int percentage = challengeTarget > 0 ? (totalTrash * 100) / challengeTarget : 0;
                String progressText = String.format("Progress: %d/%d (%d%%)", totalTrash, challengeTarget, Math.min(percentage, 100));
                binding.tvChallengeProgress.setText(progressText);
            }
            
            if (binding.tvChallengeTarget != null) {
                String targetText = "Target: Kumpulkan " + challengeTarget + " sampah";
                binding.tvChallengeTarget.setText(targetText);
            }
        } catch (Exception e) {
            // Handle error silently
        }
    }
    
    private String getBadge(int points) {
        if (points >= 1000) return "Eco Master";
        else if (points >= 500) return "Eco Expert";
        else if (points >= 200) return "Eco Warrior";
        else if (points >= 50) return "Eco Fighter";
        else return "Eco Beginner";
    }
    
    private long getStartOfWeek() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void loadRecentActivities() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int userId = prefs.getInt("USER_ID", -1);
        
        if (userId != -1) {
            executor.execute(() -> {
                try {
                    List<RecordEntity> allRecords = db.recordDao().getRecordsByUserIdSync(userId);
                    List<RecordEntity> recordsToProcess = getAllRecords(allRecords);
                    recordsToProcess.sort((r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
                    List<RecordEntity> recentRecords = new ArrayList<>();
                    
                    // Limit to 3 recent activities
                    int limit = Math.min(recordsToProcess.size(), 3);
                    for (int i = 0; i < limit; i++) {
                        recentRecords.add(recordsToProcess.get(i));
                    }
                    
                    requireActivity().runOnUiThread(() -> {
                        recentActivities.clear();
                        recentActivities.addAll(recentRecords);
                        if (recentActivitiesAdapter != null) {
                            recentActivitiesAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {
                    // Handle error silently
                }
            });
        }
    }

    private void showErrorMessage(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordClick(RecordEntity record) {
        // Handle record click
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
package com.example.glean.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.auth.FirebaseAuthManager;
import com.example.glean.databinding.FragmentStatsBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.TrashEntity;
import com.example.glean.model.UserEntity;
import com.example.glean.model.UserStats;
import com.example.glean.service.FirebaseDataManager;
import com.example.glean.util.NetworkUtil;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsFragment extends Fragment {

    private FragmentStatsBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private int userId = -1;
    private UserEntity user;
    private List<RecordEntity> recordList = new ArrayList<>();
    private List<TrashEntity> trashList = new ArrayList<>();
    private boolean dataLoaded = false;
    private FirebaseAuthManager authManager;
    private FirebaseDataManager dataManager;    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        authManager = FirebaseAuthManager.getInstance(requireContext());
        dataManager = FirebaseDataManager.getInstance(requireContext());
        
        // Get user ID from SharedPreferences using correct method
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        userId = prefs.getInt("USER_ID", -1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupBackButton();
        
        // Check network connectivity and authentication before loading stats
        if (!NetworkUtil.isNetworkAvailable(requireContext())) {
            showNetworkError();
            return;
        }
        
        if (!authManager.isLoggedIn()) {
            showAuthenticationError();
            return;
        }
        
        // Setup navigation for history button
        binding.btnViewHistory.setOnClickListener(v -> {
            // Navigate to History activity or fragment
            try {
                Intent intent = new Intent(requireContext(), Class.forName("com.example.glean.activity.HistoryActivity"));
                startActivity(intent);
            } catch (ClassNotFoundException e) {
                // If HistoryActivity doesn't exist, show a message
                android.widget.Toast.makeText(requireContext(), 
                    "Fitur riwayat akan segera hadir!", 
                    android.widget.Toast.LENGTH_SHORT).show();
            }
        });        // Load data with Firebase sync
        loadDataWithFirebaseSync();
    }
    
    private void showNetworkError() {
        // Hide content and show network error
        binding.scrollViewContent.setVisibility(View.GONE);
        Toast.makeText(requireContext(), "Fitur statistik membutuhkan koneksi internet.", Toast.LENGTH_LONG).show();
    }
    
    private void showAuthenticationError() {
        // Hide content and show auth error
        binding.scrollViewContent.setVisibility(View.GONE);
        Toast.makeText(requireContext(), "Silakan login untuk melihat statistik Anda.", Toast.LENGTH_LONG).show();
    }

    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                Navigation.findNavController(v).popBackStack();
            }
        });
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
                if (records != null && !records.isEmpty()) {
                    recordList = records;
                    updateUserStats();
                    loadTrashData(); // Load trash data after records are loaded
                    dataLoaded = true;
                } else {
                    recordList = new ArrayList<>();
                    updateUserStats();
                    loadTrashData(); // Still load trash data even if no records
                }
            });        } else {
            binding.progressIndicator.setVisibility(View.GONE);
            updateUserStats();
            loadTrashData(); // Still load trash data
        }
    }

    private void loadTrashData() {
        if (userId != -1) {            executor.execute(() -> {
                // Get all trash data for this user
                trashList = db.trashDao().getTrashByUserIdSync(userId);
                
                // Check if fragment is still attached to activity
                if (!isAdded() || getActivity() == null) {
                    Log.d("StatsFragment", "⚠️ Fragment not attached, skipping trash data UI update");
                    return;
                }
                
                requireActivity().runOnUiThread(() -> {
                    // Double check in case fragment was detached during the runOnUiThread call
                    if (!isAdded() || getActivity() == null) {
                        Log.d("StatsFragment", "⚠️ Fragment detached during UI thread execution, skipping trash data update");
                        return;
                    }
                    
                    binding.progressIndicator.setVisibility(View.GONE);
                    updateTrashAnalytics();
                });
            });
        } else {
            binding.progressIndicator.setVisibility(View.GONE);
            updateTrashAnalytics();
        }
    }    private void updateUserStats() {
        // Calculate statistics from recordList
        int totalRuns = recordList.size();
        int totalPoints = user != null ? user.getPoints() : 0;
          // Calculate total distance
        float totalDistance = 0;
        long totalDuration = 0;
        
        // Calculate weekly distance (last 7 days)
        float weeklyDistance = calculateWeeklyDistance();
        
        for (RecordEntity record : recordList) {
            totalDistance += record.getDistance();
            totalDuration += record.getDuration();
        }
          // Update UI with real user data
        binding.tvTotalRuns.setText(String.valueOf(totalRuns));
        binding.tvTotalPoints.setText(String.valueOf(totalPoints));
        binding.tvTotalDistance.setText(totalDistance > 0 ? 
            String.format(Locale.getDefault(), "%.2f", totalDistance / 1000) : "0,00");
        
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
        
        // Setup weekly distance chart
        setupWeeklyDistanceChart();
        
        // Hide progress indicator after data is loaded
        binding.progressIndicator.setVisibility(View.GONE);
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
    }    private float calculateWeeklyDistance() {
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        float weeklyDistance = 0;
        
        for (RecordEntity record : recordList) {
            if (record.getCreatedAt() >= oneWeekAgo) {
                weeklyDistance += record.getDistance();
            }
        }
        
        return weeklyDistance;
    }
      private void setupWeeklyDistanceChart() {
        LineChart chart = binding.lineChartWeeklyDistance;
        TextView emptyState = binding.tvChartEmptyState;
        
        Log.d("StatsFragment", "📈 Setting up weekly distance chart");
        
        // Get daily distance data for the last 7 days
        Map<String, Float> dailyDistances = calculateDailyDistances();
        
        Log.d("StatsFragment", "📊 Chart data check - Daily distances map size: " + dailyDistances.size());
        Log.d("StatsFragment", "📊 All distances zero check: " + allDistancesZero(dailyDistances));
        
        if (dailyDistances.isEmpty() || allDistancesZero(dailyDistances)) {
            // Show empty state
            chart.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            Log.d("StatsFragment", "📈 Showing empty chart state - no valid distance data");
            return;
        }
        
        // Hide empty state and show chart
        chart.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        
        Log.d("StatsFragment", "📈 Building chart with valid data");
        
        // Prepare data entries
        List<Entry> entries = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        
        // Create entries for the last 7 days
        for (int i = 6; i >= 0; i--) {
            calendar.setTimeInMillis(System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000L));
            String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            String labelKey = sdf.format(calendar.getTime());
            
            float distance = dailyDistances.getOrDefault(dateKey, 0f);
            entries.add(new Entry(6 - i, distance / 1000f)); // Convert to km
            dateLabels.add(labelKey);
        }
        
        // Create dataset
        LineDataSet dataSet = new LineDataSet(entries, "Jarak Harian (km)");
        dataSet.setColor(getResources().getColor(R.color.primary_green, null));
        dataSet.setCircleColor(getResources().getColor(R.color.primary_green, null));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.text_primary, null));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.primary_green, null));
        dataSet.setFillAlpha(30);
        
        // Format values to show 2 decimal places
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.2f", value);
            }
        });
        
        // Create line data
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        
        // Customize chart appearance
        Description description = new Description();
        description.setText("");
        description.setTextColor(getResources().getColor(R.color.text_secondary, null));
        description.setTextSize(12f);
        chart.setDescription(description);
        
        // Customize X-axis (dates)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dateLabels.size()) {
                    return dateLabels.get(index);
                }
                return "";
            }
        });
        xAxis.setTextColor(getResources().getColor(R.color.text_secondary, null));
        xAxis.setAxisLineColor(getResources().getColor(R.color.text_tertiary, null));
        xAxis.setGridColor(getResources().getColor(R.color.text_tertiary, null));
        
        // Customize Y-axis (distance)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(getResources().getColor(R.color.text_secondary, null));
        leftAxis.setAxisLineColor(getResources().getColor(R.color.text_tertiary, null));
        leftAxis.setGridColor(getResources().getColor(R.color.text_tertiary, null));        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.2f km", value);
            }
        });
        
        // Hide right Y-axis
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Enable touch gestures
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        
        // Animate chart
        chart.animateX(1000);
        
        // Refresh chart
        chart.invalidate();
    }      private Map<String, Float> calculateDailyDistances() {
        Map<String, Float> dailyDistances = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Get records from the last 7 days
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        
        Log.d("StatsFragment", "📈 Calculating daily distances for last 7 days");
        Log.d("StatsFragment", "📊 Total records to process: " + (recordList != null ? recordList.size() : 0));
        
        if (recordList == null || recordList.isEmpty()) {
            Log.d("StatsFragment", "⚠️ No records available for distance calculation");
            return dailyDistances;
        }
        
        for (RecordEntity record : recordList) {
            if (record.getCreatedAt() >= oneWeekAgo) {
                String dateKey = sdf.format(new Date(record.getCreatedAt()));
                float currentDistance = dailyDistances.getOrDefault(dateKey, 0f);
                float newDistance = currentDistance + record.getDistance();
                dailyDistances.put(dateKey, newDistance);
                
                Log.d("StatsFragment", "📊 Date: " + dateKey + ", Record distance: " + 
                    record.getDistance() + "m, Daily total: " + newDistance + "m");
            }
        }
        
        Log.d("StatsFragment", "✅ Daily distances calculated: " + dailyDistances.toString());
        return dailyDistances;
    }      private boolean allDistancesZero(Map<String, Float> dailyDistances) {
        Log.d("StatsFragment", "🔍 Checking if all distances are zero...");
        
        for (Map.Entry<String, Float> entry : dailyDistances.entrySet()) {
            Float distance = entry.getValue();
            Log.d("StatsFragment", "🔍 Date: " + entry.getKey() + ", Distance: " + distance + "m");
            
            if (distance > 0.01) { // Consider distances > 0.01 meters as valid
                Log.d("StatsFragment", "✅ Found valid distance: " + distance + "m");
                return false;
            }
        }
        
        Log.d("StatsFragment", "⚠️ All distances are zero or very small");
        return true;
    }private void updateTrashAnalytics() {
        LinearLayout trashItemsContainer = binding.llTrashItems;
        TextView emptyState = binding.tvTrashEmptyState;
        
        // Clear existing views
        trashItemsContainer.removeAllViews();
        
        Log.d("StatsFragment", "🗑️ Updating trash analytics - Total trash items: " + 
              (trashList != null ? trashList.size() : 0));
        
        if (trashList == null || trashList.isEmpty()) {
            // Show empty state
            emptyState.setVisibility(View.VISIBLE);
            trashItemsContainer.setVisibility(View.GONE);
            Log.d("StatsFragment", "📊 No trash data found - showing empty state");
            return;
        }
        
        // Hide empty state and show content
        emptyState.setVisibility(View.GONE);
        trashItemsContainer.setVisibility(View.VISIBLE);
        
        // Calculate trash statistics
        Map<String, Integer> trashTypeCount = new HashMap<>();
        
        for (TrashEntity trash : trashList) {
            String trashType = trash.getTrashType();
            if (trashType == null || trashType.isEmpty()) {
                trashType = "other";
            }
            
            trashTypeCount.put(trashType, trashTypeCount.getOrDefault(trashType, 0) + 1);
            Log.d("StatsFragment", "🗑️ Trash item: " + trashType + " (Record ID: " + trash.getRecordId() + ")");
        }
        
        Log.d("StatsFragment", "📊 Trash type summary: " + trashTypeCount.toString());
        
        // Create UI elements for each trash type
        for (Map.Entry<String, Integer> entry : trashTypeCount.entrySet()) {
            String trashType = entry.getKey();
            int count = entry.getValue();
            
            View trashItemView = createTrashTypeItem(trashType, count);
            trashItemsContainer.addView(trashItemView);
        }
        
        Log.d("StatsFragment", "✅ Trash analytics updated with " + trashTypeCount.size() + " types");
    }

    private View createTrashTypeItem(String trashType, int count) {
        LinearLayout itemLayout = new LinearLayout(requireContext());
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(0, 12, 0, 12);
        
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemLayout.setLayoutParams(layoutParams);
        
        // Create color indicator (small colored circle)
        CardView colorIndicator = new CardView(requireContext());
        LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(24, 24);
        colorParams.setMargins(0, 0, 24, 0);
        colorIndicator.setLayoutParams(colorParams);
        colorIndicator.setCardBackgroundColor(getColorForTrashType(trashType));
        colorIndicator.setRadius(12);
        colorIndicator.setCardElevation(0);
        
        // Create text layout
        LinearLayout textLayout = new LinearLayout(requireContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        textLayout.setLayoutParams(textParams);
        
        // Trash type name
        TextView typeText = new TextView(requireContext());
        typeText.setText(getDisplayNameForTrashType(trashType));
        typeText.setTextColor(getResources().getColor(R.color.text_primary, null));
        typeText.setTextSize(14);
        typeText.setTypeface(typeText.getTypeface(), android.graphics.Typeface.BOLD);
        
        // Count text
        TextView countText = new TextView(requireContext());
        countText.setText(count + " item" + (count > 1 ? "s" : ""));
        countText.setTextColor(getResources().getColor(R.color.text_secondary, null));
        countText.setTextSize(12);
        
        textLayout.addView(typeText);
        textLayout.addView(countText);
        
        itemLayout.addView(colorIndicator);
        itemLayout.addView(textLayout);
        
        return itemLayout;
    }

    private int getColorForTrashType(String trashType) {
        switch (trashType.toLowerCase()) {
            case "plastic": return Color.parseColor("#FF5722"); // Orange-red
            case "paper": return Color.parseColor("#8BC34A"); // Light green
            case "glass": return Color.parseColor("#2196F3"); // Blue
            case "metal": return Color.parseColor("#795548"); // Brown
            case "organic": return Color.parseColor("#4CAF50"); // Green
            case "electronic": return Color.parseColor("#9C27B0"); // Purple
            case "hazardous": return Color.parseColor("#F44336"); // Red
            case "cigarette_butt": return Color.parseColor("#607D8B"); // Blue-grey
            default: return Color.parseColor("#9E9E9E"); // Grey
        }
    }

    private String getDisplayNameForTrashType(String trashType) {
        switch (trashType.toLowerCase()) {
            case "plastic": return "Plastik";
            case "paper": return "Kertas";
            case "glass": return "Kaca";
            case "metal": return "Logam";
            case "organic": return "Organik";
            case "electronic": return "Elektronik";
            case "hazardous": return "Berbahaya";
            case "cigarette_butt": return "Puntung Rokok";
            case "other": return "Lainnya";
            default: return trashType.substring(0, 1).toUpperCase() + trashType.substring(1);
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
        
        // Stop Firebase listeners to prevent memory leaks
        if (dataManager != null) {
            dataManager.stopAllListeners();
        }
        
        if (binding != null) {
            binding = null;
        }
    }    private void loadDataWithFirebaseSync() {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        
        if (!authManager.isLoggedIn()) {
            // For guest users, just load local data
            loadData();
            binding.progressIndicator.setVisibility(View.GONE);
            return;
        }
        
        // ENHANCED FIX: Load local data first for immediate display, then sync with Firebase
        Log.d("StatsFragment", "🔥 Loading local data first, then syncing with Firebase for logged-in user");
        
        // Load local data immediately for better UX
        loadLocalDataForImediateDisplay();
        
        // Then sync with Firebase in background
        dataManager.forceRefreshUserDataAfterLogin(new FirebaseDataManager.DataSyncCallback() {
            @Override
            public void onSuccess() {
                Log.d("StatsFragment", "✅ Fresh Firebase data loaded, setting up real-time sync");
                setupRealTimeStatsSync();
            }
            
            @Override
            public void onError(String error) {
                Log.w("StatsFragment", "⚠️ Failed to load fresh Firebase data: " + error);
                // Continue with local data if Firebase fails
                Log.w("StatsFragment", "Continuing with local data due to Firebase error");
                binding.progressIndicator.setVisibility(View.GONE);
            }
        });
    }    /**
     * Set up real-time statistics synchronization
     * ENHANCED: Merge Firebase data with local data for complete picture
     */
    private void setupRealTimeStatsSync() {
        Log.d("StatsFragment", "🔥 Setting up real-time Firebase stats sync with local data merge");
          // Subscribe to real-time stats updates
        dataManager.subscribeToUserStats(new FirebaseDataManager.StatsDataCallback() {
            @Override
            public void onStatsLoaded(UserStats stats) {
                // Check if fragment is still attached to activity
                if (!isAdded() || getActivity() == null) {
                    Log.d("StatsFragment", "⚠️ Fragment not attached, skipping stats update");
                    return;
                }
                
                requireActivity().runOnUiThread(() -> {
                    // Double check in case fragment was detached during the runOnUiThread call
                    if (!isAdded() || getActivity() == null) {
                        Log.d("StatsFragment", "⚠️ Fragment detached during UI thread execution, skipping stats update");
                        return;
                    }
                    
                    Log.d("StatsFragment", "🔥 Real-time Firebase stats received: " + stats.toString());
                    Log.d("StatsFragment", "📊 Firebase Data - Points: " + stats.getTotalPoints() + 
                          ", Distance: " + stats.getTotalDistance() + ", Sessions: " + stats.getTotalSessions());
                    
                    // ENHANCED: Update UI with Firebase data and refresh local data for charts
                    updateUIWithFirebaseStats(stats);
                    
                    // Refresh local data for charts and trash analytics (important for recent sessions)
                    refreshLocalDataForCharts();
                    
                    binding.progressIndicator.setVisibility(View.GONE);
                    
                    Log.d("StatsFragment", "✅ UI updated with Firebase stats and local charts");
                });
            }
              @Override
            public void onError(String error) {
                // Check if fragment is still attached to activity
                if (!isAdded() || getActivity() == null) {
                    Log.d("StatsFragment", "⚠️ Fragment not attached, skipping error handling");
                    return;
                }
                
                requireActivity().runOnUiThread(() -> {
                    // Double check in case fragment was detached during the runOnUiThread call
                    if (!isAdded() || getActivity() == null) {
                        Log.d("StatsFragment", "⚠️ Fragment detached during UI thread execution, skipping error handling");
                        return;
                    }
                    
                    Log.w("StatsFragment", "❌ Firebase stats error: " + error);
                    Log.w("StatsFragment", "Continuing with local data display");
                    
                    // Keep using local data that was already loaded
                    binding.progressIndicator.setVisibility(View.GONE);
                });
            }
        });
        
        // Sync local data to Firebase (background operation)
        dataManager.syncAllUserData(new FirebaseDataManager.DataSyncCallback() {
            @Override
            public void onSuccess() {
                Log.d("StatsFragment", "✅ Local data synced to Firebase successfully");
            }
            
            @Override
            public void onError(String error) {
                Log.w("StatsFragment", "⚠️ Firebase sync error: " + error);
            }
        });
    }    private void updateUIWithFirebaseStats(UserStats stats) {
        Log.d("StatsFragment", "🔄 Updating UI with Firebase stats data...");
        Log.d("StatsFragment", "📊 Updating - Points: " + stats.getTotalPoints() + 
              ", Distance: " + stats.getTotalDistance() + ", Sessions: " + stats.getTotalSessions());
        
        // Update main statistics with Firebase data
        binding.tvTotalPoints.setText(String.valueOf(stats.getTotalPoints()));
        binding.tvTotalDistance.setText(stats.getTotalDistance() > 0 ? 
            String.format(Locale.getDefault(), "%.2f", stats.getTotalDistance() / 1000) : "0,00");
        binding.tvTotalRuns.setText(String.valueOf(stats.getTotalSessions()));
        
        // Calculate and display achievements/badges count based on Firebase points
        int badgeCount = calculateBadgeCount(stats.getTotalPoints());
        binding.tvAchievements.setText(String.valueOf(badgeCount));
        
        // Update average time calculation with Firebase data
        if (stats.getTotalSessions() > 0 && stats.getTotalDuration() > 0) {
            long avgDuration = stats.getTotalDuration() / stats.getTotalSessions();
            binding.tvAverageTime.setText(formatDuration(avgDuration / 1000)); // Convert to seconds
        } else {
            binding.tvAverageTime.setText("0 menit");
        }
        
        // Update progress bars and other UI elements based on Firebase data
        updateProgressBars(stats);
        
        Log.d("StatsFragment", "✅ UI successfully updated with Firebase data");
        Log.d("StatsFragment", "📱 UI Display - Points: " + binding.tvTotalPoints.getText() + 
              ", Distance: " + binding.tvTotalDistance.getText() + ", Sessions: " + binding.tvTotalRuns.getText());
    }
    
    private void updateProgressBars(UserStats stats) {
        // Update distance progress (example: target 100km per month)
        float targetDistance = 100000; // 100km in meters
        float progressDistance = Math.min((float)stats.getTotalDistance() / targetDistance * 100, 100);
        
        // Update points progress (example: target 10000 points)
        int targetPoints = 10000;
        float progressPoints = Math.min((float)stats.getTotalPoints() / targetPoints * 100, 100);
        
        // Update sessions progress (example: target 50 sessions)
        int targetSessions = 50;
        float progressSessions = Math.min((float)stats.getTotalSessions() / targetSessions * 100, 100);
        
        // Update UI with progress values
        // Note: You may need to add progress bars to your layout if they don't exist
        Log.d("StatsFragment", "Progress - Distance: " + progressDistance + "%, Points: " + 
              progressPoints + "%, Sessions: " + progressSessions + "%");
    }
      /**
     * Clear local display data to prevent showing cached/stale data
     * ENHANCED: Show loading placeholders instead of clearing everything
     */
    private void clearLocalDisplayData() {
        Log.d("StatsFragment", "🧹 Setting loading placeholders while Firebase data loads");
        
        // Show loading placeholders instead of clearing everything
        binding.tvTotalPoints.setText("...");
        binding.tvTotalDistance.setText("...");
        binding.tvTotalRuns.setText("...");
        binding.tvAchievements.setText("...");
        binding.tvAverageTime.setText("...");
        
        // Don't clear recordList and trashList to preserve chart data
        // Charts will be refreshed when new data arrives
        
        Log.d("StatsFragment", "🧹 Loading placeholders set, preserving chart data");
    }
    
    /**
     * Load local data immediately for better user experience
     * This ensures users see their latest plogging data right after session completion
     */
    private void loadLocalDataForImediateDisplay() {
        Log.d("StatsFragment", "📊 Loading local data for immediate display");
        
        if (userId != -1) {
            // Load user data
            db.userDao().getUserById(userId).observe(getViewLifecycleOwner(), userEntity -> {
                if (userEntity != null) {
                    user = userEntity;
                    Log.d("StatsFragment", "✅ Local user data loaded: " + user.getPoints() + " points");
                }
            });
              
            // Load record data
            db.recordDao().getRecordsByUserId(userId).observe(getViewLifecycleOwner(), records -> {
                if (records != null && !records.isEmpty()) {
                    recordList = records;
                    Log.d("StatsFragment", "✅ Local records loaded: " + records.size() + " sessions");
                    updateUserStats();
                    loadTrashData(); // Load trash data after records are loaded
                    dataLoaded = true;
                } else {
                    recordList = new ArrayList<>();
                    updateUserStats();
                    loadTrashData(); // Still load trash data even if no records
                }
            });        
        } else {
            updateUserStats();
            loadTrashData(); // Still load trash data
        }
    }
    
    /**
     * Refresh local data specifically for charts and analytics
     * This ensures charts show the most recent local data including just-completed sessions
     */
    private void refreshLocalDataForCharts() {
        Log.d("StatsFragment", "🔄 Refreshing local data for charts and analytics");
          if (userId != -1) {
            executor.execute(() -> {
                try {
                    // Get latest records for chart calculations
                    List<RecordEntity> latestRecords = db.recordDao().getRecordsByUserIdSync(userId);
                    List<TrashEntity> latestTrash = db.trashDao().getTrashByUserIdSync(userId);
                    
                    Log.d("StatsFragment", "📊 Latest local data: " + latestRecords.size() + 
                          " records, " + latestTrash.size() + " trash items");
                    
                    // Check if fragment is still attached to activity
                    if (!isAdded() || getActivity() == null) {
                        Log.d("StatsFragment", "⚠️ Fragment not attached, skipping local data refresh UI update");
                        return;
                    }
                    
                    requireActivity().runOnUiThread(() -> {
                        // Double check in case fragment was detached during the runOnUiThread call
                        if (!isAdded() || getActivity() == null) {
                            Log.d("StatsFragment", "⚠️ Fragment detached during UI thread execution, skipping local data refresh");
                            return;
                        }
                        
                        // Update local lists for chart calculations
                        recordList = latestRecords != null ? latestRecords : new ArrayList<>();
                        trashList = latestTrash != null ? latestTrash : new ArrayList<>();
                        
                        // Refresh charts and analytics with latest local data
                        setupWeeklyDistanceChart();
                        updateTrashAnalytics();
                        
                        Log.d("StatsFragment", "✅ Charts refreshed with latest local data");
                    });
                } catch (Exception e) {
                    Log.e("StatsFragment", "Error refreshing local data for charts", e);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // ENHANCED: Always refresh data when fragment resumes
        // This is crucial for showing data after plogging session completion
        Log.d("StatsFragment", "📱 Fragment resumed - refreshing data");
        
        if (!NetworkUtil.isNetworkAvailable(requireContext())) {
            showNetworkError();
            return;
        }
        
        if (!authManager.isLoggedIn()) {
            // For guest users, reload local data
            loadData();
        } else {
            // For logged-in users, refresh both local and Firebase data
            loadDataWithFirebaseSync();
        }
    }
    
    /**
     * Force refresh all statistics data
     * Call this method when returning from plogging session to ensure fresh data
     */
    public void forceRefreshStats() {
        Log.d("StatsFragment", "🔄 Force refresh stats triggered");
        
        if (!isAdded() || binding == null) {
            Log.w("StatsFragment", "⚠️ Fragment not ready for refresh");
            return;
        }
        
        binding.progressIndicator.setVisibility(View.VISIBLE);
        
        if (!authManager.isLoggedIn()) {
            // For guest users, reload local data
            loadData();
        } else {
            // For logged-in users, force refresh both local and Firebase data
            loadDataWithFirebaseSync();
        }
    }
}
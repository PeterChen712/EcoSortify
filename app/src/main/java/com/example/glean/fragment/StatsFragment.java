package com.example.glean.fragment;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.databinding.FragmentStatsBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.UserEntity;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
    private UserEntity user;    private List<RecordEntity> recordList = new ArrayList<>();
    private boolean dataLoaded = false;
    
    // Time filter variables
    private int currentTimeFilter = 7; // Default to 7 days

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
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set click listeners
        binding.btnViewHistory.setOnClickListener(v -> navigateToHistory());
        binding.btnViewChallenges.setOnClickListener(v -> navigateToChallenges());
        
        // Setup time filter chips
        setupTimeFilterChips();
        
        // Initialize charts
        setupCharts();
          // Load data
        loadData();
    }      private void setupTimeFilterChips() {
        // Set default selection to 7 days
        binding.chip7days.setChecked(true);
        currentTimeFilter = 7;
        
        // Set click listeners for chips
        binding.chip7days.setOnClickListener(v -> {
            if (!binding.chip7days.isChecked()) {
                binding.chip7days.setChecked(true);
            }
            currentTimeFilter = 7;
            if (dataLoaded) {
                updateCharts();
            }
        });
        
        binding.chip30days.setOnClickListener(v -> {
            if (!binding.chip30days.isChecked()) {
                binding.chip30days.setChecked(true);
            }
            currentTimeFilter = 30;
            if (dataLoaded) {
                updateCharts();
            }
        });
        
        binding.chip3months.setOnClickListener(v -> {
            if (!binding.chip3months.isChecked()) {
                binding.chip3months.setChecked(true);
            }
            currentTimeFilter = 90; // 3 months = 90 days
            if (dataLoaded) {
                updateCharts();
            }
        });
    }
    
    private List<RecordEntity> getFilteredRecords() {
        if (recordList == null || recordList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calculate the date threshold
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -currentTimeFilter);
        long thresholdTime = calendar.getTimeInMillis();
        
        // Filter records based on the time threshold
        List<RecordEntity> filteredRecords = new ArrayList<>();
        for (RecordEntity record : recordList) {
            if (record.getCreatedAt() >= thresholdTime) {
                filteredRecords.add(record);
            }
        }
        
        return filteredRecords;
    }      private void setupCharts() {
        // Set up bar chart
        binding.barChartDistance.setDrawGridBackground(false);
        binding.barChartDistance.setDrawBarShadow(false);
        binding.barChartDistance.setDrawValueAboveBar(true);
        binding.barChartDistance.setPinchZoom(false);
        binding.barChartDistance.setDrawGridBackground(false);
        
        Description description = new Description();
        description.setText("");
        binding.barChartDistance.setDescription(description);
        
        XAxis xAxis = binding.barChartDistance.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
    }private void loadData() {
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
                    updateCharts();
                    showDataViews();
                    dataLoaded = true;
                } else {
                    recordList = new ArrayList<>();
                    updateUserStats();
                    showNoDataState();
                }
            });
        } else {
            binding.progressIndicator.setVisibility(View.GONE);
            showNoDataState();
        }
    }      private void showDataViews() {
        // Show distance chart card
        binding.cardDistanceChart.setVisibility(View.VISIBLE);
    }
    
    private void showNoDataState() {
        // Hide distance chart card
        binding.cardDistanceChart.setVisibility(View.GONE);
    }
    
    private void updateUserStats() {
        // Calculate statistics from recordList
        int totalRuns = recordList.size();
        int totalPlogs = recordList.size(); // In a plogging app, runs = plogs
        int totalPoints = user != null ? user.getPoints() : 0;
        
        // Calculate total distance
        float totalDistance = 0;
        long totalDuration = 0;
        
        for (RecordEntity record : recordList) {
            totalDistance += record.getDistance();
            totalDuration += record.getDuration();
        }        // Update UI
        binding.tvTotalRuns.setText(String.valueOf(totalRuns));
        binding.tvTotalPlogs.setText(String.valueOf(totalPlogs));
        binding.tvTotalPoints.setText(String.valueOf(totalPoints));
        binding.tvTotalDistance.setText(String.format(Locale.getDefault(), "%.1f", totalDistance / 1000));
          // Calculate average run time
        if (!recordList.isEmpty()) {
            long avgDuration = totalDuration / recordList.size();
            binding.tvAverageTime.setText(formatDuration(avgDuration / 1000)); // Convert to seconds
        } else {
            binding.tvAverageTime.setText("0");
        }
    }      private void updateCharts() {
        if (recordList.isEmpty()) {
            showNoDataState();
            return;
        }
        
        showDataViews();
        updateDistanceBarChart();
    }
      private void updateDistanceBarChart() {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        // Get filtered records based on current time filter
        List<RecordEntity> filteredRecords = getFilteredRecords();
        
        if (filteredRecords.isEmpty()) {
            // Show empty state or clear chart
            if (binding.barChartDistance != null) {
                binding.barChartDistance.clear();
                binding.barChartDistance.setNoDataText("No data available for selected period");
                binding.barChartDistance.invalidate();
            }
            return;
        }
        
        // Group records by date and sum distances
        Map<String, Float> dailyDistances = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (RecordEntity record : filteredRecords) {
            String dateKey = dateFormat.format(new Date(record.getCreatedAt()));
            float distanceKm = record.getDistance() / 1000f;
            dailyDistances.put(dateKey, dailyDistances.getOrDefault(dateKey, 0f) + distanceKm);
        }
        
        // Sort dates and create entries
        List<String> sortedDates = new ArrayList<>(dailyDistances.keySet());
        Collections.sort(sortedDates);
        
        // Limit to show reasonable number of bars based on filter
        int maxBars = currentTimeFilter <= 7 ? 7 : (currentTimeFilter <= 30 ? 15 : 20);
        int startIndex = Math.max(0, sortedDates.size() - maxBars);
        
        for (int i = startIndex; i < sortedDates.size(); i++) {
            String date = sortedDates.get(i);
            Float distance = dailyDistances.get(date);
            entries.add(new BarEntry(i - startIndex, distance != null ? distance : 0f));
            
            // Format date for label
            try {
                Date parsedDate = dateFormat.parse(date);
                SimpleDateFormat displayFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
                labels.add(displayFormat.format(parsedDate));
            } catch (ParseException e) {
                labels.add(date.substring(5)); // fallback to MM-dd
            }
        }
        
        if (entries.isEmpty()) {
            // Show empty state
            if (binding.barChartDistance != null) {
                binding.barChartDistance.clear();
                binding.barChartDistance.setNoDataText("No data available for selected period");
                binding.barChartDistance.invalidate();
            }
            return;
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Distance (km)");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.environmental_green));
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        dataSet.setValueTextSize(12f);
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        
        // Configure chart if it exists
        if (binding.barChartDistance != null) {
            binding.barChartDistance.setData(barData);
            binding.barChartDistance.getDescription().setEnabled(false);
            binding.barChartDistance.setFitBars(true);
            
            // Update X-axis labels
            XAxis xAxis = binding.barChartDistance.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setLabelCount(Math.min(labels.size(), 10));
            
            binding.barChartDistance.animateY(1000);
            binding.barChartDistance.invalidate();        }
    }
    
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d hr %d min", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%d min", minutes);
        }
    }
    
    private void navigateToHistory() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_statsFragment_to_historyFragment);
    }
      private void navigateToChallenges() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_statsFragment_to_challengeFragment);
    }
      private void navigateToMainFragment() {
        NavController navController = Navigation.findNavController(requireView());
        // Navigate back to home (main) fragment using bottom navigation
        navController.navigate(R.id.homeFragment);
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
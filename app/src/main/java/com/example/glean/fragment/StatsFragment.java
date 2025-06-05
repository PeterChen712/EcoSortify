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
        
        // Set click listeners
        binding.btnViewHistory.setOnClickListener(v -> navigateToHistory());
        binding.btnViewChallenges.setOnClickListener(v -> navigateToChallenges());
        
        // Initialize charts
        setupCharts();
        
        // Load data
        loadData();
    }
    
    private void setupCharts() {
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
        
        // Set up line chart
        binding.lineChartProgress.setDrawGridBackground(false);
        binding.lineChartProgress.setDrawBorders(false);
        binding.lineChartProgress.setDescription(description);
        binding.lineChartProgress.setTouchEnabled(true);
        binding.lineChartProgress.setPinchZoom(true);
        
        // Set up pie chart
        binding.pieChartTrashTypes.setUsePercentValues(true);
        binding.pieChartTrashTypes.setDescription(description);
        binding.pieChartTrashTypes.setHoleRadius(40f);
        binding.pieChartTrashTypes.setTransparentCircleRadius(45f);
    }
    
    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        if (userId != -1) {
            // Load user data
            db.userDao().getUserById(userId).observe(getViewLifecycleOwner(), userEntity -> {
                if (userEntity != null) {
                    user = userEntity;
                }
            });
            
            // Load record data
            db.recordDao().getRecordsByUserId(userId).observe(getViewLifecycleOwner(), records -> {
                binding.progressBar.setVisibility(View.GONE);
                
                if (records != null) {
                    recordList = records;
                    updateUserStats();
                    updateCharts();
                    dataLoaded = true;
                } else {
                    recordList = new ArrayList<>();
                    updateUserStats();
                    updateCharts();
                }
            });
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvNoData.setVisibility(View.VISIBLE);
        }
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
        }
        
        // Update UI
        binding.tvTotalRuns.setText(String.valueOf(totalRuns));
        binding.tvTotalPlogs.setText(String.valueOf(totalPlogs));
        binding.tvTotalPoints.setText(String.valueOf(totalPoints));
        binding.tvTotalDistance.setText(String.format(Locale.getDefault(), "%.1f km", totalDistance / 1000));
        
        // Calculate average run time
        if (!recordList.isEmpty()) {
            long avgDuration = totalDuration / recordList.size();
            binding.tvAverageTime.setText(formatDuration(avgDuration / 1000)); // Convert to seconds
        } else {
            binding.tvAverageTime.setText("0 min");
        }
    }
    
    private void updateCharts() {
        if (recordList.isEmpty()) {
            binding.pieChartTrashTypes.setVisibility(View.GONE);
            return;
        }
        
        binding.tvNoData.setVisibility(View.GONE);
        binding.barChartDistance.setVisibility(View.VISIBLE);
        binding.lineChartProgress.setVisibility(View.VISIBLE);
        binding.pieChartTrashTypes.setVisibility(View.VISIBLE);
        
        updateDistanceBarChart();
        updateProgressLineChart();
        updateTrashTypePieChart();
    }
    
    private void updateDistanceBarChart() {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        // Get last 7 records or fewer if not enough
        int recordCount = Math.min(recordList.size(), 7);
        
        for (int i = 0; i < recordCount; i++) {
            RecordEntity record = recordList.get(recordList.size() - recordCount + i);
            // Fixed: Use getDistance() and convert to km
            float distanceKm = record.getDistance() / 1000f;
            entries.add(new BarEntry(i, distanceKm));
            
            // Format date for label - use createdAt since getStartTime() doesn't exist
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
            labels.add(dateFormat.format(new Date(record.getCreatedAt())));
        }
        
        if (entries.isEmpty()) {
            // Show empty state
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
            binding.barChartDistance.animateY(1000);
            binding.barChartDistance.invalidate();
        }
    }
    
    private void updateProgressLineChart() {
        if (binding.lineChartProgress == null) return;
        
        try {
            // Clear any existing data
            binding.lineChartProgress.clear();
            
            if (recordList == null || recordList.isEmpty()) {
                binding.lineChartProgress.setNoDataText("No progress data available");
                binding.lineChartProgress.invalidate();
                return;
            }
            
            // Prepare data entries for the line chart
            ArrayList<Entry> entries = new ArrayList<>();
            
            // Group records by date and calculate cumulative progress
            Map<String, Integer> dailyTrashCount = new HashMap<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            for (RecordEntity record : recordList) {
                // Fixed: Use getCreatedAt() instead of getStartTime()
                String dateKey = dateFormat.format(new Date(record.getCreatedAt()));
                dailyTrashCount.put(dateKey, dailyTrashCount.getOrDefault(dateKey, 0) + record.getPoints() / 10); // Convert points back to trash count
            }
            
            // Sort dates and create cumulative entries
            List<String> sortedDates = new ArrayList<>(dailyTrashCount.keySet());
            Collections.sort(sortedDates);
            
            int cumulativeCount = 0;
            for (int i = 0; i < sortedDates.size(); i++) {
                cumulativeCount += dailyTrashCount.get(sortedDates.get(i));
                entries.add(new Entry(i, cumulativeCount));
            }
            
            if (entries.isEmpty()) {
                binding.lineChartProgress.setNoDataText("No progress data available");
                binding.lineChartProgress.invalidate();
                return;
            }
            
            // Create dataset
            LineDataSet dataSet = new LineDataSet(entries, "Cumulative Trash Collected");
            dataSet.setColor(Color.GREEN);
            dataSet.setCircleColor(Color.GREEN);
            dataSet.setLineWidth(3f);
            dataSet.setCircleRadius(5f);
            dataSet.setValueTextSize(10f);
            dataSet.setValueTextColor(Color.BLACK);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.GREEN);
            dataSet.setFillAlpha(30);
            
            // Create LineData and set to chart
            LineData lineData = new LineData(dataSet);
            binding.lineChartProgress.setData(lineData);
            
            // Customize chart appearance
            binding.lineChartProgress.getDescription().setEnabled(false);
            binding.lineChartProgress.setTouchEnabled(true);
            binding.lineChartProgress.setDragEnabled(true);
            binding.lineChartProgress.setScaleEnabled(true);
            binding.lineChartProgress.setPinchZoom(true);
            
            // Customize X-axis
            XAxis xAxis = binding.lineChartProgress.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    if (index >= 0 && index < sortedDates.size()) {
                        try {
                            Date date = dateFormat.parse(sortedDates.get(index));
                            SimpleDateFormat displayFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
                            return displayFormat.format(date);
                        } catch (ParseException e) {
                            return "";
                        }
                    }
                    return "";
                }
            });
            
            // Customize Y-axis
            YAxis leftAxis = binding.lineChartProgress.getAxisLeft();
            leftAxis.setAxisMinimum(0f);
            leftAxis.setGranularity(1f);
            
            YAxis rightAxis = binding.lineChartProgress.getAxisRight();
            rightAxis.setEnabled(false);
            
            // Refresh chart
            binding.lineChartProgress.animateX(1000);
            binding.lineChartProgress.invalidate();
            
        } catch (Exception e) {
            if (binding.lineChartProgress != null) {
                binding.lineChartProgress.setNoDataText("Error loading progress data");
                binding.lineChartProgress.invalidate();
            }
        }
    }
    
    private void updateTrashTypePieChart() {
        // For now, use sample data since we need to implement trash type tracking
        Map<String, Integer> trashCounts = new HashMap<>();
        
        // Calculate total trash collected from points (assuming 10 points per trash item)
        int totalTrash = 0;
        for (RecordEntity record : recordList) {
            totalTrash += record.getPoints() / 10; // Convert points back to trash count
        }
        
        if (totalTrash > 0) {
            // Distribute trash types proportionally (sample distribution)
            trashCounts.put("Plastic", (int)(totalTrash * 0.4));
            trashCounts.put("Paper", (int)(totalTrash * 0.25));
            trashCounts.put("Metal", (int)(totalTrash * 0.15));
            trashCounts.put("Glass", (int)(totalTrash * 0.10));
            trashCounts.put("Other", (int)(totalTrash * 0.10));
        }
        
        updatePieChartData(trashCounts);
    }
    
    private void updatePieChartData(Map<String, Integer> trashCounts) {
        List<PieEntry> entries = new ArrayList<>();
        
        // Add entries for each trash type
        for (Map.Entry<String, Integer> entry : trashCounts.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }
        
        // If no trash data, show placeholders
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No Data"));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "Trash Types");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });
        
        PieData pieData = new PieData(dataSet);
        
        binding.pieChartTrashTypes.setData(pieData);
        binding.pieChartTrashTypes.animateY(1000);
        binding.pieChartTrashTypes.invalidate();
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
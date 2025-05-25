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
            totalDistance += record.getTotalDistance();
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
            // Show empty state
            binding.tvNoData.setVisibility(View.VISIBLE);
            binding.barChartDistance.setVisibility(View.GONE);
            binding.lineChartProgress.setVisibility(View.GONE);
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
        List<RecordEntity> recentRecords = recordList.subList(Math.max(0, recordList.size() - recordCount), recordList.size());
        
        // Sort by date
        Collections.sort(recentRecords, (r1, r2) -> r1.getDate().compareTo(r2.getDate()));
        
        for (int i = 0; i < recentRecords.size(); i++) {
            RecordEntity record = recentRecords.get(i);
            entries.add(new BarEntry(i, record.getTotalDistance() / 1000)); // Convert to km
            
            // Format date for label
            String dateStr = record.getDate();
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
                Date date = inputFormat.parse(dateStr);
                dateStr = outputFormat.format(date);
            } catch (ParseException e) {
                // Use as is
            }
            
            labels.add(dateStr);
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Distance (km)");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        
        binding.barChartDistance.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChartDistance.setData(barData);
        binding.barChartDistance.animateY(1000);
        binding.barChartDistance.invalidate();
    }
    
    private void updateProgressLineChart() {
        // Calculate points earned from each record (simplified approach)
        Map<String, Integer> monthlyPoints = new HashMap<>();
        List<String> monthLabels = new ArrayList<>();
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Initialize last 6 months with 0
        for (int i = 5; i >= 0; i--) {
            Calendar tempCal = (Calendar) cal.clone();
            tempCal.add(Calendar.MONTH, -i);
            String monthKey = String.format(Locale.getDefault(), "%d-%02d", 
                    tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH) + 1);
            String monthLabel = monthFormat.format(tempCal.getTime());
            monthlyPoints.put(monthKey, 0);
            monthLabels.add(monthLabel);
        }
        
        // Sum points by month (estimate 10 points per trash collected)
        for (RecordEntity record : recordList) {
            try {
                Date recordDate = inputFormat.parse(record.getDate());
                Calendar tempCal = Calendar.getInstance();
                tempCal.setTime(recordDate);
                String monthKey = String.format(Locale.getDefault(), "%d-%02d", 
                        tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH) + 1);
                
                if (monthlyPoints.containsKey(monthKey)) {
                    int estimatedPoints = record.getTrashCount() * 10; // 10 points per trash
                    monthlyPoints.put(monthKey, monthlyPoints.get(monthKey) + estimatedPoints);
                }
            } catch (ParseException e) {
                // Skip this record
            }
        }
        
        // Create entries
        List<Entry> entries = new ArrayList<>();
        Calendar tempCal = (Calendar) cal.clone();
        
        for (int i = 0; i < 6; i++) {
            tempCal.add(Calendar.MONTH, -(5-i));
            String monthKey = String.format(Locale.getDefault(), "%d-%02d", 
                    tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH) + 1);
            tempCal = (Calendar) cal.clone(); // Reset calendar
            
            int points = monthlyPoints.getOrDefault(monthKey, 0);
            entries.add(new Entry(i, points));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Monthly Points");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.BLUE);
        dataSet.setFillAlpha(50);
        
        LineData lineData = new LineData(dataSet);
        
        binding.lineChartProgress.getXAxis().setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        binding.lineChartProgress.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.lineChartProgress.getXAxis().setGranularity(1f);
        binding.lineChartProgress.getXAxis().setDrawGridLines(false);
        binding.lineChartProgress.getAxisRight().setEnabled(false);
        binding.lineChartProgress.setData(lineData);
        binding.lineChartProgress.animateX(1000);
        binding.lineChartProgress.invalidate();
    }
    
    private void updateTrashTypePieChart() {
        // For now, use sample data since we need to implement trash type tracking
        Map<String, Integer> trashCounts = new HashMap<>();
        
        // Calculate total trash collected
        int totalTrash = 0;
        for (RecordEntity record : recordList) {
            totalTrash += record.getTrashCount();
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
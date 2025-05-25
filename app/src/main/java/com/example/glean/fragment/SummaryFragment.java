package com.example.glean.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.databinding.FragmentSummaryBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SummaryFragment extends Fragment {

    private FragmentSummaryBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private int recordId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        // Get record ID from arguments
        if (getArguments() != null) {
            recordId = getArguments().getInt("RECORD_ID", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnShare.setOnClickListener(v -> shareActivity());
        
        // Load activity data
        loadActivityData();
    }

    private void loadActivityData() {
        if (recordId != -1) {
            executor.execute(() -> {
                // Load record data synchronously
                RecordEntity record = db.recordDao().getRecordByIdSync(recordId);
                
                requireActivity().runOnUiThread(() -> {
                    if (record != null) {
                        displayActivityData(record);
                    }
                });
            });
        }
    }

    private void displayActivityData(RecordEntity record) {
        // Set date
        binding.tvDate.setText(record.getDate());
        
        // Set location - use notes as location placeholder since getLocation() doesn't exist
        String location = record.getNotes() != null && !record.getNotes().isEmpty() 
            ? record.getNotes() 
            : "Plogging Session";
        binding.tvLocation.setText(location);
        
        // Format start time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String startTime = sdf.format(new Date(record.getStartTime()));
        binding.tvStartTime.setText(startTime);
        
        // Format distance - use getTotalDistance() instead of getDistance()
        float distanceKm = record.getTotalDistance() / 1000f;
        binding.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));
        
        // Format duration - convert milliseconds to seconds and use getDuration()
        long durationSeconds = record.getDuration() / 1000;
        int hours = (int) (durationSeconds / 3600);
        int minutes = (int) ((durationSeconds % 3600) / 60);
        int seconds = (int) (durationSeconds % 60);
        String durationStr;
        if (hours > 0) {
            durationStr = String.format(Locale.getDefault(), "%d hr %d min %d sec", hours, minutes, seconds);
        } else {
            durationStr = String.format(Locale.getDefault(), "%d min %d sec", minutes, seconds);
        }
        binding.tvDuration.setText(durationStr);
        
        // Set points - calculate from trash count since getPlogPoints() doesn't exist
        int points = record.getTrashCount() * 10; // 10 points per trash item
        binding.tvPoints.setText(String.valueOf(points));
        
        // Set trash collected - use getTrashCount() instead of getTrashCollected()
        binding.tvTrashCollected.setText(String.valueOf(record.getTrashCount()));
        
        // Calculate average pace - use available fields
        if (record.getTotalDistance() > 0 && record.getDuration() > 0) {
            float paceMinPerKm = (record.getDuration() / 60000f) / (record.getTotalDistance() / 1000f);
            int paceMin = (int) paceMinPerKm;
            int paceSec = (int) ((paceMinPerKm - paceMin) * 60);
            binding.tvPace.setText(String.format(Locale.getDefault(), "%d:%02d min/km", paceMin, paceSec));
        } else {
            binding.tvPace.setText("N/A");
        }
    }

    private void shareActivity() {
        if (recordId != -1) {
            executor.execute(() -> {
                RecordEntity record = db.recordDao().getRecordByIdSync(recordId);
                
                requireActivity().runOnUiThread(() -> {
                    if (record != null) {
                        // Create share text
                        String shareText = String.format(Locale.getDefault(),
                            "I just completed a plogging session!\n\n" +
                            "📅 Date: %s\n" +
                            "🏃 Distance: %.2f km\n" +
                            "⏱️ Duration: %s\n" +
                            "🗑️ Trash collected: %d items\n" +
                            "🏆 Points earned: %d\n\n" +
                            "Join me in making our environment cleaner! #Plogging #CleanEnvironment",
                            record.getDate(),
                            record.getTotalDistance() / 1000f,
                            formatDuration(record.getDuration()),
                            record.getTrashCount(),
                            record.getTrashCount() * 10
                        );
                        
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Plogging Achievement");
                        
                        startActivity(Intent.createChooser(shareIntent, "Share your plogging achievement"));
                    }
                });
            });
        }
    }
    
    private String formatDuration(long durationMillis) {
        long durationSeconds = durationMillis / 1000;
        int hours = (int) (durationSeconds / 3600);
        int minutes = (int) ((durationSeconds % 3600) / 60);
        int seconds = (int) (durationSeconds % 60);
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d hr %d min %d sec", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d min %d sec", minutes, seconds);
        }
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
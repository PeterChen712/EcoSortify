package com.example.glean.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.databinding.FragmentSummaryBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.TrashEntity;
import com.example.glean.model.UserEntity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SummaryFragment extends Fragment {

    private FragmentSummaryBinding binding;    private AppDatabase db;
    private ExecutorService executor;
    private int recordId;
    private RecordEntity currentRecord;
    private Location lastKnownLocation;
    private int currentUserId;    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();// Get current user ID from SharedPreferences with fallback pattern
        SharedPreferences prefs = requireActivity().getSharedPreferences("USER_PREFS", 0);
        currentUserId = prefs.getInt("USER_ID", -1);
        android.util.Log.d("SummaryFragment", "USER_PREFS - currentUserId: " + currentUserId);
        
        // Fallback to default preferences if not found
        if (currentUserId == -1) {
            SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            currentUserId = defaultPrefs.getInt("USER_ID", -1);
            android.util.Log.d("SummaryFragment", "DefaultPrefs - currentUserId: " + currentUserId);
        }
        
        // Additional fallback to user_prefs pattern used by other fragments
        if (currentUserId == -1) {
            SharedPreferences userPrefs = requireContext().getSharedPreferences("user_prefs", requireContext().MODE_PRIVATE);
            currentUserId = userPrefs.getInt("current_user_id", -1);
            android.util.Log.d("SummaryFragment", "user_prefs - currentUserId: " + currentUserId);
        }
        
        android.util.Log.d("SummaryFragment", "Final currentUserId: " + currentUserId);
        
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
        
        // Add share to community button
        binding.btnShareCommunity.setOnClickListener(v -> shareToCommunitiy());
    }

    private void loadActivityData() {
        if (recordId != -1) {
            executor.execute(() -> {
                // Load record data synchronously
                RecordEntity record = db.recordDao().getRecordByIdSync(recordId);
                
                requireActivity().runOnUiThread(() -> {
                    if (record != null) {
                        currentRecord = record; // Store the record for later use
                        displayActivityData(record);
                    }
                });
            });
        }
    }

    private void displayActivityData(RecordEntity record) {
        // Set date - use createdAt instead of getDate()
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date(record.getCreatedAt()));
        binding.tvDate.setText(formattedDate);
        
        // Set location - use description as location placeholder since getLocation() doesn't exist
        String location = record.getDescription() != null && !record.getDescription().isEmpty() 
            ? record.getDescription() 
            : "Plogging Session";
        binding.tvLocation.setText(location);
        
        // Format start time - use createdAt instead of getStartTime()
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String startTime = sdf.format(new Date(record.getCreatedAt()));
        binding.tvStartTime.setText(startTime);
        
        // Format distance - use getDistance() instead of getTotalDistance()
        float distanceKm = record.getDistance() / 1000f;
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
        
        // Set points - use getPoints() directly instead of calculating from trash count
        binding.tvPoints.setText(String.valueOf(record.getPoints()));
        
        // Set trash collected - calculate from points since getTrashCount() doesn't exist
        int trashCount = record.getPoints() / 10; // Assuming 10 points per trash item
        binding.tvTrashCollected.setText(String.valueOf(trashCount));
        
        // Calculate average pace - use available fields
        if (record.getDistance() > 0 && record.getDuration() > 0) {
            float paceMinPerKm = (record.getDuration() / 60000f) / (record.getDistance() / 1000f);
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
                        // Create share text - use correct field names
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        String formattedDate = dateFormat.format(new Date(record.getCreatedAt()));
                        int trashCount = record.getPoints() / 10; // Calculate trash count from points
                        
                        String shareText = String.format(Locale.getDefault(),
                            "I just completed a plogging session!\n\n" +
                            "📅 Date: %s\n" +
                            "🏃 Distance: %.2f km\n" +
                            "⏱️ Duration: %s\n" +
                            "🗑️ Trash collected: %d items\n" +
                            "🏆 Points earned: %d\n\n" +
                            "Join me in making our environment cleaner! #Plogging #CleanEnvironment",
                            formattedDate,
                            record.getDistance() / 1000f,
                            formatDuration(record.getDuration()),
                            trashCount,
                            record.getPoints()
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
    }    private void shareToCommunitiy() {
        // Simple success message instead of complex sharing
        Toast.makeText(requireContext(), "Plogging session completed! Great job! 🎉", 
                      Toast.LENGTH_LONG).show();
    }
}
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
import com.example.glean.model.PostEntity;
import com.example.glean.model.UserEntity;
import com.example.glean.repository.CommunityRepository;
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

    private FragmentSummaryBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private int recordId;
      private CommunityRepository repository;
    private PostEntity pendingPost;
    private RecordEntity currentRecord;
    private Location lastKnownLocation;
    private int currentUserId;    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        repository = new CommunityRepository(requireContext());
        executor = Executors.newSingleThreadExecutor();        // Get current user ID from SharedPreferences with fallback pattern
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
        if (currentUserId == -1) {
            showLoginPrompt();
            return;
        }

        if (currentRecord == null) {
            Toast.makeText(requireContext(), "No record data available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user data and create community post from plogging data
        executor.execute(() -> {
            UserEntity user = db.userDao().getUserByIdSync(currentUserId);
            if (user == null) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            requireActivity().runOnUiThread(() -> {
                String content = createPloggingPostContent(currentRecord);
                
                PostEntity post = new PostEntity();
                post.setUserId(currentUserId);
                post.setUsername(user.getUsername() != null ? user.getUsername() : "Anonymous");
                post.setContent(content);
                post.setTimestamp(System.currentTimeMillis());
                post.setUserAvatar(user.getProfileImagePath());
                
                // Show sharing dialog
                showSharingDialog(post);
            });
        });
    }    private void showSharingDialog(PostEntity post) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_share_community, null);
        
        EditText etContent = dialogView.findViewById(R.id.etContent);
        ImageView ivRouteMap = dialogView.findViewById(R.id.ivRouteMap);
        
        // Don't auto-populate content - let user write their own caption
        etContent.setText("");
        etContent.setHint("Tulis caption untuk share ke komunitas...");
        
        // Use placeholder image for route map (since this is SummaryFragment, not PloggingSummaryFragment)
        ivRouteMap.setImageResource(R.drawable.ic_map);
        
        builder.setView(dialogView)
                .setTitle("📤 Share ke Komunitas")
                .setPositiveButton("Share", (dialog, which) -> {
                    post.setContent(etContent.getText().toString().trim());
                    
                    // Always include location (simplified approach)
                    if (lastKnownLocation != null) {
                        post.setLatitude(lastKnownLocation.getLatitude());
                        post.setLongitude(lastKnownLocation.getLongitude());
                        post.setLocation("Location");
                    }
                    
                    // Share without photo selection (simplified)
                    sharePostToLocal(post, false);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void sharePostToLocal(PostEntity post, boolean includePhoto) {
        // Comment out until progressBar and btnShareCommunity are added to layout
        // binding.progressBar.setVisibility(View.VISIBLE);
        // binding.btnShareCommunity.setEnabled(false);
        
        if (includePhoto && currentRecord != null) {
            // Find best photo from trash collection
            executor.execute(() -> {
                // Use synchronous method instead of LiveData
                List<TrashEntity> trashItems = db.trashDao().getTrashByRecordIdSync(currentRecord.getId());
                TrashEntity photoTrash = null;
                
                for (TrashEntity trash : trashItems) {
                    if (trash.getPhotoPath() != null && !trash.getPhotoPath().isEmpty()) {
                        photoTrash = trash;
                        break;
                    }
                }
                
                if (photoTrash != null) {
                    String photoPath = photoTrash.getPhotoPath();
                    requireActivity().runOnUiThread(() -> {
                        uploadPhotoAndShare(post, photoPath);
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        sharePostWithoutPhoto(post);
                    });
                }
            });
        } else {
            sharePostWithoutPhoto(post);
        }
    }    private void uploadPhotoAndShare(PostEntity post, String photoPath) {
        // For local database, we store the local photo path directly
        post.setImageUrl(photoPath);
        sharePostWithoutPhoto(post);
    }

    private void sharePostWithoutPhoto(PostEntity post) {
        executor.execute(() -> {
            try {                repository.insertPost(post, insertedPost -> {
                    requireActivity().runOnUiThread(() -> {
                    // Comment out until UI elements are added to layout
                    // binding.progressBar.setVisibility(View.GONE);
                    // binding.btnShareCommunity.setEnabled(true);
                    
                    Toast.makeText(requireContext(), "Shared to community successfully! 🎉", 
                                   Toast.LENGTH_LONG).show();                        // Show success animation or update UI
                        // binding.btnShareCommunity.setText("✓ Shared");
                        // binding.btnShareCommunity.setBackgroundTintList(
                        //         ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.success)));
                    });
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    // Comment out until UI elements are added to layout
                    // binding.progressBar.setVisibility(View.GONE);
                    // binding.btnShareCommunity.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed to share: " + e.getMessage(), 
                                   Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String createPloggingPostContent(RecordEntity record) {
        float distanceKm = record.getDistance() / 1000f;
        int durationMin = (int) (record.getDuration() / 60000);
        int trashCount = record.getPoints() / 10; // Calculate trash count from points
        
        return String.format(Locale.getDefault(),
                "🏃‍♂️ Just completed an amazing plogging session!\n\n" +
                "📊 Stats:\n" +
                "• Distance: %.2f km\n" +
                "• Duration: %d minutes\n" +
                "• Trash collected: %d items\n" +
                "• Points earned: %d\n\n" +
                "Every small action makes a big difference! 🌱\n" +
                "#GleanGo #Plogging #MakeTheWorldClean",
                distanceKm, durationMin, trashCount, record.getPoints());
    }

    private void showLoginPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Join Community")
                .setMessage("Sign in to share your achievements with the GleanGo community!")
                .setPositiveButton("Sign In", (dialog, which) -> {
                    // Navigate to community fragment which will show login
                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigate(R.id.communityFeedFragment);
                })
                .setNegativeButton("Later", null)
                .show();
    }
}
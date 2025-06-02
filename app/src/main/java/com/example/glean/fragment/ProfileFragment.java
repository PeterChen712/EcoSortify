package com.example.glean.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.adapter.BadgeAdapter;
import com.example.glean.databinding.FragmentProfileBinding;
import com.example.glean.databinding.DialogEditProfileBinding;
import com.example.glean.databinding.DialogSettingsBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.UserEntity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private FragmentProfileBinding binding;
    
    private AppDatabase db;
    private int userId;
    private UserEntity currentUser;
    private ExecutorService executor;
    private Uri selectedImageUri;
    private String profileImagePath;

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
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup RecyclerView
        binding.rvBadges.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        
        // Set click listeners
        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        binding.btnSettings.setOnClickListener(v -> showSettingsDialog());
        binding.btnLogout.setOnClickListener(v -> logout());
        binding.ivProfilePic.setOnClickListener(v -> selectImage());
        binding.btnCustomize.setOnClickListener(v -> navigateToProfileDecor());
        
        // Load user data
        loadUserData();
    }
    
    private void loadUserData() {
        if (userId != -1) {
            db.userDao().getUserById(userId).observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    currentUser = user;
                    
                    // Set user info using available fields
                    String displayName = getDisplayName(user);
                    binding.tvName.setText(displayName);
                    binding.tvEmail.setText(user.getEmail());
                    
                    // Format creation date
                    String memberSince = formatMemberSince(user.getCreatedAt());
                    binding.tvMemberSince.setText("Member since: " + memberSince);
                    
                    binding.tvTotalPoints.setText(String.valueOf(user.getPoints()));
                    
                    // Load user statistics from database
                    loadUserStatistics();
                    
                    // Load profile image if exists using Glide with circleCrop
                    profileImagePath = user.getProfileImagePath();
                    if (profileImagePath != null && !profileImagePath.isEmpty()) {
                        Glide.with(this)
                                .load(new File(profileImagePath))
                                .placeholder(R.drawable.profile_placeholder)
                                .error(R.drawable.profile_placeholder)
                                .circleCrop()
                                .into(binding.ivProfilePic);
                    } else {
                        // Set default placeholder with circular crop
                        Glide.with(this)
                                .load(R.drawable.profile_placeholder)
                                .circleCrop()
                                .into(binding.ivProfilePic);
                    }
                    
                    // Setup badges - use empty list for now since badges field doesn't exist
                    setupBadges("");
                    
                    // Update UI with user data
                    updateUI();
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
    
    private String formatMemberSince(long createdAt) {
        if (createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            return sdf.format(new Date(createdAt));
        }
        return "Unknown";
    }
    
    private void loadUserStatistics() {
        if (userId != -1) {
            executor.execute(() -> {
                // Get user statistics from database
                int totalRecords = db.recordDao().getRecordCountByUserId(userId);
                float totalDistance = db.recordDao().getTotalDistanceByUserId(userId);
                long totalDuration = db.recordDao().getTotalDurationByUserId(userId);
                
                // Calculate derived statistics
                int totalRuns = totalRecords; // Assuming each record is a run/plog
                int totalPlogs = totalRecords; // Same as runs for plogging app
                
                requireActivity().runOnUiThread(() -> {
                    binding.tvTotalRuns.setText(String.valueOf(totalRuns));
                    binding.tvTotalPlogs.setText(String.valueOf(totalPlogs));
                });
            });
        }
    }
    
    private void setupBadges(String badgesString) {
        List<String> badges = new ArrayList<>();
        
        if (badgesString != null && !badgesString.isEmpty()) {
            badges = Arrays.asList(badgesString.split(","));
        }
        
        // Add some default badges based on user statistics
        if (currentUser != null && currentUser.getPoints() > 0) {
            if (currentUser.getPoints() >= 1000) {
                badges.add("Expert Plogger");
            }
            if (currentUser.getPoints() >= 500) {
                badges.add("Eco Warrior");
            }
            if (currentUser.getPoints() >= 100) {
                badges.add("Green Helper");
            }
        }
        
        BadgeAdapter adapter = new BadgeAdapter(badges);
        binding.rvBadges.setAdapter(adapter);
    }
    
    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == requireActivity().RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            
            // Display the selected image using Glide with circleCrop
            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .circleCrop()
                    .into(binding.ivProfilePic);
            
            // Save the image path to the user profile
            saveProfileImage();
        }
    }
    
    private void saveProfileImage() {
        if (selectedImageUri != null && currentUser != null) {
            try {
                // Get the file path from the URI
                String imagePath = selectedImageUri.toString();
                
                // Update user entity
                currentUser.setProfileImagePath(imagePath);
                
                // Save to database
                executor.execute(() -> {
                    db.userDao().update(currentUser);
                    
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Failed to save profile picture", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void showEditProfileDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        
        // Set current values
        if (currentUser != null) {
            String currentName = getDisplayName(currentUser);
            dialogBinding.etName.setText(currentName);
        }
        
        // Set click listeners
        dialogBinding.btnSave.setOnClickListener(v -> {
            String name = dialogBinding.etName.getText().toString().trim();
            
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (currentUser != null) {
                // Try to split name into first and last name
                String[] nameParts = name.split(" ", 2);
                currentUser.setFirstName(nameParts[0]);
                if (nameParts.length > 1) {
                    currentUser.setLastName(nameParts[1]);
                } else {
                    currentUser.setLastName("");
                }
                
                executor.execute(() -> {
                    db.userDao().update(currentUser);
                    
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });
            }
        });
        
        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void showSettingsDialog() {
        DialogSettingsBinding dialogBinding = DialogSettingsBinding.inflate(LayoutInflater.from(requireContext()));
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setView(dialogBinding.getRoot());
        
        // Get current settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean notificationsEnabled = prefs.getBoolean("NOTIFICATIONS_ENABLED", true);
        boolean isDarkMode = prefs.getBoolean("DARK_MODE", false);
        
        // Set current values
        dialogBinding.switchNotifications.setChecked(notificationsEnabled);
        dialogBinding.switchDarkMode.setChecked(isDarkMode);
        
        // Create dialog
        android.app.AlertDialog dialog = builder.create();
        
        // Set listeners
        dialogBinding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("NOTIFICATIONS_ENABLED", isChecked).apply();
        });
        
        dialogBinding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference but don't apply immediately
            prefs.edit().putBoolean("DARK_MODE", isChecked).apply();
        });
        
        dialogBinding.btnSave.setOnClickListener(v -> {
            dialog.dismiss();
            
            // Apply theme if it was changed
            boolean newDarkMode = prefs.getBoolean("DARK_MODE", false);
            if (newDarkMode != isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(
                    newDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                requireActivity().recreate();
            }
        });
        
        dialogBinding.btnCancel.setOnClickListener(v -> {
            // Revert any changes
            prefs.edit().putBoolean("NOTIFICATIONS_ENABLED", notificationsEnabled).apply();
            prefs.edit().putBoolean("DARK_MODE", isDarkMode).apply();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void logout() {
        // Clear user ID from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit().remove("USER_ID").apply();
        
        // Navigate to login screen
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_profileFragment_to_loginFragment);
        
        // Close current activity
        requireActivity().finish();
    }

    private void toggleTheme() {
        // Get the current theme setting
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isDarkMode = prefs.getBoolean("DARK_MODE", false);
        
        // Toggle the theme
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            prefs.edit().putBoolean("DARK_MODE", false).apply();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            prefs.edit().putBoolean("DARK_MODE", true).apply();
        }
        
        // Recreate activity to apply theme
        requireActivity().recreate();
    }

    private void navigateToProfileDecor() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_profileFragment_to_profileDecorFragment);
    }

    private void updateUI() {
        if (currentUser != null) {
            String displayName = getDisplayName(currentUser);
            binding.tvName.setText(displayName);
            binding.tvEmail.setText(currentUser.getEmail());
            binding.tvTotalPoints.setText(String.valueOf(currentUser.getPoints()));
            
            // Set profile decoration if available using activeDecoration field
            String activeDecoration = currentUser.getActiveDecoration();
            if (activeDecoration != null && !activeDecoration.isEmpty() && !activeDecoration.equals("none")) {
                switch (activeDecoration) {
                    case "1": // Gold frame
                        binding.ivProfileFrame.setImageResource(R.drawable.decor_frame_gold);
                        binding.ivProfileFrame.setVisibility(View.VISIBLE);
                        break;
                    case "2": // Silver frame
                        binding.ivProfileFrame.setImageResource(R.drawable.decor_frame_silver);
                        binding.ivProfileFrame.setVisibility(View.VISIBLE);
                        break;
                    case "3": // Bronze frame
                        binding.ivProfileFrame.setImageResource(R.drawable.decor_frame_bronze);
                        binding.ivProfileFrame.setVisibility(View.VISIBLE);
                        break;
                    default:
                        binding.ivProfileFrame.setVisibility(View.GONE);
                        break;
                }
            } else {
                binding.ivProfileFrame.setVisibility(View.GONE);
            }
            
            // Update badge list
            updateBadges();
        }
    }

    private void updateBadges() {
        if (currentUser != null) {
            List<String> badges = new ArrayList<>();
            
            // Generate badges based on user achievements
            if (currentUser.getPoints() >= 1000) {
                badges.add("Expert Plogger");
            }
            if (currentUser.getPoints() >= 500) {
                badges.add("Eco Warrior");
            }
            if (currentUser.getPoints() >= 100) {
                badges.add("Green Helper");
            }
            if (currentUser.getPoints() >= 50) {
                badges.add("Beginner");
            }
            
            BadgeAdapter adapter = new BadgeAdapter(badges);
            binding.rvBadges.setAdapter(adapter);
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
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
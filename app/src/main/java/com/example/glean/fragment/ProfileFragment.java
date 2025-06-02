package com.example.glean.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.glean.R;
import com.example.glean.adapter.BadgeAdapter;
import com.example.glean.databinding.FragmentProfileBinding;
import com.example.glean.databinding.DialogEditProfileBinding;
import com.example.glean.databinding.DialogSettingsBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.UserEntity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

    private static final String TAG = "ProfileFragment";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int REQUEST_STORAGE_PERMISSION = 1002;

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
        
        // Get user ID from SharedPreferences with better error handling
        SharedPreferences prefs = requireActivity().getSharedPreferences("USER_PREFS", 0);
        userId = prefs.getInt("USER_ID", -1);
        
        // Fallback to default preferences if not found
        if (userId == -1) {
            SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            userId = defaultPrefs.getInt("USER_ID", -1);
        }
        
        executor = Executors.newSingleThreadExecutor();
        
        Log.d(TAG, "ProfileFragment created with userId: " + userId);
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
        
        try {
            setupUI();
            loadUserData();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ProfileFragment", e);
            Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupUI() {
        // Setup RecyclerView for badges
        if (binding.rvBadges != null) {
            binding.rvBadges.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        }
        
        // Set click listeners with safe null checks
        setupClickListeners();
        
        // Add some visual feedback
        addVisualFeedback();
    }
    
    private void setupClickListeners() {
        // Edit Profile button
        if (binding.btnEditProfile != null) {
            binding.btnEditProfile.setOnClickListener(v -> {
                Log.d(TAG, "Edit profile button clicked");
                showEditProfileDialog();
            });
        }
        
        // Settings button
        if (binding.btnSettings != null) {
            binding.btnSettings.setOnClickListener(v -> {
                Log.d(TAG, "Settings button clicked");
                showSettingsDialog();
            });
        }
        
        // Logout button
        if (binding.btnLogout != null) {
            binding.btnLogout.setOnClickListener(v -> {
                Log.d(TAG, "Logout button clicked");
                showLogoutConfirmation();
            });
        }
        
        // Profile picture click
        if (binding.ivProfilePic != null) {
            binding.ivProfilePic.setOnClickListener(v -> {
                Log.d(TAG, "Profile picture clicked");
                checkPermissionsAndSelectImage();
            });
        }
        
        // Customize button - FIXED with enhanced navigation
        if (binding.btnCustomize != null) {
            binding.btnCustomize.setOnClickListener(v -> {
                Log.d(TAG, "Customize button clicked");
                navigateToProfileDecor();
            });
        } else {
            Log.w(TAG, "Customize button not found in layout");
        }
        
        // Theme toggle (if exists)
        setupThemeToggle();
    }
    
    private void addVisualFeedback() {
        // Add ripple effect and elevation feedback
        View[] clickableViews = {
            binding.btnEditProfile,
            binding.btnSettings,
            binding.btnLogout,
            binding.btnCustomize,
            binding.ivProfilePic
        };
        
        for (View view : clickableViews) {
            if (view != null) {
                view.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            v.setAlpha(0.8f);
                            v.setScaleX(0.98f);
                            v.setScaleY(0.98f);
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            v.animate()
                                .alpha(1.0f)
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(150)
                                .start();
                            break;
                    }
                    return false; // Don't consume the event
                });
            }
        }
    }
    
    private void setupThemeToggle() {
        // Try to find theme toggle switch
        View themeToggle = findViewById("switchTheme", "btnTheme", "toggleTheme");
        if (themeToggle != null) {
            themeToggle.setOnClickListener(v -> toggleTheme());
        }
    }
    
    private View findViewById(String... possibleIds) {
        for (String id : possibleIds) {
            try {
                int resId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
                if (resId != 0) {
                    View view = binding.getRoot().findViewById(resId);
                    if (view != null) {
                        return view;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "View not found with id: " + id);
            }
        }
        return null;
    }
    
    private void loadUserData() {
        if (userId == -1) {
            Log.e(TAG, "Invalid user ID, cannot load user data");
            Toast.makeText(requireContext(), "User not found. Please login again.", Toast.LENGTH_LONG).show();
            logout();
            return;
        }
        
        Log.d(TAG, "Loading user data for userId: " + userId);
        
        db.userDao().getUserById(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                Log.d(TAG, "User data loaded successfully: " + user.getUsername());
                currentUser = user;
                updateUIWithUserData(user);
            } else {
                Log.e(TAG, "User not found in database for userId: " + userId);
                Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateUIWithUserData(UserEntity user) {
        try {
            // Set user info
            String displayName = getDisplayName(user);
            if (binding.tvName != null) {
                binding.tvName.setText(displayName);
            }
            
            if (binding.tvEmail != null) {
                binding.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
            }
            
            // Format and display member since date
            String memberSince = formatMemberSince(user.getCreatedAt());
            if (binding.tvMemberSince != null) {
                binding.tvMemberSince.setText("Member since: " + memberSince);
            }
            
            // Display points
            if (binding.tvTotalPoints != null) {
                binding.tvTotalPoints.setText(String.valueOf(user.getPoints()));
            }
            
            // Load profile image
            loadProfileImage(user);
            
            // Load user statistics
            loadUserStatistics();
            
            // Setup badges
            setupBadges(user);
            
            // Update profile decorations
            updateProfileDecorations(user);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with user data", e);
        }
    }
    
    private void loadProfileImage(UserEntity user) {
        profileImagePath = user.getProfileImagePath();
        
        if (binding.ivProfilePic != null) {
            if (profileImagePath != null && !profileImagePath.isEmpty()) {
                // Load from file path
                File imageFile = new File(profileImagePath);
                if (imageFile.exists()) {
                    Glide.with(this)
                            .load(imageFile)
                            .placeholder(android.R.drawable.ic_menu_camera)
                            .error(android.R.drawable.ic_menu_camera)
                            .circleCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.ivProfilePic);
                } else {
                    // File doesn't exist, load default
                    loadDefaultProfileImage();
                }
            } else {
                // No profile image set, load default
                loadDefaultProfileImage();
            }
        }
    }
    
    private void loadDefaultProfileImage() {
        if (binding.ivProfilePic != null) {
            Glide.with(this)
                    .load(android.R.drawable.ic_menu_camera)
                    .circleCrop()
                    .into(binding.ivProfilePic);
        }
    }
    
    private String getDisplayName(UserEntity user) {
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            String fullName = user.getFirstName();
            if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                fullName += " " + user.getLastName();
            }
            return fullName;
        } else if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            return user.getUsername();
        } else if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            return user.getEmail().split("@")[0];
        }
        return "User";
    }
    
    private String formatMemberSince(long createdAt) {
        if (createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            return sdf.format(new Date(createdAt));
        }
        return "Recently";
    }
    
    private void loadUserStatistics() {
        if (userId == -1) return;
        
        executor.execute(() -> {
            try {
                // Get user statistics from database
                int totalRecords = db.recordDao().getRecordCountByUserId(userId);
                float totalDistance = db.recordDao().getTotalDistanceByUserId(userId);
                long totalDuration = db.recordDao().getTotalDurationByUserId(userId);
                
                requireActivity().runOnUiThread(() -> {
                    // Update UI with safe null checks
                    if (binding.tvTotalRuns != null) {
                        binding.tvTotalRuns.setText(String.valueOf(totalRecords));
                    }
                    if (binding.tvTotalPlogs != null) {
                        binding.tvTotalPlogs.setText(String.valueOf(totalRecords));
                    }
                    // Fixed: Now properly referencing the correct TextView ID
                    if (binding.tvTotalDistance != null) {
                        binding.tvTotalDistance.setText(String.format(Locale.getDefault(), "%.1f", totalDistance / 1000f));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading user statistics", e);
                requireActivity().runOnUiThread(() -> {
                    // Set default values on error
                    if (binding.tvTotalRuns != null) {
                        binding.tvTotalRuns.setText("0");
                    }
                    if (binding.tvTotalPlogs != null) {
                        binding.tvTotalPlogs.setText("0");
                    }
                    if (binding.tvTotalDistance != null) {
                        binding.tvTotalDistance.setText("0.0");
                    }
                });
            }
        });
    }
    
    private void setupBadges(UserEntity user) {
        List<String> badges = new ArrayList<>();
        
        // Generate badges based on user achievements
        int points = user.getPoints();
        if (points >= 2000) {
            badges.add("🏆 Legend");
        }
        if (points >= 1000) {
            badges.add("🥇 Expert Plogger");
        }
        if (points >= 500) {
            badges.add("🌍 Eco Warrior");
        }
        if (points >= 200) {
            badges.add("🏅 Green Champion");
        }
        if (points >= 100) {
            badges.add("🌱 Green Helper");
        }
        if (points >= 50) {
            badges.add("🌿 Beginner");
        }
        if (points >= 10) {
            badges.add("⭐ Starter");
        }
        
        // Add special badges
        if (points >= 1500) {
            badges.add("🧹 Master Cleaner");
        }
        
        // Ensure at least one badge
        if (badges.isEmpty()) {
            badges.add("🌟 New Member");
        }
        
        if (binding.rvBadges != null) {
            BadgeAdapter adapter = new BadgeAdapter(badges);
            binding.rvBadges.setAdapter(adapter);
        }
    }
    
    private void updateProfileDecorations(UserEntity user) {
        if (binding.ivProfileFrame == null) return;
        
        String activeDecoration = user.getActiveDecoration();
        if (activeDecoration != null && !activeDecoration.isEmpty() && !activeDecoration.equals("none")) {
            try {
                switch (activeDecoration) {
                    case "1": // Gold frame
                        binding.ivProfileFrame.setImageResource(android.R.drawable.star_big_on);
                        binding.ivProfileFrame.setVisibility(View.VISIBLE);
                        break;
                    case "2": // Silver frame
                        binding.ivProfileFrame.setImageResource(android.R.drawable.star_on);
                        binding.ivProfileFrame.setVisibility(View.VISIBLE);
                        break;
                    case "3": // Bronze frame
                        binding.ivProfileFrame.setImageResource(android.R.drawable.star_off);
                        binding.ivProfileFrame.setVisibility(View.VISIBLE);
                        break;
                    default:
                        binding.ivProfileFrame.setVisibility(View.GONE);
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile decoration", e);
                binding.ivProfileFrame.setVisibility(View.GONE);
            }
        } else {
            binding.ivProfileFrame.setVisibility(View.GONE);
        }
    }
    
    private void checkPermissionsAndSelectImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            selectImage();
        }
    }
    
    private void selectImage() {
        try {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Error selecting image", e);
            Toast.makeText(requireContext(), "Error opening image picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == requireActivity().RESULT_OK
                && data != null && data.getData() != null) {
            try {
                selectedImageUri = data.getData();
                
                // Display the selected image
                if (binding.ivProfilePic != null) {
                    Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(android.R.drawable.ic_menu_camera)
                            .error(android.R.drawable.ic_menu_camera)
                            .circleCrop()
                            .into(binding.ivProfilePic);
                }
                
                // Save the image
                saveProfileImage();
                
            } catch (Exception e) {
                Log.e(TAG, "Error handling image selection result", e);
                Toast.makeText(requireContext(), "Error loading selected image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void saveProfileImage() {
        if (selectedImageUri != null && currentUser != null) {
            try {
                String imagePath = selectedImageUri.toString();
                currentUser.setProfileImagePath(imagePath);
                
                executor.execute(() -> {
                    try {
                        db.userDao().update(currentUser);
                        
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving profile image to database", e);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Failed to save profile picture", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving profile image", e);
                Toast.makeText(requireContext(), "Failed to save profile picture", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void showEditProfileDialog() {
        try {
            BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
            DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(getLayoutInflater());
            dialog.setContentView(dialogBinding.getRoot());
            
            // Set current values
            if (currentUser != null) {
                String currentName = getDisplayName(currentUser);
                dialogBinding.etName.setText(currentName);
                
                if (currentUser.getEmail() != null) {
                    dialogBinding.etEmail.setText(currentUser.getEmail());
                }
            }
            
            // Set click listeners
            dialogBinding.btnSave.setOnClickListener(v -> {
                String name = dialogBinding.etName.getText().toString().trim();
                String email = dialogBinding.etEmail.getText().toString().trim();
                
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (currentUser != null) {
                    // Split name into first and last name
                    String[] nameParts = name.split(" ", 2);
                    currentUser.setFirstName(nameParts[0]);
                    if (nameParts.length > 1) {
                        currentUser.setLastName(nameParts[1]);
                    } else {
                        currentUser.setLastName("");
                    }
                    
                    if (!email.isEmpty()) {
                        currentUser.setEmail(email);
                    }
                    
                    executor.execute(() -> {
                        try {
                            db.userDao().update(currentUser);
                            
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating user profile", e);
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            });
            
            dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
            
            dialog.show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing edit profile dialog", e);
            Toast.makeText(requireContext(), "Error opening edit dialog", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showSettingsDialog() {
        try {
            DialogSettingsBinding dialogBinding = DialogSettingsBinding.inflate(LayoutInflater.from(requireContext()));
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setView(dialogBinding.getRoot());
            builder.setTitle("Settings");
            
            // Get current settings
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            boolean notificationsEnabled = prefs.getBoolean("NOTIFICATIONS_ENABLED", true);
            boolean isDarkMode = prefs.getBoolean("DARK_MODE", false);
            
            // Set current values
            dialogBinding.switchNotifications.setChecked(notificationsEnabled);
            dialogBinding.switchDarkMode.setChecked(isDarkMode);
            
            // Create dialog
            androidx.appcompat.app.AlertDialog dialog = builder.create();
            
            // Set listeners
            dialogBinding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("NOTIFICATIONS_ENABLED", isChecked).apply();
            });
            
            dialogBinding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
                
                Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
            });
            
            dialogBinding.btnCancel.setOnClickListener(v -> {
                // Revert any changes
                prefs.edit().putBoolean("NOTIFICATIONS_ENABLED", notificationsEnabled).apply();
                prefs.edit().putBoolean("DARK_MODE", isDarkMode).apply();
                dialog.dismiss();
            });
            
            dialog.show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing settings dialog", e);
            Toast.makeText(requireContext(), "Error opening settings", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void logout() {
        try {
            // Clear user data from SharedPreferences
            SharedPreferences prefs = requireActivity().getSharedPreferences("USER_PREFS", 0);
            prefs.edit().clear().apply();
            
            SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            defaultPrefs.edit().remove("USER_ID").apply();
            
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            // Navigate to login screen
            try {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigate(R.id.action_profileFragment_to_loginFragment);
                requireActivity().finish();
            } catch (Exception e) {
                Log.e(TAG, "Navigation error during logout", e);
                requireActivity().finish();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
            Toast.makeText(requireContext(), "Error during logout", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isDarkMode = prefs.getBoolean("DARK_MODE", false);
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            prefs.edit().putBoolean("DARK_MODE", false).apply();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            prefs.edit().putBoolean("DARK_MODE", true).apply();
        }
        
        requireActivity().recreate();
    }

    private void navigateToProfileDecor() {
        try {
            Log.d(TAG, "Navigating to ProfileDecorFragment...");
            
            // Check if user is valid before navigation
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Please wait for profile to load", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show loading feedback
            Toast.makeText(requireContext(), "Opening customization...", Toast.LENGTH_SHORT).show();
            
            NavController navController = Navigation.findNavController(requireView());
            
            // Create bundle with user data
            Bundle args = new Bundle();
            args.putInt("USER_ID", userId);
            args.putInt("USER_POINTS", currentUser.getPoints());
            
            // Navigate with enhanced error handling
            try {
                navController.navigate(R.id.action_profileFragment_to_profileDecorFragment, args);
                Log.d(TAG, "Navigation to ProfileDecorFragment successful");
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Navigation action not found, trying alternative", e);
                // Try alternative navigation ID
                try {
                    navController.navigate(R.id.profileDecorFragment, args);
                } catch (Exception e2) {
                    Log.e(TAG, "Alternative navigation failed", e2);
                    // Show temporary customization dialog as fallback
                    showTemporaryCustomizeDialog();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to profile customization", e);
            showTemporaryCustomizeDialog();
        }
    }
    
    private void showTemporaryCustomizeDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🎨 Profile Customization")
                .setMessage("Choose a profile decoration:")
                .setPositiveButton("🥇 Gold Frame", (dialog, which) -> {
                    if (currentUser != null) {
                        currentUser.setActiveDecoration("1");
                        updateProfileDecorations(currentUser);
                        saveUserDecoration();
                    }
                })
                .setNeutralButton("🥈 Silver Frame", (dialog, which) -> {
                    if (currentUser != null) {
                        currentUser.setActiveDecoration("2");
                        updateProfileDecorations(currentUser);
                        saveUserDecoration();
                    }
                })
                .setNegativeButton("🥉 Bronze Frame", (dialog, which) -> {
                    if (currentUser != null) {
                        currentUser.setActiveDecoration("3");
                        updateProfileDecorations(currentUser);
                        saveUserDecoration();
                    }
                })
                .show();
    }
    
    private void saveUserDecoration() {
        if (currentUser != null) {
            executor.execute(() -> {
                try {
                    db.userDao().update(currentUser);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Decoration applied!", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving decoration", e);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to save decoration", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                Toast.makeText(requireContext(), "Storage permission required to select image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data when returning to fragment
        if (userId != -1 && currentUser != null) {
            loadUserData();
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
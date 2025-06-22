package com.example.glean.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.glean.R;
import com.example.glean.activity.SkinSelectionActivity;
import com.example.glean.adapter.BadgeAdapter;
import com.example.glean.adapter.ProfileBadgeAdapter;
import com.example.glean.auth.AuthGuard;
import com.example.glean.auth.FirebaseAuthManager;
import com.example.glean.databinding.FragmentProfileBinding;
import com.example.glean.databinding.DialogEditProfileBinding;
import com.example.glean.databinding.DialogSettingsBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.Badge;
import com.example.glean.model.UserEntity;
import com.example.glean.util.NetworkUtil;
import com.example.glean.util.LocalProfileImageUtil;
import com.example.glean.util.ProfileImageLoader;
import com.example.glean.service.FirebaseDataManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.glean.model.UserStats;
import com.example.glean.model.RankingUser;
import com.example.glean.model.UserProfile;

public class ProfileFragment extends Fragment {    private static final String TAG = "ProfileFragment";    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int CAMERA_REQUEST = 1002;
    private static final int REQUEST_STORAGE_PERMISSION = 1003;
    private static final int REQUEST_CAMERA_PERMISSION = 1004;
    private static final int SKIN_SELECTION_REQUEST = 1005;
    private static final int UCROP_REQUEST = 1006;private FragmentProfileBinding binding;
      // Firebase components
    private FirebaseAuthManager authManager;
    private FirebaseFirestore firestore;
    private com.example.glean.service.FirebaseDataManager firebaseDataManager;
    
    private AppDatabase db;
    private int userId;
    private UserEntity currentUser;
    private ExecutorService executor;
    private Uri selectedImageUri;
    private Uri cameraImageUri;
    private String profileImagePath;@Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
          // Initialize Firebase
        authManager = FirebaseAuthManager.getInstance(requireContext());
        firestore = FirebaseFirestore.getInstance();
        firebaseDataManager = com.example.glean.service.FirebaseDataManager.getInstance(requireContext());
        
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
        Log.d(TAG, "Firebase user ID: " + (authManager.isLoggedIn() ? authManager.getUserId() : "Not logged in"));
    }@Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            binding = FragmentProfileBinding.inflate(inflater, container, false);
            Log.d(TAG, "Profile binding created successfully");
            return binding.getRoot();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create profile binding, using fallback", e);
            return createFallbackLayout(inflater, container);
        }
    }
    
    private View createFallbackLayout(LayoutInflater inflater, ViewGroup container) {
        // Create a simple fallback layout if the main layout fails
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        
        // Title
        TextView titleText = new TextView(requireContext());
        titleText.setText("Profil");
        titleText.setTextSize(24);
        titleText.setGravity(android.view.Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 24);
        layout.addView(titleText);
        
        // User info
        TextView nameText = new TextView(requireContext());
        nameText.setText(getString(R.string.loading));
        nameText.setTextSize(18);
        nameText.setGravity(android.view.Gravity.CENTER);
        layout.addView(nameText);
        
        TextView emailText = new TextView(requireContext());
        emailText.setText("Loading email...");
        emailText.setTextSize(14);
        emailText.setGravity(android.view.Gravity.CENTER);
        emailText.setPadding(0, 8, 0, 24);
        layout.addView(emailText);
        
        // Simple button for basic functionality
        Button editButton = new Button(requireContext());
        editButton.setText("Edit Profile");
        editButton.setOnClickListener(v -> showEditProfileDialog());
        layout.addView(editButton);
        
        scrollView.addView(layout);
        return scrollView;
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Check authentication first
        AuthGuard.checkFeatureAccess(requireContext(), "profile", new AuthGuard.AuthCheckCallback() {
            @Override
            public void onAuthenticationRequired() {
                // Redirect to login
                AuthGuard.navigateToLogin(ProfileFragment.this, "profile");
                return;
            }
            
            @Override
            public void onProceedWithFeature() {
                // User is authenticated, check network for profile sync
                if (NetworkUtil.isNetworkAvailable(requireContext())) {
                    // Network available, can sync profile data
                    setupAuthenticatedProfile();
                } else {
                    // No network, use local data only
                    setupOfflineProfile();
                }
            }
            
            @Override
            public void onNetworkRequired() {
                // Not applicable for profile main view
                setupOfflineProfile();
            }
        });
    }
    
    private void setupAuthenticatedProfile() {
        try {
            setupUI();
            loadUserData();
            updateProfileSkin(); // Apply current skin
        } catch (Exception e) {
            Log.e(TAG, "Error setting up authenticated ProfileFragment", e);
            Toast.makeText(requireContext(), getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupOfflineProfile() {
        try {
            setupUI();
            // Show limited functionality message
            Toast.makeText(requireContext(), "Profil terbatas tanpa koneksi internet.", Toast.LENGTH_SHORT).show();
            loadUserData(); // Load local data only
            updateProfileSkin();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up offline ProfileFragment", e);
            Toast.makeText(requireContext(), getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show();
        }
    }      private void setupUI() {
        // Setup RecyclerView for badges with same design as Customize Profile
        if (binding.rvBadges != null) {
            binding.rvBadges.setLayoutManager(new GridLayoutManager(requireContext(), 3)); // Match CustomizeProfile 3-column layout
        }
        
        // Set click listeners with safe null checks
        setupClickListeners();
        
        // Add some visual feedback
        addVisualFeedback();
        
        // Set initial button state (disabled until data loads)
        updateCustomizeButtonState();
    }
      private void setupClickListeners() {
        // Back button to navigate to Plogging
        if (binding.btnBack != null) {
            binding.btnBack.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked - navigating to Plogging");
                navigateBackToPlogging();
            });
        }
        
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
            });        }
          // Customize Profile button
        if (binding.btnCustomize != null) {
            binding.btnCustomize.setOnClickListener(v -> {
                Log.d(TAG, "Customize profile button clicked");
                openCustomizeProfileActivity();
            });
        }
        
        // Settings icon in header
        if (binding.btnSettings != null) {
            binding.btnSettings.setOnClickListener(v -> {
                Log.d(TAG, "Settings icon clicked");
                showSettingsDialog();
            });
        }        // Theme toggle (if exists)
        setupThemeToggle();
    }
      private void addVisualFeedback() {        // Add ripple effect and elevation feedback
    View[] clickableViews = {
        binding.btnEditProfile,
        binding.btnSettings,
        binding.btnLogout,
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
            }        }
    }
    
    private void setupThemeToggle() {
        // Try to find theme toggle switch
        View themeToggle = findViewById("switchTheme", "btnTheme", "toggleTheme");
        if (themeToggle != null) {
            themeToggle.setOnClickListener(v -> toggleTheme());
        }
    }

    private void toggleTheme() {
        // Get current theme
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        
        // Toggle between light and dark mode
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        
        // Save theme preference
        SharedPreferences prefs = requireActivity().getSharedPreferences("USER_PREFS", 0);
        prefs.edit().putInt("THEME_MODE", AppCompatDelegate.getDefaultNightMode()).apply();
    }
    
    private void navigateBackToPlogging() {
        // Navigate back to Plogging using NavController
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.ploggingTabsFragment);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating back to plogging", e);
            // Fallback: try to pop back stack
            try {
                requireActivity().onBackPressed();
            } catch (Exception fallbackError) {
                Log.e(TAG, "Fallback navigation also failed", fallbackError);
            }
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
    }    private void loadUserData() {
        // If user is logged in with Firebase, prioritize Firebase data
        if (authManager.isLoggedIn()) {
            Log.d(TAG, "🔥 Setting up real-time Firebase data synchronization");
            setupFirebaseRealTimeSync();
            // Only load local data as fallback after Firebase setup
        } else {
            Log.d(TAG, "User not logged in with Firebase, using local data only");
            // First load from local database for non-Firebase users
            loadUserDataFromLocal();
        }
    }
      /**
     * Set up real-time Firebase data synchronization
     */
    private void setupFirebaseRealTimeSync() {
        try {
            Log.d(TAG, "🔥 Setting up real-time Firebase synchronization");
              // ENHANCED: Force refresh data from Firebase for new user
            if (firebaseDataManager != null) {
                // First, force refresh to ensure we get fresh data
                firebaseDataManager.forceRefreshUserDataAfterLogin(new com.example.glean.service.FirebaseDataManager.DataSyncCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ Fresh data loaded successfully");
                        // Now set up real-time listeners
                        setupRealTimeListeners();
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "⚠️ Failed to load fresh data, continuing with real-time setup: " + error);
                        // Continue with real-time setup even if refresh fails
                        setupRealTimeListeners();
                    }
                });
            } else {
                // Fallback if dataManager is null
                setupRealTimeListeners();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Firebase real-time sync", e);
        }
    }
      /**
     * Set up real-time listeners after fresh data is loaded
     */
    private void setupRealTimeListeners() {
        try {
            // Subscribe to real-time user stats updates
            firebaseDataManager.subscribeToUserStats(new com.example.glean.service.FirebaseDataManager.StatsDataCallback() {
                @Override
                public void onStatsLoaded(UserStats stats) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.d(TAG, "🔥 Real-time stats update received - Points: " + stats.getTotalPoints());
                            updateUIWithFirebaseStats(stats);
                            Toast.makeText(requireContext(), "Data updated from server", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Firebase stats listener error: " + error);
                }
            });
            
            // Subscribe to real-time user profile updates
            firebaseDataManager.subscribeToUserProfile(new com.example.glean.service.FirebaseDataManager.ProfileDataCallback() {
                @Override
                public void onProfileLoaded(UserProfile profile) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.d(TAG, "🔥 Real-time profile update received");
                            updateUIWithFirebaseProfile(profile);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Firebase profile listener error: " + error);
                    // If profile listener fails, try to use Firebase Auth data as fallback
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateUIWithFirebaseAuthFallback();
                        });
                    }
                }
            });
            
            // Also immediately try to get Firebase Auth user data as initial fallback
            // This ensures we have some user data displayed even if Firestore profile is empty
            updateUIWithFirebaseAuthFallback();
            
            Log.d(TAG, "🔥 Real-time Firebase listeners activated");
              } catch (Exception e) {
            Log.e(TAG, "Error setting up Firebase real-time sync", e);
            // Only fall back to local data if Firebase completely fails
            Log.d(TAG, "Falling back to local data due to Firebase error");
            loadUserDataFromLocalAsFirebaseFallback();
        }
    }
    
    /**
     * Update UI with real-time Firebase stats data
     */
    private void updateUIWithFirebaseStats(UserStats stats) {
        try {
            if (binding != null) {
                // Update points display
                if (binding.tvTotalPoints != null) {
                    binding.tvTotalPoints.setText(String.valueOf(stats.getTotalPoints()));
                }
                
                // Update distance display
                if (binding.tvTotalDistance != null) {
                    binding.tvTotalDistance.setText(String.format(java.util.Locale.getDefault(), "%.1f", stats.getTotalDistance() / 1000f));
                }
                
                // Update sessions count
                if (binding.tvTotalPlogs != null) {
                    binding.tvTotalPlogs.setText(String.valueOf(stats.getTotalSessions()));
                }
                
                // Update local user entity if needed
                if (currentUser != null && currentUser.getPoints() != stats.getTotalPoints()) {
                    currentUser.setPoints(stats.getTotalPoints());
                    executor.execute(() -> {
                        try {
                            db.userDao().update(currentUser);
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating local user with Firebase stats", e);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with Firebase stats", e);
        }
    }
      /**
     * Update UI with real-time Firebase profile data
     */    private void updateUIWithFirebaseProfile(UserProfile profile) {
        try {
            if (binding != null) {
                Log.d(TAG, "🔥 Updating UI with Firebase profile data - Name: " + profile.getDisplayName());
                
                // Update name display with enhanced fallback logic
                String displayName = profile.getDisplayName();
                if (binding.tvName != null) {
                    binding.tvName.setText(displayName);
                    Log.d(TAG, "🔥 Firebase profile name set to: " + displayName);
                }
                  // Update email with fallback to Firebase Auth if not in profile
                String email = profile.getEmail();
                if ((email == null || email.isEmpty()) && authManager.isLoggedIn()) {
                    // Fallback to Firebase Auth user email
                    try {
                        String firebaseEmail = authManager.getFirebaseUserEmail();
                        if (firebaseEmail != null && !firebaseEmail.isEmpty()) {
                            email = firebaseEmail;
                            Log.d(TAG, "🔥 Using Firebase Auth email as fallback: " + email);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Could not get email from Firebase Auth", e);
                    }
                }
                if (binding.tvEmail != null) {
                    binding.tvEmail.setText(email != null && !email.isEmpty() ? email : "No email");
                    Log.d(TAG, "🔥 Firebase profile email set to: " + email);
                }
                  // Update profile image with enhanced fallback logic
                String avatarUrl = profile.getAvatarUrl();
                if ((avatarUrl == null || avatarUrl.isEmpty()) && authManager.isLoggedIn()) {
                    // Fallback to Firebase Auth user photo
                    try {
                        String firebasePhotoUrl = authManager.getUserPhotoUrl();
                        if (firebasePhotoUrl != null && !firebasePhotoUrl.isEmpty()) {
                            avatarUrl = firebasePhotoUrl;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Could not get photo URL from Firebase Auth", e);
                    }
                }
                
                // Create final versions for lambda
                final String finalEmail = email;
                final String finalAvatarUrl = avatarUrl;
                  // Update local user data if available - but don't let it override UI
                if (currentUser != null) {
                    // Update current user object with profile data
                    String firstName = profile.getFirstName();
                    String lastName = profile.getLastName();
                    
                    if (firstName != null && !firstName.isEmpty()) {
                        currentUser.setFirstName(firstName);
                    }
                    if (lastName != null && !lastName.isEmpty()) {
                        currentUser.setLastName(lastName);
                    }                    if (finalEmail != null && !finalEmail.isEmpty()) {
                        currentUser.setEmail(finalEmail);
                    }
                    if (finalAvatarUrl != null && !finalAvatarUrl.isEmpty()) {
                        currentUser.setProfileImagePath(finalAvatarUrl);
                    }
                    
                    // Load the updated profile image
                    loadProfileImage(currentUser);
                    
                    // Update local database in background without affecting UI
                    executor.execute(() -> {
                        try {
                            db.userDao().update(currentUser);
                            Log.d(TAG, "Local user updated with Firebase profile data");
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating local user with Firebase profile", e);
                        }
                    });
                } else {
                    // ENHANCED: Create currentUser for Firebase users when it's null
                    Log.d(TAG, "Creating currentUser from Firebase profile data");
                    UserEntity firebaseUser = new UserEntity();
                    firebaseUser.setId(userId);
                    firebaseUser.setFirstName(profile.getFirstName());
                    firebaseUser.setLastName(profile.getLastName());
                    firebaseUser.setUsername(displayName);                    firebaseUser.setEmail(finalEmail);
                    firebaseUser.setProfileImagePath(finalAvatarUrl);
                    firebaseUser.setCreatedAt(System.currentTimeMillis());
                    
                    // Set currentUser for immediate use
                    currentUser = firebaseUser;
                    
                    // Load the profile image
                    loadProfileImage(firebaseUser);
                    
                    // Try to save/update in local database in background
                    executor.execute(() -> {
                        try {
                            // Try to find existing user first
                            UserEntity existingUser = db.userDao().getUserByIdSync(userId);
                            if (existingUser != null) {
                                // Update existing user
                                existingUser.setFirstName(profile.getFirstName());
                                existingUser.setLastName(profile.getLastName());
                                existingUser.setUsername(displayName);                                existingUser.setEmail(finalEmail);
                                existingUser.setProfileImagePath(finalAvatarUrl);
                                db.userDao().update(existingUser);
                                Log.d(TAG, "Updated existing local user with Firebase profile data");
                                
                                // Update currentUser reference to the persistent entity
                                getActivity().runOnUiThread(() -> currentUser = existingUser);
                            } else {
                                // Insert new user
                                long newUserId = db.userDao().insert(firebaseUser);
                                Log.d(TAG, "Created new local user from Firebase profile with ID: " + newUserId);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving Firebase user to local database", e);
                            // Keep the temporary user object even if database save fails
                        }
                    });
                    
                    Log.d(TAG, "Created currentUser from Firebase profile: " + displayName);
                }
                
                Log.d(TAG, "UI updated with Firebase profile: " + displayName);
                
                // Update customize button state after Firebase profile is loaded
                updateCustomizeButtonState();
                
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with Firebase profile", e);
            // Try to fallback to Firebase Auth data directly
            updateUIWithFirebaseAuthFallback();
        }
    }
      /**
     * Fallback method to update UI with Firebase Auth user data when profile is not available
     */
    private void updateUIWithFirebaseAuthFallback() {
        try {
            if (!authManager.isLoggedIn()) {
                Log.d(TAG, "User not logged in, skipping Firebase Auth fallback");
                return;
            }
            
            Log.d(TAG, "🔄 Using Firebase Auth fallback for profile data");
              // Get data from Firebase Auth
            String firebaseName = authManager.getUserDisplayName();
            String firebaseEmail = authManager.getFirebaseUserEmail();
            String firebasePhotoUrl = authManager.getUserPhotoUrl();
            
            Log.d(TAG, "🔄 Firebase Auth data - Name: " + firebaseName + ", Email: " + firebaseEmail + ", Photo: " + (firebasePhotoUrl != null ? "Available" : "None"));
            
            if (binding != null) {
                // Update name
                if (binding.tvName != null) {
                    String displayName = firebaseName;
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = firebaseEmail != null ? firebaseEmail.split("@")[0] : "User";
                    }
                    binding.tvName.setText(displayName);
                    Log.d(TAG, "🔄 Updated name display to: " + displayName);
                }
                
                // Update email
                if (binding.tvEmail != null) {
                    String emailDisplay = firebaseEmail != null && !firebaseEmail.isEmpty() ? firebaseEmail : "No email";
                    binding.tvEmail.setText(emailDisplay);
                    Log.d(TAG, "🔄 Updated email display to: " + emailDisplay);
                }
                  // Load profile image if available
                if (firebasePhotoUrl != null && !firebasePhotoUrl.isEmpty()) {
                    Log.d(TAG, "🔄 Loading Firebase Auth profile image");
                    if (currentUser != null) {
                        currentUser.setProfileImagePath(firebasePhotoUrl);
                        loadProfileImage(currentUser);
                    } else {
                        // Create temporary user for image loading
                        UserEntity tempUser = new UserEntity();
                        tempUser.setProfileImagePath(firebasePhotoUrl);
                        loadProfileImage(tempUser);
                    }
                }
                
                // ENHANCED: Create currentUser from Firebase Auth data if it's null
                if (currentUser == null) {
                    Log.d(TAG, "🔄 Creating currentUser from Firebase Auth data");
                    UserEntity authUser = new UserEntity();
                    authUser.setId(userId);
                    
                    // Set name data
                    String displayName = firebaseName;
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = firebaseEmail != null ? firebaseEmail.split("@")[0] : "User";
                    }
                    
                    // Try to split first/last name
                    String[] nameParts = displayName.split(" ", 2);
                    authUser.setFirstName(nameParts[0]);
                    if (nameParts.length > 1) {
                        authUser.setLastName(nameParts[1]);
                    }
                    authUser.setUsername(displayName);
                    
                    // Set email
                    if (firebaseEmail != null && !firebaseEmail.isEmpty()) {
                        authUser.setEmail(firebaseEmail);
                    }
                    
                    // Set profile image
                    if (firebasePhotoUrl != null && !firebasePhotoUrl.isEmpty()) {
                        authUser.setProfileImagePath(firebasePhotoUrl);
                    }
                    
                    authUser.setCreatedAt(System.currentTimeMillis());
                    
                    // Set currentUser for immediate use
                    currentUser = authUser;
                    
                    Log.d(TAG, "✅ Created currentUser from Firebase Auth: " + displayName);
                }
            }
            
            Log.d(TAG, "✅ Firebase Auth fallback data applied successfully");
            
            // Update customize button state after Firebase Auth fallback is applied
            updateCustomizeButtonState();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error applying Firebase Auth fallback", e);
        }
    }
    
    private void loadUserDataFromLocal() {
        if (userId == -1) {
            Log.e(TAG, "Invalid user ID, cannot load user data");
            Toast.makeText(requireContext(), "User not found. Please login again.", Toast.LENGTH_LONG).show();
            logout();
            return;
        }
        
        Log.d(TAG, "Loading user data for userId: " + userId);
          db.userDao().getUserById(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                String username = user.getUsername() != null ? user.getUsername() : "Unknown User";
                Log.d(TAG, "User data loaded successfully: " + username);
                currentUser = user;
                updateUIWithUserData(user);
            } else {
                Log.e(TAG, "User not found in database for userId: " + userId);
                // Try to create a default user
                createDefaultUserIfNeeded();
            }
        });
    }
      /**
     * Load local data only as fallback when Firebase authentication/sync fails
     * This method will not override Firebase data if Firebase user is logged in
     */
    private void loadUserDataFromLocalAsFirebaseFallback() {
        if (userId == -1) {
            Log.e(TAG, "Invalid user ID, cannot load user data");
            Toast.makeText(requireContext(), "User not found. Please login again.", Toast.LENGTH_LONG).show();
            logout();
            return;
        }
        
        Log.d(TAG, "Loading local data as Firebase fallback for userId: " + userId);
          db.userDao().getUserById(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Only use local data if we don't have Firebase data
                if (!authManager.isLoggedIn() || (binding.tvName != null && 
                    (binding.tvName.getText().toString().equals("User") || 
                     binding.tvName.getText().toString().equals("Loading...")))) {
                    String username = user.getUsername() != null ? user.getUsername() : "Unknown User";
                    Log.d(TAG, "Using local fallback data: " + username);
                    currentUser = user;
                    updateUIWithUserData(user);
                } else {
                    Log.d(TAG, "Firebase data already loaded, skipping local fallback");
                    // Just update currentUser reference but don't override UI
                    currentUser = user;
                }
            } else {
                Log.e(TAG, "User not found in database for userId: " + userId);
                // Try to create a default user only if Firebase is not available
                if (!authManager.isLoggedIn()) {
                    createDefaultUserIfNeeded();
                }
            }
        });
    }
      private void updateUIWithUserData(UserEntity user) {
        try {
            // For Firebase users, don't override Firebase data with local data
            if (authManager.isLoggedIn()) {
                Log.d(TAG, "Firebase user detected - limiting local data updates to avoid overriding Firebase data");
                
                // Only update UI elements that Firebase might not provide
                // or if the current display is still showing default/loading values
                if (binding.tvName != null) {
                    String currentDisplayName = binding.tvName.getText().toString();
                    if (currentDisplayName.equals("User") || currentDisplayName.equals("Loading...") || 
                        currentDisplayName.isEmpty() || currentDisplayName.equals("Unknown User")) {
                        // Only set local data if no Firebase data is displayed
                        String displayName = getDisplayName(user);
                        binding.tvName.setText(displayName);
                        Log.d(TAG, "Updated name from local data (no Firebase data): " + displayName);
                    } else {
                        Log.d(TAG, "Keeping existing Firebase name: " + currentDisplayName);
                    }
                }
                
                if (binding.tvEmail != null) {
                    String currentEmail = binding.tvEmail.getText().toString();
                    if (currentEmail.equals("No email") || currentEmail.isEmpty() || 
                        currentEmail.equals("Loading email...")) {
                        // Only set local email if no Firebase email is displayed
                        String email = user.getEmail();
                        binding.tvEmail.setText(email != null && !email.isEmpty() ? email : "No email");
                        Log.d(TAG, "Updated email from local data (no Firebase data): " + email);
                    } else {
                        Log.d(TAG, "Keeping existing Firebase email: " + currentEmail);
                    }
                }
            } else {
                // For non-Firebase users, update UI normally
                Log.d(TAG, "Non-Firebase user - updating UI with local data");
                
                // Set user info with safe null checks
                String displayName = getDisplayName(user);
                if (binding.tvName != null) {
                    binding.tvName.setText(displayName);
                }
                
                if (binding.tvEmail != null) {
                    String email = user.getEmail();
                    binding.tvEmail.setText(email != null && !email.isEmpty() ? email : "No email");
                }
            }
            
            // These can be updated regardless of Firebase status
            // Format and display member since date
            String memberSince = formatMemberSince(user.getCreatedAt());
            if (binding.tvMemberSince != null) {
                binding.tvMemberSince.setText("Member since: " + memberSince);
            }
            
            // Display points with safe handling (only if not Firebase user or no Firebase stats loaded)
            if (binding.tvTotalPoints != null && !authManager.isLoggedIn()) {
                binding.tvTotalPoints.setText(String.valueOf(user.getPoints()));
            }
            
            // Load profile image only if no Firebase image is set
            loadProfileImage(user);
            
            // Load user statistics (for non-Firebase users)
            if (!authManager.isLoggedIn()) {
                loadUserStatistics();
            }
            
            // Setup badges
            setupBadges(user);
              // Update profile decorations
            updateProfileDecorations(user);
            
            // Update customize button state after user data is loaded
            updateCustomizeButtonState();
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with user data", e);
            // Set default values on error
            setDefaultUIValues();
        }
    }
    
    private void setDefaultUIValues() {
        try {
            if (binding.tvName != null) {
                binding.tvName.setText("User");
            }
            if (binding.tvEmail != null) {
                binding.tvEmail.setText("No email");
            }
            if (binding.tvMemberSince != null) {
                binding.tvMemberSince.setText("Member since: Recently");
            }
            if (binding.tvTotalPoints != null) {
                binding.tvTotalPoints.setText("0");
            }
            if (binding.tvTotalPlogs != null) {
                binding.tvTotalPlogs.setText("0");
            }
            if (binding.tvTotalDistance != null) {
                binding.tvTotalDistance.setText("0.0");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting default UI values", e);
        }
    }    private void loadProfileImage(UserEntity user) {
        if (binding.ivProfilePic == null || user == null) return;
        
        // Use the utility class to handle all profile image loading logic
        ProfileImageLoader.loadProfileImage(requireContext(), binding.ivProfilePic, user);
    }private String getDisplayName(UserEntity user) {
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
    
    /**
     * Updates the state of the customize profile button based on user data availability
     */
    private void updateCustomizeButtonState() {
        try {
            if (binding == null || binding.btnCustomize == null) {
                return;
            }
            
            boolean hasUserData = false;
            String debugInfo = "";
            
            // Check if we have valid user data
            if (currentUser != null) {
                hasUserData = true;
                debugInfo = "currentUser available: " + getDisplayName(currentUser);
            } else if (binding.tvName != null && binding.tvEmail != null) {
                String userName = binding.tvName.getText().toString();
                String userEmail = binding.tvEmail.getText().toString();
                
                // Check if UI has valid user data (not default/loading values)
                if (!userName.isEmpty() && !userName.equals("User") && !userName.equals("Loading...") && 
                    !userName.equals("Unknown User") && !userEmail.equals("Loading email...") &&
                    !userEmail.equals("No email")) {
                    hasUserData = true;
                    debugInfo = "UI data available: " + userName + " (" + userEmail + ")";
                } else {
                    debugInfo = "UI has default/loading values: " + userName + " / " + userEmail;
                }
            } else {
                debugInfo = "No user data or UI elements available";
            }
            
            // Update button state
            binding.btnCustomize.setEnabled(hasUserData);
            binding.btnCustomize.setAlpha(hasUserData ? 1.0f : 0.6f);
            
            Log.d(TAG, "Customize button state updated - Enabled: " + hasUserData + " (" + debugInfo + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating customize button state", e);
            // Fallback to enabled state on error
            if (binding != null && binding.btnCustomize != null) {
                binding.btnCustomize.setEnabled(true);
                binding.btnCustomize.setAlpha(1.0f);
            }
        }
    }    private void updateProfileSkin() {
        // Check if fragment is still active and binding is not null
        if (binding != null && binding.profileSkinBackground != null && isAdded() && !isDetached()) {
            // Try to load from Firebase first
            FirebaseDataManager firebaseDataManager = FirebaseDataManager.getInstance(requireContext());
              firebaseDataManager.loadProfileCustomization(new FirebaseDataManager.ProfileDataCallback() {
                @Override
                public void onProfileLoaded(UserProfile profile) {                    // Check if fragment is still active and binding is not null
                    if (binding != null && isAdded() && !isDetached()) {
                        String currentSkin = profile.getActiveBackground();
                        setSkinBackground(currentSkin);
                        Log.d(TAG, "Profile skin updated from Firebase to: " + currentSkin);
                    }
                }
                  @Override
                public void onError(String error) {                    // Check if fragment is still active and binding is not null
                    if (binding != null && isAdded() && !isDetached()) {
                        // Fallback to SharedPreferences
                        SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
                        String currentSkin = prefs.getString("selected_skin", "default");
                        setSkinBackground(currentSkin);
                        Log.d(TAG, "Profile skin updated from SharedPreferences (fallback) to: " + currentSkin);
                    }
                }
            });
        }
    }
      private int getSkinResource(String skinId) {
        switch (skinId) {
            case "nature":
                return R.drawable.profile_skin_nature;
            case "ocean":
                return R.drawable.profile_skin_ocean;
            case "sunset":
                return R.drawable.profile_skin_sunset;
            case "galaxy":
                return R.drawable.profile_skin_galaxy;
            case "animated_nature":
                return R.raw.bg_animated_nature;
            case "animated_ocean":
                return R.raw.bg_animated_ocean;
            default:
                return R.drawable.profile_skin_default;
        }
    }
      private boolean isGifSkin(String skinId) {
        return "animated_nature".equals(skinId) || "animated_ocean".equals(skinId);
    }
    
    private void setSkinBackground(String skinId) {
        if (binding == null) return;
        
        int skinResource = getSkinResource(skinId);
        
        if (isGifSkin(skinId)) {
            // For GIF backgrounds, use ImageView with Glide
            binding.profileSkinBackground.setBackground(null); // Remove drawable background
            binding.profileSkinBackgroundImage.setVisibility(View.VISIBLE);
            
            Glide.with(this)
                    .asGif()
                    .load(skinResource)
                    .centerCrop()
                    .into(binding.profileSkinBackgroundImage);
        } else {
            // For static backgrounds, use traditional method
            binding.profileSkinBackgroundImage.setVisibility(View.GONE);
            binding.profileSkinBackground.setBackgroundResource(skinResource);
        }
    }
  
    private void updateUIWithFirebaseData(com.google.firebase.firestore.DocumentSnapshot document) {
        try {
            // Get data from Firebase document
            String fullName = document.getString("nama");
            String email = document.getString("email");
            Long totalPoints = document.getLong("totalPoints");
            Double totalKm = document.getDouble("totalKm");
            String photoURL = document.getString("photoURL");
            
            Log.d(TAG, "Firebase data - Name: " + fullName + ", Email: " + email + ", Points: " + totalPoints);
            
            // Update UI with Firebase data
            if (binding.tvName != null) {
                binding.tvName.setText(fullName != null ? fullName : "Unknown User");
            }
            
            if (binding.tvEmail != null) {
                binding.tvEmail.setText(email != null ? email : "No email");
            }
            
            // Display points
            if (binding.tvTotalPoints != null) {
                int points = totalPoints != null ? totalPoints.intValue() : 0;
                binding.tvTotalPoints.setText(String.valueOf(points));
            }
              // Display distance
            if (binding.tvTotalDistance != null) {
                double km = totalKm != null ? totalKm : 0.0;
                binding.tvTotalDistance.setText(String.format(Locale.getDefault(), "%.2f km", km));
            }
            
            // Set member since date
            if (binding.tvMemberSince != null) {
                binding.tvMemberSince.setText("Member since: Recently joined");
            }
              // Load profile image if available
            if (binding.ivProfilePic != null && photoURL != null && !photoURL.isEmpty()) {
                com.bumptech.glide.Glide.with(this)
                        .load(photoURL)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(binding.ivProfilePic);            }
            
            Log.d(TAG, "UI successfully updated with Firebase data");
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with Firebase data", e);
            // Fallback to local data if Firebase UI update fails
            loadUserDataFromLocal();
        }
    }

    private void openCustomizeProfileActivity() {
        try {
            Log.d(TAG, "Customize profile button clicked");
            Log.d(TAG, "Opening CustomizeProfileActivity...");
            
            // Enhanced check: Verify if user data is available either from currentUser or UI
            boolean hasUserData = false;
            String userName = "";
            String userEmail = "";
            
            // Check if currentUser is available (for local users)
            if (currentUser != null) {
                hasUserData = true;
                userName = getDisplayName(currentUser);
                userEmail = currentUser.getEmail();
                Log.d(TAG, "User data available from currentUser: " + userName);
            }
            // Check if UI has valid user data (for Firebase users)
            else if (binding != null && binding.tvName != null && binding.tvEmail != null) {
                userName = binding.tvName.getText().toString();
                userEmail = binding.tvEmail.getText().toString();
                
                // Verify that the UI contains real user data, not default/loading values
                if (!userName.isEmpty() && !userName.equals("User") && !userName.equals("Loading...") && 
                    !userName.equals("Unknown User") && !userEmail.equals("Loading email...")) {
                    hasUserData = true;
                    Log.d(TAG, "User data available from UI: " + userName + " (" + userEmail + ")");
                    
                    // For Firebase users, create a temporary currentUser for compatibility
                    if (authManager.isLoggedIn() && currentUser == null) {
                        Log.d(TAG, "Creating temporary user object for Firebase user");
                        currentUser = createTempUserFromUI();
                    }
                } else {
                    Log.w(TAG, "UI contains default/loading values - Name: '" + userName + "', Email: '" + userEmail + "'");
                }
            }
            
            // If no user data is available, show warning and return
            if (!hasUserData) {
                Log.w(TAG, "Current user is null, waiting for profile to load");
                if (authManager.isLoggedIn()) {
                    Toast.makeText(requireContext(), "Loading profile data, please wait...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Please wait for profile to load", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            // Validate userId as well
            if (userId == -1) {
                Log.e(TAG, "Invalid user ID, cannot open customize profile");
                Toast.makeText(requireContext(), "User session invalid. Please login again.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Create intent to open CustomizeProfileActivity
            Intent intent = new Intent(requireContext(), com.example.glean.activity.CustomizeProfileActivity.class);
            
            // Pass user data
            intent.putExtra("USER_ID", userId);
            
            // For Firebase users, pass additional data if available
            if (authManager.isLoggedIn()) {
                intent.putExtra("FIREBASE_USER", true);
                intent.putExtra("USER_NAME", userName);
                intent.putExtra("USER_EMAIL", userEmail);
            }
            
            // Start activity
            startActivity(intent);
            Log.d(TAG, "CustomizeProfileActivity opened successfully for user: " + userName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening CustomizeProfileActivity", e);
            Toast.makeText(requireContext(), "Failed to open profile customization", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Creates a temporary UserEntity from UI data for Firebase users
     * This ensures compatibility with existing code that expects currentUser to be non-null
     */
    private UserEntity createTempUserFromUI() {
        try {
            UserEntity tempUser = new UserEntity();
            tempUser.setId(userId);
            
            if (binding != null) {
                // Extract name from UI
                if (binding.tvName != null) {
                    String fullName = binding.tvName.getText().toString();
                    // Try to split first/last name
                    String[] nameParts = fullName.split(" ", 2);
                    tempUser.setFirstName(nameParts[0]);
                    if (nameParts.length > 1) {
                        tempUser.setLastName(nameParts[1]);
                    }
                    tempUser.setUsername(fullName);
                }
                
                // Extract email from UI
                if (binding.tvEmail != null) {
                    String email = binding.tvEmail.getText().toString();
                    if (!email.equals("No email") && !email.equals("Loading email...")) {
                        tempUser.setEmail(email);
                    }
                }
                
                // Extract points from UI if available
                if (binding.tvTotalPoints != null) {
                    try {
                        int points = Integer.parseInt(binding.tvTotalPoints.getText().toString());
                        tempUser.setPoints(points);
                    } catch (NumberFormatException e) {
                        tempUser.setPoints(0);
                    }
                }
            }
              // Set timestamp
            tempUser.setCreatedAt(System.currentTimeMillis());
            
            Log.d(TAG, "Created temporary user object from UI data: " + tempUser.getUsername());
            return tempUser;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating temporary user from UI", e);
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImageSourceDialog();
            } else {
                Toast.makeText(requireContext(), "Storage permission required to select image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImageFromCamera();
            } else {
                Toast.makeText(requireContext(), "Camera permission required to take photo", Toast.LENGTH_SHORT).show();
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
        // Update button state on resume
        updateCustomizeButtonState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Stop Firebase real-time listeners
        if (firebaseDataManager != null) {
            Log.d(TAG, "🔥 Stopping Firebase real-time listeners");
            firebaseDataManager.stopAllListeners();
        }
        
        binding = null;
    }    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
        
        // Optional: Clean up old profile images periodically
        // This is done in background and won't block the UI
        try {
            LocalProfileImageUtil.cleanupOldProfileImages(requireContext());
        } catch (Exception e) {
            // Ignore cleanup errors during destroy
        }
    }// Missing essential methods that are called by the UI
    private void showEditProfileDialog() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(getLayoutInflater());
        
        // Perbaikan: Pre-fill current user data dengan nama dari getFullName() atau username/email
        String displayName = null;
        if (currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()) {
            displayName = currentUser.getFullName();
        } else if (currentUser.getUsername() != null && !currentUser.getUsername().isEmpty()) {
            displayName = currentUser.getUsername();
        } else if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            displayName = currentUser.getEmail();
        } else {
            displayName = "";
        }
        dialogBinding.etName.setText(displayName);
        dialogBinding.etEmail.setText(currentUser.getEmail());
        dialogBinding.etEmail.setEnabled(false); // Email is read-only
        
        // Create and show dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.getRoot());
        
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(dialogBinding.getRoot());
        
        // Set up button listeners
        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialogBinding.btnSave.setOnClickListener(v -> {
            String newName = dialogBinding.etName.getText().toString().trim();
            String newPassword = dialogBinding.etPassword.getText().toString().trim();
            
            // Validate name
            if (newName.isEmpty()) {
                dialogBinding.etName.setError("Name cannot be empty");
                return;
            }
            
            // Validate password if provided
            if (!newPassword.isEmpty() && newPassword.length() < 8) {
                dialogBinding.etPassword.setError("Password must be at least 8 characters");
                return;
            }
            
            // Update user data
            updateUserProfile(newName, newPassword, dialog);
        });
        
        dialog.show();
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }    private void logout() {
        try {
            // Clean up local profile image for current user before logout
            String currentUserId = getCurrentUserIdForStorage();
            if (currentUserId != null) {
                boolean deleted = LocalProfileImageUtil.deleteLocalProfileImage(requireContext(), currentUserId);
                Log.d(TAG, "Profile image cleanup during logout: " + (deleted ? "successful" : "failed"));
            }
            
            // Clear user session
            SharedPreferences prefs = requireActivity().getSharedPreferences("USER_PREFS", 0);
            prefs.edit().clear().apply();
              // Sign out from Firebase if logged in
            if (authManager.isLoggedIn()) {
                authManager.logout();
            }
            
            // Navigate back to login or main screen
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
            Toast.makeText(requireContext(), "Error during logout", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSettingsDialog() {
        // Placeholder implementation
        Toast.makeText(requireContext(), "Settings feature coming soon", Toast.LENGTH_SHORT).show();
    }    private void checkPermissionsAndSelectImage() {
        // Check storage permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), 
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                    REQUEST_STORAGE_PERMISSION);
        } else {
            showImageSourceDialog();
        }
    }    private void showImageSourceDialog() {
        String[] options = {"Pilih dari Galeri", "Ambil Foto Baru"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ganti Foto Profil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Pilih dari galeri
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/*");
                        startActivityForResult(intent, PICK_IMAGE_REQUEST);
                    } else if (which == 1) {
                        // Ambil foto baru
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                        } else {
                            captureImageFromCamera();
                        }
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void captureImageFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";
                File storageDir = requireActivity().getExternalFilesDir(null);
                photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
                cameraImageUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", photoFile);
            } catch (Exception ex) {
                Toast.makeText(requireContext(), "Gagal membuat file foto", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                startActivityForResult(intent, CAMERA_REQUEST);
            }
        }
    }    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SKIN_SELECTION_REQUEST && resultCode == Activity.RESULT_OK) {
            // Profile customization was updated, refresh badges
            Log.d(TAG, "Profile customization updated, refreshing badges");
            if (currentUser != null) {
                setupBadges(currentUser);
            }
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            // Start image cropping
            startImageCropping(selectedImageUri);
        } else if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            if (cameraImageUri != null) {
                // Start image cropping
                startImageCropping(cameraImageUri);
            }
        } else if (requestCode == UCROP_REQUEST) {
            handleCropResult(resultCode, data);
        }
    }
    
    /**
     * Start image cropping with uCrop
     */
    private void startImageCropping(Uri sourceUri) {
        try {
            // Create destination file for cropped image
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "CROP_" + timeStamp + ".jpg";
            File cropDir = new File(requireContext().getCacheDir(), "crop");
            if (!cropDir.exists()) {
                cropDir.mkdirs();
            }
            File destinationFile = new File(cropDir, imageFileName);
            Uri destinationUri = Uri.fromFile(destinationFile);
            
            // Configure uCrop options
            UCrop.Options options = new UCrop.Options();
            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(85);
            options.setMaxBitmapSize(1024);
            options.setHideBottomControls(false);
            options.setFreeStyleCropEnabled(false);
            options.setShowCropFrame(true);
            options.setShowCropGrid(true);
            options.setCircleDimmedLayer(true); // Circular crop overlay
            options.setCropGridStrokeWidth(2);
            options.setCropFrameStrokeWidth(4);
            options.setToolbarTitle("Crop Profile Picture");
            
            // Set colors to match app theme
            options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.primary));
            options.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.primary_dark));
            options.setActiveControlsWidgetColor(ContextCompat.getColor(requireContext(), R.color.accent));
            
            // Start uCrop with 1:1 aspect ratio for profile pictures
            UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(512, 512)
                    .withOptions(options)
                    .start(requireContext(), this, UCROP_REQUEST);
                    
        } catch (Exception e) {
            Log.e(TAG, "Error starting image cropping", e);
            Toast.makeText(requireContext(), "Gagal memulai crop gambar", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Handle result from uCrop
     */
    private void handleCropResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            // Get cropped image URI
            Uri croppedImageUri = UCrop.getOutput(data);
            if (croppedImageUri != null) {
                // Save the cropped image locally
                saveProfileImageLocally(croppedImageUri);
            } else {
                Toast.makeText(requireContext(), "Gagal mendapatkan hasil crop", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            // Handle crop error
            Throwable cropError = UCrop.getError(data);
            Log.e(TAG, "Crop error", cropError);
            Toast.makeText(requireContext(), "Error saat crop gambar: " + 
                (cropError != null ? cropError.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
        } else {
            // User cancelled cropping
            Log.d(TAG, "Image cropping cancelled by user");
        }
    }private void saveProfileImageLocally(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(requireContext(), "Gambar tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading indicator
        Toast.makeText(requireContext(), "Menyimpan foto profil...", Toast.LENGTH_SHORT).show();
        
        // Execute in background thread
        executor.execute(() -> {
            try {
                String currentUserId = getCurrentUserIdForStorage();
                if (currentUserId == null) {
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                
                // Save image locally
                String localImagePath = LocalProfileImageUtil.saveProfileImageLocally(
                    requireContext(), currentUserId, imageUri);
                
                if (localImagePath != null) {
                    // Update user's profile image path in local database
                    updateUserProfileImagePath(localImagePath);
                    
                    // Update Firestore with placeholder URL (not the local path)
                    updateUserPhotoUrlInFirestore(currentUserId, LocalProfileImageUtil.getDefaultProfileImageUrl());                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Foto profil berhasil disimpan! Gambar disimpan secara lokal.", Toast.LENGTH_LONG).show();
                        // Refresh the profile image display
                        if (currentUser != null) {
                            loadProfileImage(currentUser);
                        }
                    });
                } else {                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "Gagal menyimpan foto profil ke storage lokal", Toast.LENGTH_SHORT).show()
                    );
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving profile image locally", e);
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(requireContext(), "Gagal menyimpan foto: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
      /**
     * Update user's profile image path in local SQLite database
     * This saves the local file path to the database, NOT to Firestore
     */
    private void updateUserProfileImagePath(String localImagePath) {
        if (currentUser != null) {
            executor.execute(() -> {
                try {
                    currentUser.setProfileImagePath(localImagePath);
                    db.userDao().update(currentUser);
                    Log.d(TAG, "Updated user profile image path in local database: " + localImagePath);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating user profile image path in database", e);
                }
            });
        }
    }
    
    /**
     * Get current user ID for storage purposes
     */
    private String getCurrentUserIdForStorage() {
        if (authManager != null && authManager.isLoggedIn()) {
            return authManager.getUserId();
        } else if (currentUser != null) {
            return String.valueOf(currentUser.getId());
        }
        return null;
    }    private void updateUserPhotoUrlInFirestore(String userId, String photoUrl) {
        if (!NetworkUtil.isNetworkAvailable(requireContext())) {
            // No network, skip Firestore update
            Log.d(TAG, "No network available, skipping Firestore photo URL update");
            return;
        }
        
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(userId)
            .update("photoURL", photoUrl)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Photo URL updated in Firestore (placeholder): " + photoUrl);
                // Don't show toast for placeholder updates to avoid confusion
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Failed to update photo URL in Firestore: " + e.getMessage());
                // Don't show error toast for placeholder updates
            });
    }
    
    // Create a default user in the local database if none exists
    private void createDefaultUserIfNeeded() {
        executor.execute(() -> {
            if (db.userDao().getUserCount() == 0) {
                UserEntity defaultUser = new UserEntity();
                defaultUser.setUsername("User");
                defaultUser.setEmail("user@example.com");
                defaultUser.setPassword("password");
                defaultUser.setFirstName("User");
                defaultUser.setLastName("");
                defaultUser.setPoints(0);
                defaultUser.setCreatedAt(System.currentTimeMillis());
                db.userDao().insert(defaultUser);
                Log.d(TAG, "Default user created");
            }
        });
    }

    // Format the member since date as a readable string
    private String formatMemberSince(long createdAt) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(createdAt));
    }

    // Load user statistics (e.g., total plogs, distance, etc.)
    private void loadUserStatistics() {
        if (currentUser == null) return;
        // Example: set dummy stats, replace with real queries if available
        if (binding.tvTotalPlogs != null) binding.tvTotalPlogs.setText("0");
        if (binding.tvTotalDistance != null) binding.tvTotalDistance.setText("0 km");
    }

    // Setup badges for the user and display them in the RecyclerView
    private void setupBadges(UserEntity user) {
        if (binding.rvBadges == null) return;
        // Example: create dummy badges, replace with real badge logic if available
        List<Badge> badges = new ArrayList<>();
        badges.add(new Badge(1, "Eco Starter", "Join the app", "milestone", 1, true));
        badges.add(new Badge(2, "First Plog", "Complete your first plogging", "activity", 1, user.getPoints() > 0));
        badges.add(new Badge(3, "Eco Warrior", "Reach 100 points", "points", 2, user.getPoints() >= 100));
        ProfileBadgeAdapter adapter = new ProfileBadgeAdapter(requireContext(), badges);
        binding.rvBadges.setAdapter(adapter);
    }

    // Update profile decorations (e.g., frame, background) based on user data
    private void updateProfileDecorations(UserEntity user) {
        if (binding == null) return;
        String decoration = user.getActiveDecoration();
        // Example: set a background or frame, replace with real logic if needed
        if (binding.profileSkinBackground != null) {
            if ("gold".equals(decoration)) {
                binding.profileSkinBackground.setBackgroundResource(R.drawable.gold_frame);
            } else if ("silver".equals(decoration)) {
                binding.profileSkinBackground.setBackgroundResource(R.drawable.silver_frame);
            } else if ("bronze".equals(decoration)) {
                binding.profileSkinBackground.setBackgroundResource(R.drawable.bronze_frame);
            } else {
                binding.profileSkinBackground.setBackgroundResource(0); // Default
            }
        }
    }

    // Update user profile (name and password) in the database
    private void updateUserProfile(String newName, String newPassword, BottomSheetDialog dialog) {
        if (currentUser == null) return;
        executor.execute(() -> {
            currentUser.setUsername(newName);
            currentUser.setFirstName(newName.split(" ")[0]);
            if (newName.split(" ").length > 1) {
                currentUser.setLastName(newName.substring(newName.indexOf(' ') + 1));
            }
            if (!newPassword.isEmpty()) {
                currentUser.setPassword(newPassword);
            }
            db.userDao().update(currentUser);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), getString(R.string.profile_updated_success), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadUserData();
            });
        });
    }
}
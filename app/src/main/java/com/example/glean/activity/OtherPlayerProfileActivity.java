package com.example.glean.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.adapter.ProfileBadgeAdapter;
import com.example.glean.databinding.ActivityOtherPlayerProfileBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.Badge;
import com.example.glean.model.RankingUser;
import com.example.glean.model.UserEntity;
import com.example.glean.util.AvatarManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity untuk menampilkan profil pemain lain dari ranking
 * Hanya menampilkan data publik tanpa opsi edit
 */
public class OtherPlayerProfileActivity extends AppCompatActivity {

    private static final String TAG = "OtherPlayerProfile";
    public static final String EXTRA_PLAYER_ID = "player_id";
    public static final String EXTRA_PLAYER_USERNAME = "player_username";
    public static final String EXTRA_RANKING_USER = "ranking_user";

    private ActivityOtherPlayerProfileBinding binding;
    private FirebaseFirestore firestore;
    private AppDatabase db;
    private ExecutorService executor;
    
    private String playerId;
    private String playerUsername;
    private RankingUser rankingUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtherPlayerProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());        // Initialize components
        firestore = FirebaseFirestore.getInstance();
        db = AppDatabase.getInstance(this);
        executor = Executors.newFixedThreadPool(2);

        // Get data from intent
        getIntentData();
        
        // Setup UI
        setupUI();
        
        // Load player data
        loadPlayerData();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        playerId = intent.getStringExtra(EXTRA_PLAYER_ID);
        playerUsername = intent.getStringExtra(EXTRA_PLAYER_USERNAME);
        
        // Try to get RankingUser object if passed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            rankingUser = intent.getSerializableExtra(EXTRA_RANKING_USER, RankingUser.class);
        } else {
            rankingUser = (RankingUser) intent.getSerializableExtra(EXTRA_RANKING_USER);
        }

        Log.d(TAG, "Player ID: " + playerId + ", Username: " + playerUsername);
    }    private void setupUI() {
        // Setup back button
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Hide all edit controls - this is read-only profile
        hideEditControls();
        
        // Set initial title in header (no separate tvTitle in new layout)
        // The title is now part of the header layout directly
        
        // Setup badges RecyclerView
        if (binding.rvBadges != null) {
            binding.rvBadges.setLayoutManager(new GridLayoutManager(this, 3));
        }
    }    private void hideEditControls() {
        // These elements don't exist in the new layout, so no need to hide them
        // The new layout is designed to not have edit controls for other players
        
        // Make profile picture non-clickable
        if (binding.ivProfilePic != null) {
            binding.ivProfilePic.setClickable(false);
            binding.ivProfilePic.setOnClickListener(null);
        }
    }

    private void loadPlayerData() {
        if (rankingUser != null) {
            // Use data from ranking if available
            displayPlayerData(rankingUser);
        } else if (playerId != null) {
            // Load from Firebase
            loadFromFirebase();
        } else {
            showError("Invalid player data");
        }
    }    private void loadFromFirebase() {
        // Show loading state
        if (binding.progressBar != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        
        firestore.collection("users")
                .document(playerId)
                .get()
                .addOnSuccessListener(this::handleFirebaseData)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading player data", e);
                    if (binding.progressBar != null) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                    showError("Gagal memuat data pemain");
                });
    }    private void handleFirebaseData(DocumentSnapshot document) {
        // Hide loading state
        if (binding.progressBar != null) {
            binding.progressBar.setVisibility(View.GONE);
        }
        
        if (document.exists()) {
            try {                // Convert Firebase data to RankingUser
                String username = document.getString("username");
                String fullName = document.getString("fullName");
                Number totalPoints = document.getLong("totalPoints");
                Number totalDistance = document.getDouble("totalDistance");
                Number totalTrash = document.getLong("totalTrashCollected");
                String activeAvatar = document.getString("activeAvatar"); // Get activeAvatar instead of profileImageUrl

                RankingUser user = new RankingUser();
                user.setUserId(playerId);
                user.setUsername(username != null ? username : playerUsername);
                user.setActiveAvatar(activeAvatar); // Set activeAvatar instead of profileImageUrl
                user.setTotalPoints(totalPoints != null ? totalPoints.intValue() : 0);
                user.setTotalDistance(totalDistance != null ? totalDistance.doubleValue() : 0.0);
                user.setTrashCount(totalTrash != null ? totalTrash.intValue() : 0);
                user.setBadgeCount(0); // Will be calculated from achievements

                // Load player background directly from this document (fresh data)
                loadPlayerBackgroundFromDocument(document);
                
                displayPlayerData(user);
                
            } catch (Exception e) {
                Log.e(TAG, "Error parsing player data", e);
                showError("Error parsing player data");
            }
        } else {
            showError("Player not found");
        }
    }    private void displayPlayerData(RankingUser user) {
        try {
            // Display basic info
            String displayName = user.getUsername();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "Player " + user.getUserId().substring(0, 8);
            }
            
            binding.tvName.setText(displayName);
            // No separate tvTitle in new layout - title is static in header
            
            // Display statistics
            binding.tvTotalPoints.setText(String.valueOf(user.getTotalPoints()));
            binding.tvTotalPlogs.setText("0"); // Not available in ranking data
            binding.tvTotalDistance.setText(String.format(Locale.getDefault(), 
                    "%.1f", user.getTotalDistance() / 1000.0));
              // Load profile image using activeAvatar (local assets only)
            loadProfileImage(user.getActiveAvatar());
            
            // Load and apply profile background from Firestore only if not loaded from handleFirebaseData
            if (rankingUser != null) {
                // Data came from ranking, need to fetch background separately
                loadPlayerBackgroundFromFirestore(user.getUserId());
            }
            // If data came from handleFirebaseData, background is already loaded
            
            // Generate and display badges based on points
            generateAndDisplayBadges(user.getTotalPoints());
            
            // Show email and member since in new layout (they exist and are visible)
            if (binding.tvEmail != null) {
                binding.tvEmail.setText("Email not available for other players");
            }
            if (binding.tvMemberSince != null) {
                binding.tvMemberSince.setText("Member info private");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying player data", e);
            showError("Error displaying data");
        }
    }    /**
     * Load the active background for the visited player from an existing DocumentSnapshot
     */
    private void loadPlayerBackgroundFromDocument(DocumentSnapshot document) {
        try {
            // Get the activeBackground field directly from document root (not from user_profile)
            String activeBackground = "default"; // Default fallback
            
            String backgroundFromFirestore = document.getString("activeBackground");
            if (backgroundFromFirestore != null && !backgroundFromFirestore.trim().isEmpty()) {
                activeBackground = backgroundFromFirestore;
            }
            
            Log.d(TAG, "Player background loaded from document: " + activeBackground);
            
            // Apply the background to the profile
            int backgroundResource = getSkinResource(activeBackground);
            if (binding.profileSkinBackground != null) {
                binding.profileSkinBackground.setBackgroundResource(backgroundResource);
                Log.d(TAG, "Background applied from document for other player: " + activeBackground);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing player background from document", e);
            // Fallback to default background
            applyDefaultBackground();
        }
    }    /**
     * Load the active background for the visited player from Firestore
     */
    private void loadPlayerBackgroundFromFirestore(String userId) {
        Log.d(TAG, "Loading background for player: " + userId);
        
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        try {
                            // Get the activeBackground field directly from document root (not from user_profile)
                            String activeBackground = "default"; // Default fallback
                            
                            String backgroundFromFirestore = document.getString("activeBackground");
                            if (backgroundFromFirestore != null && !backgroundFromFirestore.trim().isEmpty()) {
                                activeBackground = backgroundFromFirestore;
                            }
                            
                            Log.d(TAG, "Player background loaded: " + activeBackground);
                            
                            // Apply the background to the profile
                            int backgroundResource = getSkinResource(activeBackground);
                            if (binding.profileSkinBackground != null) {
                                binding.profileSkinBackground.setBackgroundResource(backgroundResource);
                                Log.d(TAG, "Background applied for other player: " + activeBackground);
                            }
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing player background data", e);
                            // Fallback to default background
                            applyDefaultBackground();
                        }
                    } else {
                        Log.w(TAG, "Player document not found, using default background");
                        applyDefaultBackground();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading player background from Firestore", e);
                    // Fallback to default background
                    applyDefaultBackground();
                });
    }

    /**
     * Apply default background when Firestore data is not available
     */
    private void applyDefaultBackground() {
        if (binding.profileSkinBackground != null) {
            binding.profileSkinBackground.setBackgroundResource(R.drawable.profile_skin_default);
            Log.d(TAG, "Default background applied for other player");
        }
    }

    /**
     * Get the drawable resource for a skin ID (same logic as ProfileFragment)
     */
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
            default:
                return R.drawable.profile_skin_default;
        }
    }    private void loadProfileImage(String activeAvatar) {
        if (activeAvatar != null && !activeAvatar.trim().isEmpty()) {
            // Use AvatarManager to load local avatar
            AvatarManager.loadAvatarIntoImageView(this, binding.ivProfilePic, activeAvatar);
        } else {
            // Fallback to default avatar when activeAvatar is missing
            binding.ivProfilePic.setImageResource(R.drawable.avatar_default);
        }
    }

    private void generateAndDisplayBadges(int points) {
        List<Badge> badges = new ArrayList<>();
        int badgeIdCounter = 1;
        
        // Generate badges based on points (same logic as ProfileFragment)
        if (points >= 50) {
            Badge starter = new Badge(badgeIdCounter++, "Starter", "Getting started with plogging", "starter", 1, true);
            starter.setIconResource(R.drawable.ic_star);
            badges.add(starter);
        }
        if (points >= 100) {
            Badge greenHelper = new Badge(badgeIdCounter++, "Green Helper", "Environmental helper", "green_helper", 1, true);
            greenHelper.setIconResource(R.drawable.ic_leaf);
            badges.add(greenHelper);
        }
        if (points >= 150) {
            Badge ecoWarrior = new Badge(badgeIdCounter++, "Eco Warrior", "Environmental champion", "eco_warrior", 2, true);
            ecoWarrior.setIconResource(R.drawable.ic_award);
            badges.add(ecoWarrior);
        }
        if (points >= 200) {
            Badge greenChampion = new Badge(badgeIdCounter++, "Green Champion", "Green environmental champion", "green_champion", 2, true);
            greenChampion.setIconResource(R.drawable.ic_award);
            badges.add(greenChampion);
        }
        if (points >= 500) {
            Badge earthGuardian = new Badge(badgeIdCounter++, "Earth Guardian", "Protector of the environment", "earth_guardian", 3, true);
            earthGuardian.setIconResource(R.drawable.ic_globe);
            badges.add(earthGuardian);
        }
        if (points >= 1000) {
            Badge expertPlogger = new Badge(badgeIdCounter++, "Expert Plogger", "Master of plogging", "expert_plogger", 3, true);
            expertPlogger.setIconResource(R.drawable.ic_crown);
            badges.add(expertPlogger);
        }
        
        // Display badges (read-only)
        if (binding.rvBadges != null && !badges.isEmpty()) {
            ProfileBadgeAdapter adapter = new ProfileBadgeAdapter(this, badges);
            binding.rvBadges.setAdapter(adapter);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}

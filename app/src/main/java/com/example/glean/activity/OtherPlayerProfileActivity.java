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
    }

    private void setupUI() {
        // Setup back button
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Hide all edit controls - this is read-only profile
        hideEditControls();
        
        // Set initial title
        binding.tvTitle.setText(playerUsername != null ? playerUsername + "'s Profile" : "Player Profile");
        
        // Setup badges RecyclerView
        if (binding.rvBadges != null) {
            binding.rvBadges.setLayoutManager(new GridLayoutManager(this, 3));
        }
    }

    private void hideEditControls() {
        // Hide all editing/settings elements
        if (binding.btnEditProfile != null) {
            binding.btnEditProfile.setVisibility(View.GONE);
        }
        if (binding.btnSettings != null) {
            binding.btnSettings.setVisibility(View.GONE);
        }
        if (binding.btnLogout != null) {
            binding.btnLogout.setVisibility(View.GONE);
        }
        if (binding.btnCustomize != null) {
            binding.btnCustomize.setVisibility(View.GONE);
        }
        
        // Make profile picture non-clickable
        if (binding.ivProfilePic != null) {
            binding.ivProfilePic.setClickable(false);
            binding.ivProfilePic.setOnClickListener(null);
        }
          // Hide profile settings section if exists
        // View profileSettingsCard = findViewById(R.id.cardProfileSettings);
        // if (profileSettingsCard != null) {
        //     profileSettingsCard.setVisibility(View.GONE);
        // }
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
    }

    private void loadFromFirebase() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        firestore.collection("users")
                .document(playerId)
                .get()
                .addOnSuccessListener(this::handleFirebaseData)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading player data", e);
                    binding.progressBar.setVisibility(View.GONE);
                    showError("Gagal memuat data pemain");
                });
    }

    private void handleFirebaseData(DocumentSnapshot document) {
        binding.progressBar.setVisibility(View.GONE);
        
        if (document.exists()) {
            try {
                // Convert Firebase data to RankingUser
                String username = document.getString("username");
                String fullName = document.getString("fullName");
                Number totalPoints = document.getLong("totalPoints");
                Number totalDistance = document.getDouble("totalDistance");
                Number totalTrash = document.getLong("totalTrashCollected");
                String profileImageUrl = document.getString("profileImageUrl");

                RankingUser user = new RankingUser();
                user.setUserId(playerId);
                user.setUsername(username != null ? username : playerUsername);
                user.setProfileImageUrl(profileImageUrl);
                user.setTotalPoints(totalPoints != null ? totalPoints.intValue() : 0);
                user.setTotalDistance(totalDistance != null ? totalDistance.doubleValue() : 0.0);
                user.setTrashCount(totalTrash != null ? totalTrash.intValue() : 0);
                user.setBadgeCount(0); // Will be calculated from achievements

                displayPlayerData(user);
                
            } catch (Exception e) {
                Log.e(TAG, "Error parsing player data", e);
                showError("Error parsing player data");
            }
        } else {
            showError("Player not found");
        }
    }

    private void displayPlayerData(RankingUser user) {
        try {
            // Display basic info
            String displayName = user.getUsername();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "Player " + user.getUserId().substring(0, 8);
            }
            
            binding.tvName.setText(displayName);
            binding.tvTitle.setText(displayName + "'s Profile");
            
            // Display statistics
            binding.tvTotalPoints.setText(String.valueOf(user.getTotalPoints()));
            binding.tvTotalPlogs.setText("0"); // Not available in ranking data
            binding.tvTotalDistance.setText(String.format(Locale.getDefault(), 
                    "%.1f", user.getTotalDistance() / 1000.0));
            
            // Load profile image
            loadProfileImage(user.getProfileImageUrl());
            
            // Generate and display badges based on points
            generateAndDisplayBadges(user.getTotalPoints());
            
            // Hide email and member since for privacy
            if (binding.tvEmail != null) {
                binding.tvEmail.setVisibility(View.GONE);
            }
            if (binding.tvMemberSince != null) {
                binding.tvMemberSince.setVisibility(View.GONE);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying player data", e);
            showError("Error displaying data");
        }
    }

    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_user_avatar)
                    .error(R.drawable.ic_user_avatar)
                    .circleCrop()
                    .into(binding.ivProfilePic);
        } else {
            binding.ivProfilePic.setImageResource(R.drawable.ic_user_avatar);
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

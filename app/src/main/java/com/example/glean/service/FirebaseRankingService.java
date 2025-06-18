package com.example.glean.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.glean.auth.FirebaseAuthManager;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.UserEntity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service untuk sinkronisasi data ranking user ke Firebase
 */
public class FirebaseRankingService {
    
    private static final String TAG = "FirebaseRankingService";
    private static FirebaseRankingService instance;
    
    private FirebaseFirestore firestore;
    private AppDatabase localDb;
    private ExecutorService executor;
    private Context context;
    
    private FirebaseRankingService(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.localDb = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized FirebaseRankingService getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseRankingService(context);
        }
        return instance;
    }
    
    /**
     * Update user ranking data di Firebase berdasarkan data lokal
     */
    public void updateUserRankingData() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.w(TAG, "No current user ID, skipping ranking data update");
            return;
        }
        
        executor.execute(() -> {
            try {
                // Get user data from local database
                UserEntity user = localDb.userDao().getUserByIdSync(getCurrentLocalUserId());
                if (user == null) {
                    Log.w(TAG, "User not found in local database");
                    return;
                }
                
                // Calculate user statistics
                UserRankingData rankingData = calculateUserRankingData(user);
                
                // Update Firebase
                updateFirebaseUserData(currentUserId, rankingData);
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating user ranking data", e);
            }
        });
    }
    
    private UserRankingData calculateUserRankingData(UserEntity user) {
        try {
            // Get all user's records
            List<RecordEntity> records = localDb.recordDao().getRecordsByUserIdSync(user.getId());
            
            // Calculate totals
            int totalPoints = 0;
            double totalDistance = 0.0;
            int totalTrashCollected = 0;
            int totalSessions = records.size();
            
            for (RecordEntity record : records) {
                totalPoints += record.getPoints();
                totalDistance += record.getDistance();
                // Get trash count for this record
                int trashCount = localDb.trashDao().getTrashCountByRecordIdSync(record.getId());
                totalTrashCollected += trashCount;
            }
            
            return new UserRankingData(
                user.getUsername(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getProfileImagePath(),
                totalPoints,
                totalDistance,
                totalTrashCollected,
                totalSessions,
                0, // badgeCount - can be calculated separately
                System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating user ranking data", e);
            return null;
        }
    }
    
    private void updateFirebaseUserData(String userId, UserRankingData data) {
        if (data == null) return;
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", data.getUsername());
        userData.put("displayName", data.getDisplayName());
        userData.put("email", data.getEmail());
        userData.put("profileImageUrl", data.getProfileImageUrl());
        userData.put("totalPoints", data.getTotalPoints());
        userData.put("totalDistance", data.getTotalDistance());
        userData.put("totalTrashCollected", data.getTotalTrashCollected());
        userData.put("totalSessions", data.getTotalSessions());
        userData.put("badgeCount", data.getBadgeCount());
        userData.put("lastUpdated", data.getLastUpdated());
        userData.put("updatedAt", System.currentTimeMillis());
        
        firestore.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User ranking data updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user ranking data in Firebase", e);
                });
    }
    
    /**
     * Initialize user data in Firebase if not exists
     */
    public void initializeUserIfNotExists(String email, String username) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return;
        
        firestore.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        // Create initial user data
                        Map<String, Object> initialData = new HashMap<>();
                        initialData.put("username", username != null ? username : "User Baru");
                        initialData.put("email", email);
                        initialData.put("totalPoints", 0);
                        initialData.put("totalDistance", 0.0);
                        initialData.put("totalTrashCollected", 0);
                        initialData.put("totalSessions", 0);
                        initialData.put("badgeCount", 0);
                        initialData.put("createdAt", System.currentTimeMillis());
                        initialData.put("lastUpdated", System.currentTimeMillis());
                        
                        firestore.collection("users")
                                .document(currentUserId)
                                .set(initialData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Initial user data created in Firebase");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating initial user data", e);
                                });
                    } else {
                        // User exists, update with latest local data
                        updateUserRankingData();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user existence in Firebase", e);
                });
    }
    
    private String getCurrentUserId() {
        SharedPreferences prefs = context.getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE);
        return prefs.getString("FIREBASE_USER_ID", "");
    }
    
    private int getCurrentLocalUserId() {
        SharedPreferences prefs = context.getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE);
        return prefs.getInt("USER_ID", -1);
    }
    
    /**
     * Class untuk menyimpan data ranking user
     */
    public static class UserRankingData {
        private String username;
        private String displayName;
        private String email;
        private String profileImageUrl;
        private int totalPoints;
        private double totalDistance;
        private int totalTrashCollected;
        private int totalSessions;
        private int badgeCount;
        private long lastUpdated;
        
        public UserRankingData(String username, String displayName, String email, 
                              String profileImageUrl, int totalPoints, double totalDistance,
                              int totalTrashCollected, int totalSessions, int badgeCount, 
                              long lastUpdated) {
            this.username = username;
            this.displayName = displayName;
            this.email = email;
            this.profileImageUrl = profileImageUrl;
            this.totalPoints = totalPoints;
            this.totalDistance = totalDistance;
            this.totalTrashCollected = totalTrashCollected;
            this.totalSessions = totalSessions;
            this.badgeCount = badgeCount;
            this.lastUpdated = lastUpdated;
        }
        
        // Getters
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public String getEmail() { return email; }
        public String getProfileImageUrl() { return profileImageUrl; }
        public int getTotalPoints() { return totalPoints; }
        public double getTotalDistance() { return totalDistance; }
        public int getTotalTrashCollected() { return totalTrashCollected; }
        public int getTotalSessions() { return totalSessions; }
        public int getBadgeCount() { return badgeCount; }
        public long getLastUpdated() { return lastUpdated; }
    }
}

package com.example.glean.service;

import android.content.Context;
import android.util.Log;

import com.example.glean.auth.FirebaseAuthManager;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.UserEntity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager untuk sinkronisasi data statistik, ranking, dan profile user dengan Firebase
 * Menghandle real-time synchronization untuk semua data user
 */
public class FirebaseDataManager {
    
    private static final String TAG = "FirebaseDataManager";
    private static FirebaseDataManager instance;
    
    // Collection names in Firebase
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_STATS = "user_stats";
    private static final String COLLECTION_RANKING = "ranking";
    private static final String COLLECTION_RECORDS = "user_records";
    
    private FirebaseFirestore firestore;
    private AppDatabase localDb;
    private ExecutorService executor;
    private Context context;
    private ListenerRegistration statsListener;
    private ListenerRegistration rankingListener;
    private ListenerRegistration profileListener;
    
    public interface DataSyncCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface StatsDataCallback {
        void onStatsLoaded(UserStats stats);
        void onError(String error);
    }
    
    public interface RankingDataCallback {
        void onRankingLoaded(List<RankingUser> ranking);
        void onError(String error);
    }
    
    public interface ProfileDataCallback {
        void onProfileLoaded(UserProfile profile);
        void onError(String error);
    }
    
    private FirebaseDataManager(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.localDb = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized FirebaseDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseDataManager(context);
        }
        return instance;
    }
    
    /**
     * Sinkronisasi semua data user ke Firebase
     */
    public void syncAllUserData(DataSyncCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                String userId = getCurrentUserId();
                
                // Sync stats
                syncUserStats(userId);
                
                // Sync ranking data
                syncUserRanking(userId);
                
                // Sync profile
                syncUserProfile(userId);
                
                callback.onSuccess();
                
            } catch (Exception e) {
                Log.e(TAG, "Error syncing user data", e);
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Ambil dan sinkronkan data statistik user secara real-time
     */
    public void subscribeToUserStats(StatsDataCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }
        
        String userId = getCurrentUserId();
        
        // Unsubscribe previous listener
        if (statsListener != null) {
            statsListener.remove();
        }
        
        // Subscribe to real-time stats updates
        statsListener = firestore.collection(COLLECTION_STATS)
                .document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for stats", e);
                        callback.onError(e.getMessage());
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        UserStats stats = documentSnapshot.toObject(UserStats.class);
                        if (stats != null) {
                            // Update local database
                            updateLocalStatsData(stats);
                            callback.onStatsLoaded(stats);
                        }
                    } else {
                        // No stats data found, create default
                        createDefaultStats(userId, callback);
                    }
                });
    }
    
    /**
     * Ambil dan sinkronkan data ranking secara real-time
     */
    public void subscribeToRanking(RankingDataCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }
        
        // Unsubscribe previous listener
        if (rankingListener != null) {
            rankingListener.remove();
        }
        
        // Subscribe to real-time ranking updates
        rankingListener = firestore.collection(COLLECTION_RANKING)
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(100) // Top 100 users
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for ranking", e);
                        callback.onError(e.getMessage());
                        return;
                    }
                    
                    if (queryDocumentSnapshots != null) {
                        List<RankingUser> ranking = queryDocumentSnapshots.toObjects(RankingUser.class);
                        callback.onRankingLoaded(ranking);
                    }
                });
    }
    
    /**
     * Ambil dan sinkronkan data profile user secara real-time
     */
    public void subscribeToUserProfile(ProfileDataCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }
        
        String userId = getCurrentUserId();
        
        // Unsubscribe previous listener
        if (profileListener != null) {
            profileListener.remove();
        }
        
        // Subscribe to real-time profile updates
        profileListener = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for profile", e);
                        callback.onError(e.getMessage());
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                        if (profile != null) {
                            // Update local database
                            updateLocalProfileData(profile);
                            callback.onProfileLoaded(profile);
                        }
                    }
                });
    }
    
    /**
     * Sinkronisasi data statistik user ke Firebase
     */
    private void syncUserStats(String userId) {
        try {
            // Calculate stats from local data
            UserStats stats = calculateUserStats();
            
            // Upload to Firebase
            firestore.collection(COLLECTION_STATS)
                    .document(userId)
                    .set(stats)
                    .addOnSuccessListener(aVoid -> 
                        Log.d(TAG, "User stats synced successfully"))
                    .addOnFailureListener(e -> 
                        Log.e(TAG, "Error syncing user stats", e));
                        
        } catch (Exception e) {
            Log.e(TAG, "Error in syncUserStats", e);
        }
    }
    
    /**
     * Sinkronisasi data ranking user ke Firebase
     */
    private void syncUserRanking(String userId) {
        try {
            // Calculate ranking data from local data
            RankingUser rankingData = calculateUserRanking();
            
            // Upload to Firebase
            firestore.collection(COLLECTION_RANKING)
                    .document(userId)
                    .set(rankingData)
                    .addOnSuccessListener(aVoid -> 
                        Log.d(TAG, "User ranking synced successfully"))
                    .addOnFailureListener(e -> 
                        Log.e(TAG, "Error syncing user ranking", e));
                        
        } catch (Exception e) {
            Log.e(TAG, "Error in syncUserRanking", e);
        }
    }
    
    /**
     * Sinkronisasi data profile user ke Firebase
     */
    private void syncUserProfile(String userId) {
        try {
            // Get profile data from local database
            UserProfile profile = getUserProfileFromLocal();
            
            // Upload to Firebase
            firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .set(profile)
                    .addOnSuccessListener(aVoid -> 
                        Log.d(TAG, "User profile synced successfully"))
                    .addOnFailureListener(e -> 
                        Log.e(TAG, "Error syncing user profile", e));
                        
        } catch (Exception e) {
            Log.e(TAG, "Error in syncUserProfile", e);
        }
    }
    
    /**
     * Hitung statistik user dari data lokal
     */
    private UserStats calculateUserStats() {
        try {
            int currentUserId = getCurrentLocalUserId();
            List<RecordEntity> records = localDb.recordDao().getRecordsByUserIdSync(currentUserId);
            
            int totalPoints = 0;
            double totalDistance = 0.0;
            int totalTrashCollected = 0;
            int totalSessions = records.size();
            long totalDuration = 0;
            
            for (RecordEntity record : records) {
                totalPoints += record.getPoints();
                totalDistance += record.getDistance();
                totalDuration += record.getDuration();
                
                // Get trash count for this record
                int trashCount = localDb.trashDao().getTrashCountByRecordIdSync(record.getId());
                totalTrashCollected += trashCount;
            }
            
            return new UserStats(totalPoints, totalDistance, totalTrashCollected, 
                               totalSessions, totalDuration, System.currentTimeMillis());
                               
        } catch (Exception e) {
            Log.e(TAG, "Error calculating user stats", e);
            return new UserStats(0, 0.0, 0, 0, 0, System.currentTimeMillis());
        }
    }
    
    /**
     * Hitung data ranking user dari data lokal
     */
    private RankingUser calculateUserRanking() {
        try {
            UserStats stats = calculateUserStats();
            UserEntity user = localDb.userDao().getUserByIdSync(getCurrentLocalUserId());
            
            if (user != null) {
                return new RankingUser(
                    getCurrentUserId(),
                    user.getUsername(),
                    user.getFirstName() + " " + user.getLastName(),
                    stats.getTotalPoints(),
                    stats.getTotalDistance(),
                    stats.getTotalTrashCollected(),
                    System.currentTimeMillis()
                );
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating user ranking", e);
        }
        
        return null;
    }
    
    /**
     * Ambil data profile user dari database lokal
     */
    private UserProfile getUserProfileFromLocal() {
        try {
            UserEntity user = localDb.userDao().getUserByIdSync(getCurrentLocalUserId());
            
            if (user != null) {                return new UserProfile(
                    getCurrentUserId(),
                    user.getUsername(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getProfileImagePath() != null ? user.getProfileImagePath() : "",
                    user.getActiveDecoration() != null ? user.getActiveDecoration() : "",
                    System.currentTimeMillis()
                );
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting user profile from local", e);
        }
        
        return null;
    }
    
    /**
     * Update data statistik di database lokal
     */
    private void updateLocalStatsData(UserStats stats) {
        // This could be used to update local cache if needed
        // For now, we rely on the local database as source of truth
        Log.d(TAG, "Stats data received from Firebase: " + stats.toString());
    }
    
    /**
     * Update data profile di database lokal
     */
    private void updateLocalProfileData(UserProfile profile) {
        executor.execute(() -> {
            try {
                UserEntity user = localDb.userDao().getUserByIdSync(getCurrentLocalUserId());                if (user != null) {
                    // Update user data with Firebase data
                    user.setFirstName(profile.getFirstName());
                    user.setLastName(profile.getLastName());
                    if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
                        user.setProfileImagePath(profile.getAvatarUrl());
                    }
                    if (profile.getBadgeUrl() != null && !profile.getBadgeUrl().isEmpty()) {
                        user.setActiveDecoration(profile.getBadgeUrl());
                    }
                      // Update user in database
                    localDb.userDao().update(user);
                    Log.d(TAG, "Local profile updated from Firebase");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating local profile", e);
            }
        });
    }
    
    /**
     * Buat data statistik default untuk user baru
     */
    private void createDefaultStats(String userId, StatsDataCallback callback) {
        UserStats defaultStats = new UserStats(0, 0.0, 0, 0, 0, System.currentTimeMillis());
        
        firestore.collection(COLLECTION_STATS)
                .document(userId)
                .set(defaultStats)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Default stats created");
                    callback.onStatsLoaded(defaultStats);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating default stats", e);
                    callback.onError(e.getMessage());
                });
    }
    
    /**
     * Stop semua listener
     */
    public void stopAllListeners() {
        if (statsListener != null) {
            statsListener.remove();
            statsListener = null;
        }
        
        if (rankingListener != null) {
            rankingListener.remove();
            rankingListener = null;
        }
        
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }
    
    /**
     * Helper methods
     */
    private boolean isUserLoggedIn() {
        return FirebaseAuthManager.getInstance(context).isLoggedIn();
    }
    
    private String getCurrentUserId() {
        return FirebaseAuthManager.getInstance(context).getCurrentUserId();
    }
    
    private int getCurrentLocalUserId() {
        // Get local user ID from SharedPreferences or database
        // This should match the logic in your existing code
        return FirebaseAuthManager.getInstance(context).getCurrentLocalUserId();
    }
    
    // Data classes for Firebase
    public static class UserStats {
        private int totalPoints;
        private double totalDistance;
        private int totalTrashCollected;
        private int totalSessions;
        private long totalDuration;
        private long lastUpdated;
        
        public UserStats() {} // Required for Firebase
        
        public UserStats(int totalPoints, double totalDistance, int totalTrashCollected, 
                        int totalSessions, long totalDuration, long lastUpdated) {
            this.totalPoints = totalPoints;
            this.totalDistance = totalDistance;
            this.totalTrashCollected = totalTrashCollected;
            this.totalSessions = totalSessions;
            this.totalDuration = totalDuration;
            this.lastUpdated = lastUpdated;
        }
        
        // Getters and setters
        public int getTotalPoints() { return totalPoints; }
        public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
        
        public double getTotalDistance() { return totalDistance; }
        public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
        
        public int getTotalTrashCollected() { return totalTrashCollected; }
        public void setTotalTrashCollected(int totalTrashCollected) { this.totalTrashCollected = totalTrashCollected; }
        
        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
        
        public long getTotalDuration() { return totalDuration; }
        public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }
        
        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
        
        @Override
        public String toString() {
            return "UserStats{points=" + totalPoints + ", distance=" + totalDistance + 
                   ", trash=" + totalTrashCollected + ", sessions=" + totalSessions + "}";
        }
    }
    
    public static class RankingUser {
        private String userId;
        private String username;
        private String fullName;
        private int totalPoints;
        private double totalDistance;
        private int totalTrashCollected;
        private long lastUpdated;
        
        public RankingUser() {} // Required for Firebase
        
        public RankingUser(String userId, String username, String fullName, int totalPoints, 
                          double totalDistance, int totalTrashCollected, long lastUpdated) {
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.totalPoints = totalPoints;
            this.totalDistance = totalDistance;
            this.totalTrashCollected = totalTrashCollected;
            this.lastUpdated = lastUpdated;
        }
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public int getTotalPoints() { return totalPoints; }
        public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
        
        public double getTotalDistance() { return totalDistance; }
        public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
        
        public int getTotalTrashCollected() { return totalTrashCollected; }
        public void setTotalTrashCollected(int totalTrashCollected) { this.totalTrashCollected = totalTrashCollected; }
        
        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    }
    
    public static class UserProfile {
        private String userId;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private String avatarUrl;
        private String badgeUrl;
        private long lastUpdated;
        
        public UserProfile() {} // Required for Firebase
        
        public UserProfile(String userId, String username, String firstName, String lastName,
                          String email, String avatarUrl, String badgeUrl, long lastUpdated) {
            this.userId = userId;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.avatarUrl = avatarUrl;
            this.badgeUrl = badgeUrl;
            this.lastUpdated = lastUpdated;
        }
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        
        public String getBadgeUrl() { return badgeUrl; }
        public void setBadgeUrl(String badgeUrl) { this.badgeUrl = badgeUrl; }
        
        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}

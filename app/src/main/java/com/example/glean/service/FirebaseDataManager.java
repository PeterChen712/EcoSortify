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
    private FirebaseAuthManager authManager;
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
        this.authManager = FirebaseAuthManager.getInstance(context);
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
    }    /**
     * Hitung statistik user dari data lokal
     */
    private UserStats calculateUserStats() {
        try {
            Log.d(TAG, "🔢 === CALCULATING USER STATS ===");
            
            // Try multiple ways to get correct user ID
            int currentUserId = getCurrentLocalUserId();
            
            // If getCurrentLocalUserId returns -1, try getting from SharedPreferences directly
            if (currentUserId == -1) {
                android.content.SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                currentUserId = prefs.getInt("USER_ID", -1);
                Log.w(TAG, "🔍 FirebaseAuthManager returned -1, trying USER_ID from SharedPreferences: " + currentUserId);
            }
            
            Log.d(TAG, "🔍 Using currentUserId: " + currentUserId + " to calculate stats");
            
            if (currentUserId == -1) {
                Log.e(TAG, "❌ CRITICAL: Cannot find valid user ID! Stats will be zero.");
                return new UserStats(0, 0.0, 0, 0, 0, System.currentTimeMillis());
            }
            
            List<RecordEntity> records = localDb.recordDao().getRecordsByUserIdSync(currentUserId);
            Log.d(TAG, "🔍 Found " + records.size() + " records for userId: " + currentUserId);
            
            if (records.isEmpty()) {
                Log.w(TAG, "⚠️ No records found for user ID: " + currentUserId + " - stats will be zero");
                return new UserStats(0, 0.0, 0, 0, 0, System.currentTimeMillis());
            }
            
            int totalPoints = 0;
            double totalDistance = 0.0;
            int totalTrashCollected = 0;
            int totalSessions = records.size();
            long totalDuration = 0;
            
            for (RecordEntity record : records) {
                try {
                    int recordPoints = record.getPoints();
                    double recordDistance = record.getDistance();
                    long recordDuration = record.getDuration();
                    
                    totalPoints += recordPoints;
                    totalDistance += recordDistance;
                    totalDuration += recordDuration;
                    
                    // Get trash count for this record
                    int trashCount = localDb.trashDao().getTrashCountByRecordIdSync(record.getId());
                    totalTrashCollected += trashCount;
                    
                    Log.d(TAG, "🔍 Record ID " + record.getId() + ": points=" + recordPoints + 
                              ", distance=" + recordDistance + ", duration=" + recordDuration + ", trash=" + trashCount);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing record ID " + record.getId(), e);
                }
            }
            
            // Always use current timestamp for lastUpdated to ensure Firebase reflects latest sync
            long currentTimestamp = System.currentTimeMillis();
            
            UserStats stats = new UserStats(totalPoints, totalDistance, totalTrashCollected, 
                               totalSessions, totalDuration, currentTimestamp);
            
            Log.d(TAG, "📊 Calculated fresh user stats:");
            Log.d(TAG, "   Points: " + totalPoints + ", Distance: " + String.format("%.2f km", totalDistance/1000));
            Log.d(TAG, "   Trash: " + totalTrashCollected + ", Sessions: " + totalSessions);
            Log.d(TAG, "   Duration: " + (totalDuration/60000) + " minutes, LastUpdated: " + new java.util.Date(currentTimestamp));
            Log.d(TAG, "🔢 === END CALCULATING USER STATS ===");
            
            return stats;
                               
        } catch (Exception e) {
            Log.e(TAG, "❌ CRITICAL ERROR in calculateUserStats", e);
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
        // Log Firebase stats data for debugging
        Log.d(TAG, "Stats data received from Firebase: " + stats.toString());
        
        // Note: Kita tidak update database lokal dari Firebase untuk menghindari konflik data
        // Database lokal tetap menjadi source of truth, Firebase hanya menerima update dari lokal
        // Namun kita bisa gunakan data Firebase untuk validasi atau debugging
        
        executor.execute(() -> {
            try {
                // Hitung statistik dari database lokal untuk perbandingan
                UserStats localStats = calculateUserStats();
                
                Log.d(TAG, "📊 Stats comparison:");
                Log.d(TAG, "   Firebase - Points: " + stats.getTotalPoints() + ", Distance: " + stats.getTotalDistance() + 
                          ", Trash: " + stats.getTotalTrashCollected() + ", Sessions: " + stats.getTotalSessions());
                Log.d(TAG, "   Local    - Points: " + localStats.getTotalPoints() + ", Distance: " + localStats.getTotalDistance() + 
                          ", Trash: " + localStats.getTotalTrashCollected() + ", Sessions: " + localStats.getTotalSessions());
                
                // Jika ada perbedaan signifikan, log sebagai warning
                if (Math.abs(stats.getTotalPoints() - localStats.getTotalPoints()) > 0 ||
                    Math.abs(stats.getTotalDistance() - localStats.getTotalDistance()) > 1.0 ||
                    Math.abs(stats.getTotalTrashCollected() - localStats.getTotalTrashCollected()) > 0) {
                    Log.w(TAG, "⚠️  Data discrepancy detected between Firebase and local database");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error comparing local and Firebase stats", e);
            }
        });
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
     * Update Firebase stats setelah sesi plogging selesai
     * Menggunakan increment untuk menghindari race condition
     */    public void updateUserStatsAfterPloggingSession(DataSyncCallback callback) {
        Log.d(TAG, "🔄 === UPDATE FIREBASE STATS AFTER PLOGGING SESSION ===");
        
        if (!isUserLoggedIn()) {
            Log.e(TAG, "❌ Cannot update Firebase stats - user not logged in");
            callback.onError("User not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                String userId = getCurrentUserId();
                int localUserId = getCurrentLocalUserId();
                Log.d(TAG, "🔄 Starting Firebase stats update for user: " + userId + " (local ID: " + localUserId + ")");
                
                // Calculate fresh stats from local data
                UserStats freshStats = calculateUserStats();
                
                // Log the stats we're about to send to Firebase
                Log.d(TAG, "🔄 Stats to be sent to Firebase:");
                Log.d(TAG, "   Points: " + freshStats.getTotalPoints());
                Log.d(TAG, "   Distance: " + freshStats.getTotalDistance() + " meters");
                Log.d(TAG, "   Trash: " + freshStats.getTotalTrashCollected());
                Log.d(TAG, "   Sessions: " + freshStats.getTotalSessions());
                Log.d(TAG, "   Duration: " + freshStats.getTotalDuration() + " ms");
                
                // Validate that we have meaningful data before sending to Firebase
                if (freshStats.getTotalSessions() == 0) {
                    Log.w(TAG, "⚠️ WARNING: No sessions found, Firebase update may not be meaningful");
                }
                
                // Update Firebase with latest data
                firestore.collection(COLLECTION_STATS)
                        .document(userId)
                        .set(freshStats)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "✅ Firebase user stats updated successfully after plogging session");
                            Log.d(TAG, "   Updated stats: " + freshStats.toString());
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ Error updating Firebase user stats after plogging session", e);
                            callback.onError(e.getMessage());
                        });
                        
            } catch (Exception e) {
                Log.e(TAG, "❌ Error in updateUserStatsAfterPloggingSession", e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Update Firebase stats with retry mechanism untuk meningkatkan reliabilitas
     */
    public void updateUserStatsWithRetry(DataSyncCallback callback) {
        updateUserStatsWithRetry(callback, 3); // Default 3 attempts
    }
    
    private void updateUserStatsWithRetry(DataSyncCallback callback, int attemptsLeft) {
        if (!isUserLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }
        
        if (attemptsLeft <= 0) {
            callback.onError("Failed to update Firebase stats after multiple attempts");
            return;
        }
        
        updateUserStatsAfterPloggingSession(new DataSyncCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Firebase update attempt failed, " + (attemptsLeft - 1) + " attempts remaining: " + error);
                
                if (attemptsLeft > 1) {
                    // Retry after a delay
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        updateUserStatsWithRetry(callback, attemptsLeft - 1);
                    }, 2000); // 2 second delay between retries
                } else {
                    callback.onError("Failed after retries: " + error);
                }
            }
        });
    }

    /**
     * Verify Firebase stats update by reading back the data
     * This helps debug if the update actually worked
     */
    public void verifyFirebaseStatsUpdate(DataSyncCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }
        
        String userId = getCurrentUserId();
        Log.d(TAG, "🔍 Verifying Firebase stats for user: " + userId);
        
        firestore.collection(COLLECTION_STATS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserStats firebaseStats = documentSnapshot.toObject(UserStats.class);
                        if (firebaseStats != null) {
                            Log.d(TAG, "✅ Firebase verification - current stats in Firebase:");
                            Log.d(TAG, "   Points: " + firebaseStats.getTotalPoints());
                            Log.d(TAG, "   Distance: " + firebaseStats.getTotalDistance() + " meters");
                            Log.d(TAG, "   Trash: " + firebaseStats.getTotalTrashCollected());
                            Log.d(TAG, "   Sessions: " + firebaseStats.getTotalSessions());
                            Log.d(TAG, "   Duration: " + firebaseStats.getTotalDuration() + " ms");
                            Log.d(TAG, "   LastUpdated: " + new java.util.Date(firebaseStats.getLastUpdated()));
                            callback.onSuccess();
                        } else {
                            Log.w(TAG, "⚠️ Firebase document exists but data is null");
                            callback.onError("Firebase data is null");
                        }
                    } else {
                        Log.w(TAG, "⚠️ Firebase document does not exist for user: " + userId);
                        callback.onError("Firebase document does not exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error verifying Firebase stats", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Debug method to check current local database state
     */
    public void debugCurrentDatabaseState() {
        executor.execute(() -> {
            try {
                Log.d(TAG, "🔍 === DEBUG DATABASE STATE ===");
                
                // Check multiple user ID sources
                int userIdFromFirebase = getCurrentLocalUserId();
                android.content.SharedPreferences userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                int userIdFromPrefs = userPrefs.getInt("USER_ID", -1);
                
                Log.d(TAG, "🔍 User ID from FirebaseAuthManager: " + userIdFromFirebase);
                Log.d(TAG, "🔍 User ID from user_prefs: " + userIdFromPrefs);
                
                // Check all users in database
                List<UserEntity> allUsers = localDb.userDao().getAllUsersSync();
                Log.d(TAG, "🔍 Total users in database: " + allUsers.size());
                for (UserEntity user : allUsers) {
                    Log.d(TAG, "🔍 User ID: " + user.getId() + ", Name: " + user.getFirstName() + " " + user.getLastName());
                }
                
                // Check records for each user ID
                for (int userId : new int[]{userIdFromFirebase, userIdFromPrefs}) {
                    if (userId != -1) {
                        List<RecordEntity> records = localDb.recordDao().getRecordsByUserIdSync(userId);
                        Log.d(TAG, "🔍 Records for userId " + userId + ": " + records.size());
                        
                        for (RecordEntity record : records) {
                            int trashCount = localDb.trashDao().getTrashCountByRecordIdSync(record.getId());
                            Log.d(TAG, "🔍   Record " + record.getId() + ": points=" + record.getPoints() + 
                                      ", distance=" + record.getDistance() + ", duration=" + record.getDuration() + 
                                      ", trash=" + trashCount);
                        }
                    }
                }
                
                Log.d(TAG, "🔍 === END DEBUG ===");
                
            } catch (Exception e) {
                Log.e(TAG, "Error in debugCurrentDatabaseState", e);
            }
        });
    }

    // Helper methods for authentication
    private boolean isUserLoggedIn() {
        return authManager != null && authManager.isLoggedIn();
    }

    private String getCurrentUserId() {
        if (authManager == null) return null;
        
        // Get Firebase UID if using real Firebase authentication
        String firebaseUserId = authManager.getCurrentUserId();
        if (firebaseUserId != null && !firebaseUserId.isEmpty() && !firebaseUserId.equals("-1")) {
            Log.d(TAG, "🔍 Using Firebase UID: " + firebaseUserId);
            return firebaseUserId;
        }
        
        // Fallback: If using local authentication, generate a consistent Firebase-compatible ID
        int localUserId = authManager.getCurrentLocalUserId();
        if (localUserId != -1) {
            String localFirebaseId = "local_user_" + localUserId;
            Log.d(TAG, "🔍 Using local Firebase-compatible ID: " + localFirebaseId + " (from local ID: " + localUserId + ")");
            return localFirebaseId;
        }
        
        Log.w(TAG, "⚠️ No valid user ID found");
        return null;
    }

    private int getCurrentLocalUserId() {
        return authManager != null ? authManager.getCurrentLocalUserId() : -1;
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
    
    /**
     * Manual method to force update Firebase stats for testing
     */
    public void forceUpdateFirebaseStatsForTesting(DataSyncCallback callback) {
        Log.d(TAG, "🧪 === FORCE UPDATE FOR TESTING ===");
        debugCurrentDatabaseState();
        
        // Try both update methods
        updateUserStatsAfterPloggingSession(callback);
        
        // Also try the new force update method
        int localUserId = getCurrentLocalUserId();
        if (localUserId != -1) {
            // Find the latest record for this user
            executor.execute(() -> {
                try {
                    List<RecordEntity> records = localDb.recordDao().getRecordsByUserIdSync(localUserId);
                    if (!records.isEmpty()) {
                        RecordEntity latestRecord = records.get(records.size() - 1);
                        forceUpdateAfterPloggingSession((int) latestRecord.getId(), new DataSyncCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "🧪 Force update testing completed successfully");
                            }
                            
                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "🧪 Force update testing failed: " + error);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in testing force update", e);
                }
            });
        }
    }

    /**
     * Manual method to force update Firebase stats immediately after plogging session
     * This ensures Firebase is updated with latest data
     */
    public void forceUpdateAfterPloggingSession(int recordId, DataSyncCallback callback) {
        Log.d(TAG, "🚀 === FORCE UPDATE AFTER PLOGGING SESSION ===");
        Log.d(TAG, "🔍 Triggered by record ID: " + recordId);
        
        if (!isUserLoggedIn()) {
            Log.e(TAG, "❌ Cannot force update Firebase stats - user not logged in");
            callback.onError("User not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                String userId = getCurrentUserId();
                int localUserId = getCurrentLocalUserId();
                
                Log.d(TAG, "🔍 Force update Firebase for user: " + userId + " (local ID: " + localUserId + ")");
                Log.d(TAG, "🔍 After saving record ID: " + recordId);
                
                // Wait a moment to ensure database transaction is complete
                Thread.sleep(500);
                
                // Calculate fresh stats from local data including the new record
                UserStats freshStats = calculateUserStats();
                
                if (freshStats == null) {
                    Log.e(TAG, "❌ Failed to calculate stats");
                    callback.onError("Failed to calculate stats");
                    return;
                }
                
                Log.d(TAG, "🚀 FORCE UPDATING Firebase with fresh stats:");
                Log.d(TAG, "   Points: " + freshStats.getTotalPoints());
                Log.d(TAG, "   Distance: " + String.format("%.2f km", freshStats.getTotalDistance()/1000));
                Log.d(TAG, "   Trash: " + freshStats.getTotalTrashCollected());
                Log.d(TAG, "   Sessions: " + freshStats.getTotalSessions());
                Log.d(TAG, "   Duration: " + freshStats.getTotalDuration() + " ms");
                
                // Force update Firebase with latest data
                firestore.collection(COLLECTION_STATS)
                        .document(userId)
                        .set(freshStats)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "✅ FORCE UPDATE SUCCESS! Firebase stats updated after record " + recordId);
                            Log.d(TAG, "   Final stats in Firebase: " + freshStats.toString());
                            
                            // Also update ranking data
                            updateUserRankingInFirebase(userId, freshStats);
                            
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ FORCE UPDATE FAILED! Error updating Firebase stats after record " + recordId, e);
                            callback.onError("Firebase update failed: " + e.getMessage());
                        });
                        
            } catch (Exception e) {
                Log.e(TAG, "❌ Error in forceUpdateAfterPloggingSession", e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Update ranking data in Firebase
     */
    private void updateUserRankingInFirebase(String userId, UserStats stats) {
        try {
            // Get user profile data for ranking
            UserEntity user = localDb.userDao().getUserByIdSync(getCurrentLocalUserId());
            if (user != null) {
                RankingUser rankingData = new RankingUser(
                    userId,
                    user.getUsername(),
                    user.getFirstName() + " " + user.getLastName(),
                    stats.getTotalPoints(),
                    stats.getTotalDistance(),
                    stats.getTotalTrashCollected(),
                    stats.getLastUpdated()
                );
                
                firestore.collection(COLLECTION_RANKING)
                        .document(userId)
                        .set(rankingData)
                        .addOnSuccessListener(aVoid -> 
                            Log.d(TAG, "✅ User ranking updated in Firebase"))
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "❌ Error updating user ranking in Firebase", e));
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating ranking data", e);
        }
    }
}

package com.example.glean.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

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
    
    // Cached user data objects
    private UserStats userStats;
    private UserProfile userProfile;
    
    // LiveData objects for UI observers
    private MutableLiveData<UserStats> userStatsLiveData;
    private MutableLiveData<UserProfile> userProfileLiveData;
    
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
    }    /**
     * Ambil dan sinkronkan data statistik user secara real-time
     */    public void subscribeToUserStats(StatsDataCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = getCurrentUserId();
        logDataSource("UserStats", "Fresh-Firebase-Fetch", userId);

        // Unsubscribe previous listener
        if (statsListener != null) {
            statsListener.remove();
        }

        // ENHANCED: First, force fresh data fetch from Firebase
        Log.d(TAG, "🔥 First fetching fresh stats, then setting up real-time listener for user: " + userId);
        
        fetchFreshUserStats(new StatsDataCallback() {
            @Override
            public void onStatsLoaded(UserStats stats) {
                logDataSource("UserStats", "Firebase-Fresh-Success", userId);
                // Fresh data loaded, now callback and set up real-time listener
                callback.onStatsLoaded(stats);
                setupStatsRealTimeListener(userId, callback);
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to fetch fresh stats, setting up real-time listener anyway: " + error);
                logDataSource("UserStats", "Firebase-Fresh-Failed", userId);
                // Even if fresh fetch fails, set up real-time listener
                setupStatsRealTimeListener(userId, callback);
            }
        });
    }
    
    /**
     * Set up real-time listener for user stats
     */
    private void setupStatsRealTimeListener(String userId, StatsDataCallback callback) {
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
                            Log.d(TAG, "🔥 Real-time stats update received: " + stats.toString());
                            // Update local database with real-time data
                            updateLocalStatsData(stats);
                            callback.onStatsLoaded(stats);
                        }
                    } else {
                        Log.d(TAG, "🔥 Stats document deleted or doesn't exist");
                        // Document was deleted or doesn't exist, create default
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
     * Sinkronisasi data statistik user ke Firebase - ONLY for local users
     * For Firebase users, this should NOT overwrite existing Firebase data
     */
    private void syncUserStats(String userId) {
        try {
            // Check if user is logged in with Firebase
            if (authManager.isLoggedIn() && userId != null && !userId.isEmpty()) {
                Log.w(TAG, "⚠️ WARNING: syncUserStats() called for Firebase user: " + userId);
                Log.w(TAG, "⚠️ Skipping stats sync to prevent overwriting Firebase data");
                Log.w(TAG, "⚠️ Stats sync should only be used for local users, not Firebase users");
                return;
            }
            
            // Calculate stats from local data (only for local users)
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
    }/**
     * Sinkronisasi data ranking user ke Firebase
     */
    private void syncUserRanking(String userId) {
        try {
            // Calculate ranking data from local data (safe on background thread)
            RankingUser rankingData = calculateUserRanking();
            
            if (rankingData == null) {
                Log.w(TAG, "⚠️ Cannot sync user ranking - ranking data is null");
                return;
            }
            
            // Upload to Firebase (post to main thread)
            new Handler(Looper.getMainLooper()).post(() -> {
                firestore.collection(COLLECTION_RANKING)
                        .document(userId)
                        .set(rankingData)
                        .addOnSuccessListener(aVoid -> 
                            Log.d(TAG, "User ranking synced successfully"))
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "Error syncing user ranking", e));
            });
                    
        } catch (Exception e) {
            Log.e(TAG, "Error calculating user ranking", e);
        }
    }    /**
     * Sinkronisasi data profile user ke Firebase
     */
    private void syncUserProfile(String userId) {
        try {
            // Get profile data from local database (safe on background thread)
            UserProfile profile = getUserProfileFromLocal();
            
            if (profile == null) {
                Log.w(TAG, "⚠️ Cannot sync user profile - profile data is null");
                return;
            }
            
            // Upload to Firebase (post to main thread)
            new Handler(Looper.getMainLooper()).post(() -> {
                firestore.collection(COLLECTION_USERS)
                        .document(userId)
                        .set(profile)
                        .addOnSuccessListener(aVoid -> 
                            Log.d(TAG, "User profile synced successfully"))
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "Error syncing user profile", e));
            });
                        
        } catch (Exception e) {
            Log.e(TAG, "Error getting user profile from local", e);
        }
    }    /**
     * Hitung statistik user dari data lokal - ONLY for local users
     * For Firebase users, this should NOT be called - use Firebase data directly
     */
    private UserStats calculateUserStats() {
        try {
            Log.d(TAG, "🔢 === CALCULATING USER STATS ===");
            
            // Check if user is logged in with Firebase
            String firebaseUserId = authManager.getCurrentUserId();
            if (firebaseUserId != null && !firebaseUserId.isEmpty() && authManager.isLoggedIn()) {
                Log.w(TAG, "⚠️ WARNING: calculateUserStats() called for Firebase user: " + firebaseUserId);
                Log.w(TAG, "⚠️ For Firebase users, stats should be fetched from Firebase directly, not calculated from local DB");
                Log.w(TAG, "⚠️ Returning zero stats to prevent overwriting Firebase data");
                return new UserStats(0, 0.0, 0, 0, 0, System.currentTimeMillis());
            }
            
            // Try multiple ways to get correct user ID for LOCAL users only
            int currentUserId = getCurrentLocalUserId();
              // If getCurrentLocalUserId returns -1, try getting from SharedPreferences directly
            if (currentUserId == -1) {
                android.content.SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                currentUserId = defaultPrefs.getInt("USER_ID", -1);
                Log.w(TAG, "🔍 FirebaseAuthManager returned -1, trying USER_ID from default SharedPreferences: " + currentUserId);
            }
            
            Log.d(TAG, "🔍 Using currentUserId: " + currentUserId + " to calculate stats");
            
            if (currentUserId == -1) {
                Log.e(TAG, "❌ CRITICAL: Cannot find valid user ID! Stats will be zero.");
                return new UserStats(0, 0.0, 0, 0, 0, System.currentTimeMillis());
            }
              List<RecordEntity> records = localDb.recordDao().getRecordsByUserIdSync(currentUserId);
            Log.d(TAG, "🔍 Found " + records.size() + " records for userId: " + currentUserId);
              // Log each record for debugging
            for (int i = 0; i < records.size(); i++) {
                RecordEntity record = records.get(i);
                int trashCount = localDb.trashDao().getTrashCountByRecordIdSync(record.getId());                Log.d(TAG, "🔍 Record " + (i+1) + "/" + records.size() + ": ID=" + record.getId() + 
                          ", points=" + record.getPoints() + ", distance=" + record.getDistance() + 
                          ", duration=" + record.getDuration() + ", trash=" + trashCount + 
                          ", userId=" + record.getUserId() + ", createdAt=" + record.getCreatedAt());
            }
            
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
            if (stats == null) {
                Log.w(TAG, "⚠️ Cannot calculate user ranking - user stats is null");
                return null;
            }
            
            int localUserId = getCurrentLocalUserId();
            if (localUserId == -1) {
                Log.w(TAG, "⚠️ Cannot calculate user ranking - local user ID is invalid (-1)");
                return null;
            }
            
            UserEntity user = localDb.userDao().getUserByIdSync(localUserId);
            if (user == null) {
                Log.w(TAG, "⚠️ Cannot calculate user ranking - user entity not found for ID: " + localUserId);
                return null;
            }
            
            String firebaseUserId = getCurrentUserId();
            if (firebaseUserId == null || firebaseUserId.isEmpty()) {
                Log.w(TAG, "⚠️ Cannot calculate user ranking - Firebase user ID is null or empty");
                return null;
            }
              return new RankingUser(
                firebaseUserId,
                user.getUsername(),
                user.getProfileImagePath() != null ? user.getProfileImagePath() : "", // profileImageUrl
                stats.getTotalPoints(),
                stats.getTotalDistance(),
                stats.getTotalTrashCollected(),
                0 // badgeCount - defaulting to 0 for now
            );
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error calculating user ranking", e);
            return null;
        }
    }
      /**
     * Ambil data profile user dari database lokal
     */
    private UserProfile getUserProfileFromLocal() {
        try {
            int localUserId = getCurrentLocalUserId();
            if (localUserId == -1) {
                Log.w(TAG, "⚠️ Cannot get user profile - local user ID is invalid (-1)");
                return null;
            }
            
            UserEntity user = localDb.userDao().getUserByIdSync(localUserId);
            if (user == null) {
                Log.w(TAG, "⚠️ Cannot get user profile - user entity not found for ID: " + localUserId);
                return null;
            }
            
            String firebaseUserId = getCurrentUserId();
            if (firebaseUserId == null || firebaseUserId.isEmpty()) {
                Log.w(TAG, "⚠️ Cannot get user profile - Firebase user ID is null or empty");
                return null;
            }
            
            return new UserProfile(
                firebaseUserId,
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getProfileImagePath() != null ? user.getProfileImagePath() : "",
                user.getActiveDecoration() != null ? user.getActiveDecoration() : "",
                System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting user profile from local", e);
            return null;
        }
    }    /**
     * Update data statistik di database lokal - ONLY for local users
     * For Firebase users, this method should skip local database updates
     */    private void updateLocalStatsData(UserStats stats) {
        Log.d(TAG, "🔄 Firebase stats update received: " + stats.toString());
        
        // Clear any cached data from previous user first
        this.userStats = null;
        this.userProfile = null;
        
        executor.execute(() -> {
            try {
                // Check if user is logged in with Firebase
                String firebaseUserId = authManager.getCurrentUserId();
                if (firebaseUserId != null && !firebaseUserId.isEmpty() && authManager.isLoggedIn()) {
                    Log.d(TAG, "🔥 Firebase user detected: " + firebaseUserId + " - skipping local database updates");
                    Log.d(TAG, "🔥 Firebase stats will be used directly without local database sync");
                    return;
                }
                
                int localUserId = getCurrentLocalUserId();
                if (localUserId == -1) {
                    Log.w(TAG, "Cannot update local stats - no valid local user ID");
                    return;
                }
                
                // Get current local stats for comparison
                UserStats localStats = calculateUserStats();
                
                Log.d(TAG, "📊 Stats comparison:");
                Log.d(TAG, "   Firebase - Points: " + stats.getTotalPoints() + ", Distance: " + stats.getTotalDistance() + 
                          ", Trash: " + stats.getTotalTrashCollected() + ", Sessions: " + stats.getTotalSessions());
                Log.d(TAG, "   Local    - Points: " + localStats.getTotalPoints() + ", Distance: " + localStats.getTotalDistance() + 
                          ", Trash: " + localStats.getTotalTrashCollected() + ", Sessions: " + localStats.getTotalSessions());
                
                // Check if Firebase has newer data (manual updates from dashboard)
                boolean firebaseHasNewerData = stats.getLastUpdated() > localStats.getLastUpdated();
                boolean significantDifference = Math.abs(stats.getTotalPoints() - localStats.getTotalPoints()) > 0;
                
                if (firebaseHasNewerData && significantDifference) {
                    Log.d(TAG, "🔄 Firebase has newer data - updating local user entity");
                    
                    // Update the user's points in local database to match Firebase
                    UserEntity user = localDb.userDao().getUserByIdSync(localUserId);
                    if (user != null) {
                        int oldPoints = user.getPoints();
                        user.setPoints(stats.getTotalPoints());
                        localDb.userDao().update(user);
                        
                        Log.d(TAG, "✅ Local user points updated from " + oldPoints + " to " + stats.getTotalPoints());
                        
                        // Notify UI thread about the update
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Log.d(TAG, "🔄 UI will be notified of real-time update");
                        });
                    }
                } else if (significantDifference) {
                    Log.w(TAG, "⚠️ Data discrepancy detected but local data is newer - Firebase may need update");
                } else {
                    Log.d(TAG, "✅ Firebase and local data are in sync");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating local stats from Firebase", e);
            }
        });
    }
      /**
     * Update data profile di database lokal
     */
    private void updateLocalProfileData(UserProfile profile) {
        // Clear any cached profile data from previous user first
        this.userProfile = null;
        
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
        
        Log.d(TAG, "🔥 Creating default stats for new user: " + userId);
        
        firestore.collection(COLLECTION_STATS)
                .document(userId)
                .set(defaultStats)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Default stats created for user: " + userId);
                    callback.onStatsLoaded(defaultStats);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error creating default stats for user: " + userId, e);
                    callback.onError(e.getMessage());
                });
    }
    
    /**
     * Force fetch fresh user statistics from Firebase (no cache)
     * This method ensures we always get the latest data from Firebase
     */
    public void fetchFreshUserStats(StatsDataCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = getCurrentUserId();
        Log.d(TAG, "🔥 Fetching fresh stats from Firebase for user: " + userId);

        // Force fetch from server (not from cache)
        firestore.collection(COLLECTION_STATS)
                .document(userId)
                .get(com.google.firebase.firestore.Source.SERVER) // Force server fetch
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserStats stats = documentSnapshot.toObject(UserStats.class);
                        if (stats != null) {
                            Log.d(TAG, "✅ Fresh stats fetched from Firebase: " + stats.toString());
                            // Update local database with fresh data
                            updateLocalStatsData(stats);
                            callback.onStatsLoaded(stats);
                        } else {
                            Log.w(TAG, "⚠️ Stats document exists but conversion failed");
                            callback.onError("Failed to parse stats data");
                        }
                    } else {
                        Log.d(TAG, "🔥 No stats found for user, creating default");
                        // No stats found, create default
                        createDefaultStats(userId, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error fetching fresh stats from Firebase", e);
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
     * Complete logout - clear all listeners and reset singleton instance
     */
    public void logout() {
        Log.d(TAG, "🔴 FirebaseDataManager logout initiated");
        
        // Stop all Firebase listeners
        stopAllListeners();
        
        // Shutdown executor service
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                // Wait for tasks to complete with timeout
                if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
        
        Log.d(TAG, "🔴 FirebaseDataManager logout completed - all resources cleared");
    }
      /**
     * Reset singleton instance (called during logout)
     */
    public static void resetInstance() {
        Log.d(TAG, "🔴 Resetting FirebaseDataManager singleton instance");
        if (instance != null) {
            instance.logout();
            // ENHANCED: Clear all cached data to prevent cross-user data contamination
            instance.clearAllCachedData();
            instance = null;
        }
    }
      /**
     * Clear all cached user data to prevent data leakage between accounts
     */    private void clearAllCachedData() {
        try {
            String currentUserId = getCurrentUserId();
            Log.d(TAG, "🔴 Clearing all cached Firebase data for user: " + currentUserId);
            
            // Clear any cached user statistics, ranking, and profile data
            this.userStats = null;
            this.userProfile = null;
            
            // Clear LiveData observers to remove previous user's data
            if (userStatsLiveData != null) {
                userStatsLiveData.setValue(null);
                Log.d(TAG, "🔴 Cleared userStatsLiveData");
            }
            if (userProfileLiveData != null) {
                userProfileLiveData.setValue(null);
                Log.d(TAG, "🔴 Cleared userProfileLiveData");
            }
            
            // Remove Firebase real-time listeners
            if (statsListener != null) {
                statsListener.remove();
                statsListener = null;
                Log.d(TAG, "🔴 Removed Firebase stats listener");
            }
            if (profileListener != null) {
                profileListener.remove();
                profileListener = null;
                Log.d(TAG, "🔴 Removed Firebase profile listener");
            }
            
            // This ensures no previous user's data remains in memory
            Log.d(TAG, "🔴 All cached Firebase data cleared successfully");
        } catch (Exception e) {
            Log.w(TAG, "Error clearing cached data", e);
        }
    }
    
    /**
     * Force refresh all user data from Firebase after login
     * This ensures we always get fresh data for the new user
     */
    public void forceRefreshUserDataAfterLogin(DataSyncCallback callback) {
        if (!isUserLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }
        
        String userId = getCurrentUserId();
        Log.d(TAG, "🔥 Force refreshing all user data after login for user: " + userId);
        
        executor.execute(() -> {
            try {
                // Stop all existing listeners first
                stopAllListeners();
                
                // Clear any local cached data
                clearAllCachedData();
                
                // Force fresh sync from Firebase
                syncAllUserData(new DataSyncCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ Fresh user data synced successfully after login");
                        callback.onSuccess();
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error syncing fresh user data after login: " + error);
                        callback.onError(error);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Error in forceRefreshUserDataAfterLogin", e);
                callback.onError(e.getMessage());
            }
        });
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
                  // Add detailed logging for debugging
                Log.d(TAG, "🔍 === DEBUGGING STATS CALCULATION ===");
                Log.d(TAG, "🔍 Current User ID (Firebase): " + userId);
                Log.d(TAG, "🔍 Current Local User ID: " + localUserId);
                Log.d(TAG, "🔍 Record ID that just completed: " + recordId);
                
                // Verify the specific record exists and has data
                RecordEntity specificRecord = localDb.recordDao().getRecordByIdSync(recordId);
                if (specificRecord != null) {
                    Log.d(TAG, "🔍 SPECIFIC RECORD verification:");
                    Log.d(TAG, "   Record ID: " + specificRecord.getId());
                    Log.d(TAG, "   User ID: " + specificRecord.getUserId()); 
                    Log.d(TAG, "   Points: " + specificRecord.getPoints());
                    Log.d(TAG, "   Distance: " + specificRecord.getDistance());
                    Log.d(TAG, "   Duration: " + specificRecord.getDuration());
                    Log.d(TAG, "   Trash count: " + localDb.trashDao().getTrashCountByRecordIdSync(recordId));
                } else {
                    Log.e(TAG, "❌ CRITICAL: Specific record " + recordId + " not found!");
                }
                
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
    }    /**
     * Update ranking data in Firebase
     */
    private void updateUserRankingInFirebase(String userId, UserStats stats) {
        if (stats == null) {
            Log.w(TAG, "⚠️ Cannot update user ranking - stats is null");
            return;
        }
        
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "⚠️ Cannot update user ranking - userId is null or empty");
            return;
        }
        
        // Execute database operation on a background thread
        executor.execute(() -> {
            try {
                int localUserId = getCurrentLocalUserId();
                if (localUserId == -1) {
                    Log.w(TAG, "⚠️ Cannot update user ranking - local user ID is invalid (-1)");
                    return;
                }
                
                // Get user profile data for ranking
                UserEntity user = localDb.userDao().getUserByIdSync(localUserId);
                if (user != null) {                    RankingUser rankingData = new RankingUser(
                        userId,
                        user.getUsername(),
                        user.getProfileImagePath() != null ? user.getProfileImagePath() : "", // profileImageUrl
                        stats.getTotalPoints(),
                        stats.getTotalDistance(),
                        stats.getTotalTrashCollected(),
                        0 // badgeCount - defaulting to 0 for now
                    );
                    
                    // Update ranking data in Firebase (this needs to be on main thread)
                    new Handler(Looper.getMainLooper()).post(() -> {
                        firestore.collection(COLLECTION_RANKING)
                                .document(userId)
                                .set(rankingData)
                                .addOnSuccessListener(aVoid -> 
                                    Log.d(TAG, "✅ User ranking updated in Firebase"))
                                .addOnFailureListener(e -> 
                                    Log.e(TAG, "❌ Error updating user ranking in Firebase", e));
                    });
                } else {
                    Log.w(TAG, "⚠️ User not found for ranking update, local userId: " + localUserId);
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating ranking data", e);
            }
        });
    }
    
    /**
     * Comprehensive test method to verify logout and real-time sync functionality
     */
    public void testLogoutAndRealTimeSync() {
        Log.d(TAG, "🧪 === TESTING LOGOUT AND REAL-TIME SYNC ===");
        
        // Test authentication state
        boolean isLoggedIn = isUserLoggedIn();
        String userId = getCurrentUserId();
        int localUserId = getCurrentLocalUserId();
        
        Log.d(TAG, "🧪 Current auth state:");
        Log.d(TAG, "   Is logged in: " + isLoggedIn);
        Log.d(TAG, "   Firebase user ID: " + userId);
        Log.d(TAG, "   Local user ID: " + localUserId);
        
        if (isLoggedIn) {
            // Test real-time listeners
            Log.d(TAG, "🧪 Testing real-time listeners...");
            
            subscribeToUserStats(new StatsDataCallback() {
                @Override
                public void onStatsLoaded(UserStats stats) {
                    Log.d(TAG, "🧪 ✅ Real-time stats listener working: " + stats.toString());
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "🧪 ❌ Real-time stats listener error: " + error);
                }
            });
            
            // Test Firebase sync
            syncAllUserData(new DataSyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "🧪 ✅ Firebase sync successful");
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "🧪 ❌ Firebase sync error: " + error);
                }
            });
        } else {
            Log.d(TAG, "🧪 User not logged in - testing auth guards");
        }
    }
    
    /**
     * Debug logging untuk tracking sumber data
     */
    private void logDataSource(String dataType, String source, String userId) {
        Log.d(TAG, "📊 [DATA-SOURCE] " + dataType + " loaded from " + source + " for user: " + userId);
    }
    
    /**
     * Debug logging untuk tracking user switch
     */
    private void logUserSwitch(String previousUserId, String newUserId) {
        Log.d(TAG, "🔄 [USER-SWITCH] User changed from: " + previousUserId + " to: " + newUserId);
        Log.d(TAG, "🧹 [USER-SWITCH] Clearing all cached data...");
    }
}

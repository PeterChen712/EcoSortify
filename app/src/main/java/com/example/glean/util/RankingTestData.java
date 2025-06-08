package com.example.glean.util;

import android.content.Context;
import android.util.Log;

import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.TrashEntity;
import com.example.glean.model.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class to create test data for ranking functionality
 * This is useful for testing and demonstration purposes
 */
public class RankingTestData {
    private static final String TAG = "RankingTestData";
    private AppDatabase database;
    private ExecutorService executor;
    
    public RankingTestData(Context context) {
        database = AppDatabase.getInstance(context);
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Add sample users with different points for testing ranking
     */
    public void addSampleUsersForRanking() {
        executor.execute(() -> {
            Log.d(TAG, "Adding sample users for ranking test...");
            
            try {
                // Check if we already have test users
                if (database.userDao().getUserCount() >= 5) {
                    Log.d(TAG, "Already have sufficient users, skipping test data creation");
                    return;
                }
                
                // Create test users with different points
                createTestUser("GreenChampion", "green@test.com", 2500, 15.5f, 45);
                createTestUser("EcoHero", "eco@test.com", 2200, 12.3f, 38);
                createTestUser("CleanMaster", "clean@test.com", 1900, 18.7f, 42);
                createTestUser("PlanetSaver", "planet@test.com", 1650, 9.2f, 25);
                createTestUser("GreenRanger", "ranger@test.com", 1400, 11.8f, 33);
                createTestUser("EcoGuardian", "guardian@test.com", 1200, 7.5f, 20);
                createTestUser("NatureLover", "nature@test.com", 980, 6.1f, 18);
                createTestUser("EarthProtector", "earth@test.com", 850, 4.9f, 15);
                
                Log.d(TAG, "✅ Sample users created successfully!");
                
            } catch (Exception e) {
                Log.e(TAG, "Error creating sample users", e);
            }
        });
    }
    
    private void createTestUser(String username, String email, int points, float totalDistance, int trashItems) {
        try {
            // Check if user already exists
            if (database.userDao().checkUsernameExists(username) > 0) {
                Log.d(TAG, "User " + username + " already exists, skipping");
                return;
            }
            
            // Create user
            UserEntity user = new UserEntity();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword("test123");
            user.setFirstName(username.substring(0, Math.min(username.length(), 8)));
            user.setLastName("User");
            user.setPoints(points);
            user.setCreatedAt(System.currentTimeMillis() - (long)(Math.random() * 30 * 24 * 60 * 60 * 1000L)); // Random time in last 30 days
            
            long userId = database.userDao().insert(user);
            Log.d(TAG, "Created user " + username + " with ID " + userId);
            
            // Create some activity records for this user
            createSampleRecords((int)userId, totalDistance, trashItems);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating user " + username, e);
        }
    }
    
    private void createSampleRecords(int userId, float totalDistance, int trashItems) {
        try {
            // Create 2-4 records for this user
            int recordCount = 2 + (int)(Math.random() * 3);
            float distancePerRecord = totalDistance / recordCount;
            int trashPerRecord = trashItems / recordCount;
            
            for (int i = 0; i < recordCount; i++) {
                RecordEntity record = new RecordEntity();
                record.setUserId(userId);
                record.setDistance(distancePerRecord * 1000); // Convert to meters
                record.setDuration((long)(distancePerRecord * 10 * 60 * 1000)); // Approximate 10 minutes per km
                record.setPoints((int)(distancePerRecord * 50 + trashPerRecord * 10)); // Points calculation
                record.setCreatedAt(System.currentTimeMillis() - (long)(Math.random() * 7 * 24 * 60 * 60 * 1000L)); // Random time in last week
                record.setUpdatedAt(record.getCreatedAt());
                
                long recordId = database.recordDao().insert(record);
                
                // Create trash items for this record
                for (int j = 0; j < trashPerRecord; j++) {
                    TrashEntity trash = new TrashEntity();
                    trash.setRecordId((int)recordId);
                    trash.setTrashType(getRandomTrashType());
                    trash.setTimestamp(record.getCreatedAt() + (j * 60000)); // Spread over time
                    
                    database.trashDao().insert(trash);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating records for user " + userId, e);
        }
    }
    
    private String getRandomTrashType() {
        String[] types = {"plastic", "paper", "metal", "glass", "organic"};
        return types[(int)(Math.random() * types.length)];
    }
    
    /**
     * Clear all test data (useful for resetting)
     */
    public void clearTestData() {
        executor.execute(() -> {
            Log.d(TAG, "Clearing all test data...");
            database.trashDao().deleteAll();
            database.recordDao().deleteAll();
            database.userDao().deleteAll();
            Log.d(TAG, "✅ Test data cleared!");
        });
    }
}

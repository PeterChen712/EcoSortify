package com.example.glean.db;

import android.content.Context;
import android.util.Log;

import com.example.glean.model.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseSeeder {
    private static final String TAG = "DatabaseSeeder";
    private AppDatabase database;
    private ExecutorService executor;
    
    public DatabaseSeeder(Context context) {
        database = AppDatabase.getInstance(context);
        executor = Executors.newSingleThreadExecutor();
    }    public void seedDatabaseIfEmpty() {        executor.execute(() -> {            synchronized (this) {
                // Check if database already has data
                int userCount = database.userDao().getUserCount();
                
                Log.d(TAG, "Current user count: " + userCount);
                
                // Only seed if user table is empty (completely fresh database)
                if (userCount == 0) {
                    Log.d(TAG, "Database is completely empty, seeding initial data...");
                    seedUsers();
                    seedPosts();
                    Log.d(TAG, "✅ Database seeding completed!");
                } else {
                    Log.d(TAG, "Database already contains data (users: " + userCount + "), skipping seeding");
                }
            }
        });
    }    private void seedUsers() {
        Log.d(TAG, "Seeding users...");
        
        // User seeding disabled - no default users will be created
        // This ensures the ranking starts clean without any predefined #1 user
        Log.d(TAG, "✅ User seeding skipped - no default users created");
    }    private void seedPosts() {
        Log.d(TAG, "Seeding posts...");
        
        // Post seeding disabled since no default user exists
        // Posts will be created by actual users through app usage
        Log.d(TAG, "✅ Post seeding skipped - no default posts created");
    }    /**
     * Force reset and reseed the database - useful for testing
     * This will clear all existing data but won't populate any default seed data
     */
    public void forceReseedDatabase() {
        executor.execute(() -> {            Log.d(TAG, "Force reseeding database - clearing existing data...");
            
            // Clear existing data
            database.userDao().deleteAll();
            
            Log.d(TAG, "Existing data cleared. Database is now clean.");
            
            // No seed data will be created - database starts completely fresh
            seedUsers();  // This will skip user creation
            seedPosts();  // This will skip post creation
            
            Log.d(TAG, "✅ Force reseeding completed - database is clean and ready for real user data!");
        });
    }
}

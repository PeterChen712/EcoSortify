package com.example.glean.util;

import android.content.Context;
import android.util.Log;

import com.example.glean.db.AppDatabase;
import com.example.glean.db.DatabaseSeeder;

/**
 * Helper class for database operations, especially for testing and development
 */
public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
    
    /**
     * Reset app data and populate with fresh seed data
     * Useful when user wants to reset the app or for testing
     */
    public static void resetAppData(Context context) {
        Log.d(TAG, "Resetting app data...");
        
        DatabaseSeeder seeder = new DatabaseSeeder(context);
        seeder.forceReseedDatabase();
        
        Log.d(TAG, "✅ App data reset completed!");
    }
    
    /**
     * Check if database is empty and seed if needed
     */
    public static void ensureSeedData(Context context) {
        Log.d(TAG, "Checking seed data...");
        
        DatabaseSeeder seeder = new DatabaseSeeder(context);
        seeder.seedDatabaseIfEmpty();
    }
    
    /**
     * Get current database statistics
     */
    public static void logDatabaseStats(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        
        new Thread(() -> {
            int userCount = database.userDao().getUserCount();
            int postCount = database.postDao().getPostCount();
            
            Log.d(TAG, "📊 Database Stats:");
            Log.d(TAG, "   Users: " + userCount);
            Log.d(TAG, "   Posts: " + postCount);
        }).start();
    }
}

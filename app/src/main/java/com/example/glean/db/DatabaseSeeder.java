package com.example.glean.db;

import android.content.Context;
import android.util.Log;

import com.example.glean.model.PostEntity;
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
    }    public void seedDatabaseIfEmpty() {
        executor.execute(() -> {
            synchronized (this) {
                // Check if database already has data
                int userCount = database.userDao().getUserCount();
                int postCount = database.postDao().getPostCount();
                
                Log.d(TAG, "Current user count: " + userCount + ", post count: " + postCount);
                
                // Only seed if BOTH user and post are empty (completely fresh database)
                if (userCount == 0 && postCount == 0) {
                    Log.d(TAG, "Database is completely empty, seeding initial data...");
                    seedUsers();
                    seedPosts();
                    Log.d(TAG, "✅ Database seeding completed!");
                } else {
                    Log.d(TAG, "Database already contains data (users: " + userCount + ", posts: " + postCount + "), skipping seeding");
                }
            }
        });
    }private void seedUsers() {
        Log.d(TAG, "Seeding users...");
        
        // Check if EcoWarrior already exists
        if (database.userDao().checkUsernameExists("EcoWarrior") > 0) {
            Log.d(TAG, "EcoWarrior already exists, skipping user seeding");
            return;
        }
          // Hanya 1 user - EcoWarrior
        UserEntity user = new UserEntity();
        user.setUsername("EcoWarrior");
        user.setEmail("ecowarrior@glean.app");
        user.setPassword("ecowarrior123"); // Password untuk testing
        user.setFirstName("Eko");
        user.setLastName("Pejuang");
        user.setProfileImagePath("https://i.pravatar.cc/150?img=1");
        user.setPoints(150);
        user.setCreatedAt(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)); // 7 days ago
        
        long userId = database.userDao().insert(user);
        Log.d(TAG, "✅ Inserted single user EcoWarrior with ID: " + userId);
    }    private void seedPosts() {
        Log.d(TAG, "Seeding posts...");
        
        // Extra safety check - if any posts already exist, skip entirely
        int totalPostCount = database.postDao().getPostCount();
        if (totalPostCount > 0) {
            Log.d(TAG, "Database already has " + totalPostCount + " posts, skipping post seeding entirely");
            return;
        }
        
        // Double check for specific posts
        int pantaiLosariCount = database.postDao().checkPostExistsByLocationAndUser("Pantai Losari, Makassar", 1);
        int unhasCount = database.postDao().checkPostExistsByLocationAndUser("Universitas Hasanuddin, Makassar", 1);
        
        if (pantaiLosariCount > 0 || unhasCount > 0) {
            Log.d(TAG, "Specific posts already exist (Pantai: " + pantaiLosariCount + ", UNHAS: " + unhasCount + "), skipping post seeding");
            return;
        }
        
        // Post 1 - dari EcoWarrior (userId = 1)
        PostEntity post1 = new PostEntity();
        post1.setUserId(1);
        post1.setUsername("EcoWarrior");
        post1.setUserAvatar("https://i.pravatar.cc/150?img=1");
        post1.setContent("Hari ini berhasil mengumpulkan 5kg sampah plastik di pantai Losari! 🌊♻️ Setiap botol plastik yang kita ambil adalah satu langkah menuju laut yang lebih bersih. Mari bersama-sama jaga pantai kita! #PloggingChallenge #BersihPantai");
        post1.setImageUrl("https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=300&fit=crop");
        post1.setLikeCount(24);
        post1.setCommentCount(8);
        post1.setTimestamp(System.currentTimeMillis() - (3 * 60 * 60 * 1000L)); // 3 hours ago
        post1.setLocation("Pantai Losari, Makassar");
        post1.setTrashWeight(5.0f);
        post1.setDistance(2.5f);
        
        long postId1 = database.postDao().insertPost(post1);
        Log.d(TAG, "✅ Inserted post 1 from EcoWarrior with ID: " + postId1);
        
        // Post 2 - dari EcoWarrior yang sama (userId = 1)
        PostEntity post2 = new PostEntity();
        post2.setUserId(1);
        post2.setUsername("EcoWarrior");
        post2.setUserAvatar("https://i.pravatar.cc/150?img=1");
        post2.setContent("Pagi yang produktif di sekitar kampus UNHAS! 🎓🌿 Sebagai mahasiswa, kita punya tanggung jawab untuk menjaga lingkungan kampus tetap bersih. Hari ini terkumpul 2.8kg sampah plastik dan kertas. #CampusClean #UNHAS");
        post2.setImageUrl("https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=400&h=300&fit=crop");
        post2.setLikeCount(32);
        post2.setCommentCount(7);
        post2.setTimestamp(System.currentTimeMillis() - (18 * 60 * 60 * 1000L)); // 18 hours ago
        post2.setLocation("Universitas Hasanuddin, Makassar");
        post2.setTrashWeight(2.8f);
        post2.setDistance(1.2f);
        
        long postId2 = database.postDao().insertPost(post2);
        Log.d(TAG, "✅ Inserted post 2 from EcoWarrior with ID: " + postId2);
        
        Log.d(TAG, "✅ Seeding completed! Created exactly 2 posts from EcoWarrior");
    }

    /**
     * Force reset and reseed the database - useful for testing
     * This will clear all existing data and populate fresh seed data
     */
    public void forceReseedDatabase() {
        executor.execute(() -> {
            Log.d(TAG, "Force reseeding database - clearing existing data...");
            
            // Clear existing data
            database.postDao().deleteAll();
            database.userDao().deleteAll();
            
            Log.d(TAG, "Existing data cleared. Starting fresh seeding...");
            
            // Seed fresh data
            seedUsers();
            seedPosts();
            
            Log.d(TAG, "✅ Force reseeding completed!");
        });
    }
}

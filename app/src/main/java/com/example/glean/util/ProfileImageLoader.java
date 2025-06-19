package com.example.glean.util;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.glean.R;
import com.example.glean.auth.FirebaseAuthManager;
import com.example.glean.model.UserEntity;

import java.io.File;

/**
 * Utility class for loading profile images throughout the app
 * Handles the logic for displaying local images for own profile vs placeholder for others
 */
public class ProfileImageLoader {
    
    /**
     * Load profile image into ImageView with proper fallback logic
     * @param context Application context
     * @param imageView Target ImageView
     * @param user User whose profile image to load
     * @param currentUserId Current logged-in user's ID (null if viewing own profile)
     */
    public static void loadProfileImage(Context context, ImageView imageView, UserEntity user, String currentUserId) {
        if (imageView == null || user == null) return;
        
        // Check if this is the current user's own profile
        boolean isOwnProfile = isCurrentUserProfile(context, user, currentUserId);
        
        if (isOwnProfile) {
            // Load local image for own profile
            loadOwnProfileImage(context, imageView, user);
        } else {
            // Load placeholder for other users
            loadPlaceholderImage(context, imageView);
        }
    }
    
    /**
     * Load profile image with automatic current user detection
     * @param context Application context
     * @param imageView Target ImageView
     * @param user User whose profile image to load
     */
    public static void loadProfileImage(Context context, ImageView imageView, UserEntity user) {
        loadProfileImage(context, imageView, user, null);
    }
    
    /**
     * Check if the profile being viewed belongs to the current user
     */
    private static boolean isCurrentUserProfile(Context context, UserEntity user, String providedCurrentUserId) {
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(context);
        
        if (authManager.isLoggedIn()) {
            // For Firebase users, compare Firebase UID
            String currentFirebaseId = authManager.getUserId();
            
            // If provided currentUserId, use it for comparison
            if (providedCurrentUserId != null) {
                return providedCurrentUserId.equals(currentFirebaseId);
            }
            
            // Otherwise, compare with user's identifier
            String userIdentifier = user.getEmail(); // Assuming email as identifier for Firebase users
            return currentFirebaseId != null && userIdentifier != null && 
                   currentFirebaseId.equals(userIdentifier);
        } else {
            // For local users, we can only determine ownership if we have the current user ID
            // Without it, assume it's not the current user (safer default)
            return false;
        }
    }
    
    /**
     * Load profile image for current user (check local storage first)
     */
    private static void loadOwnProfileImage(Context context, ImageView imageView, UserEntity user) {
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(context);
        String currentUserId;
        
        if (authManager.isLoggedIn()) {
            currentUserId = authManager.getUserId();
        } else {
            currentUserId = String.valueOf(user.getId());
        }
        
        if (currentUserId == null) {
            loadPlaceholderImage(context, imageView);
            return;
        }
        
        // Try to load from local storage first
        String localImagePath = LocalProfileImageUtil.getLocalProfileImagePath(context, currentUserId);
        
        if (localImagePath != null) {
            // Load from local file
            File imageFile = new File(localImagePath);
            if (imageFile.exists() && imageFile.canRead()) {
                Glide.with(context)
                        .load(imageFile)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
                return;
            }
        }
        
        // No local image found, load placeholder
        loadPlaceholderImage(context, imageView);
    }
    
    /**
     * Load placeholder image for other users or when local image is not available
     */
    private static void loadPlaceholderImage(Context context, ImageView imageView) {
        Glide.with(context)
                .load(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(imageView);
    }
    
    /**
     * Load profile image for other users (always placeholder)
     * This method is for use in lists, rankings, etc. where you're displaying other users
     */
    public static void loadOtherUserProfileImage(Context context, ImageView imageView) {
        loadPlaceholderImage(context, imageView);
    }
    
    /**
     * Load profile image with Firebase photo URL fallback (for migration compatibility)
     * This can be used during transition period if some users still have Firebase URLs
     */
    public static void loadProfileImageWithFirebaseFallback(Context context, ImageView imageView, 
            UserEntity user, String currentUserId, String firebasePhotoUrl) {
        
        boolean isOwnProfile = isCurrentUserProfile(context, user, currentUserId);
        
        if (isOwnProfile) {
            // For own profile, try local first, then Firebase URL, then placeholder
            String userId = currentUserId != null ? currentUserId : String.valueOf(user.getId());
            String localImagePath = LocalProfileImageUtil.getLocalProfileImagePath(context, userId);
            
            if (localImagePath != null) {
                File imageFile = new File(localImagePath);
                if (imageFile.exists() && imageFile.canRead()) {
                    Glide.with(context)
                            .load(imageFile)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .circleCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imageView);
                    return;
                }
            }
            
            // Try Firebase URL as fallback for migration
            if (firebasePhotoUrl != null && !firebasePhotoUrl.isEmpty() && 
                !firebasePhotoUrl.equals(LocalProfileImageUtil.getDefaultProfileImageUrl())) {
                Glide.with(context)
                        .load(firebasePhotoUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
                return;
            }
        }
        
        // For other users or when no image available, load placeholder
        loadPlaceholderImage(context, imageView);
    }
}

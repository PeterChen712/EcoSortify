package com.example.glean.util;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.model.Avatar;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing local avatars
 * Handles avatar list, loading avatar images, and avatar selection
 */
public class AvatarManager {

    /**
     * Get all available avatars in the app
     * @return List of all avatars
     */
    public static List<Avatar> getAllAvatars() {
        List<Avatar> avatars = new ArrayList<>();
        
        // Default avatar (always unlocked)
        avatars.add(new Avatar("default", "Default Avatar", R.drawable.avatar_default));
        
        // Basic avatars (unlocked by default)
        avatars.add(new Avatar("avatar_1", "Green Avatar", R.drawable.avatar_1));
        avatars.add(new Avatar("avatar_2", "Blue Avatar", R.drawable.avatar_2));
        avatars.add(new Avatar("avatar_3", "Red Avatar", R.drawable.avatar_3));
        avatars.add(new Avatar("avatar_4", "Purple Avatar", R.drawable.avatar_4));
        
        // Premium avatars (require points to unlock)
        avatars.add(new Avatar("avatar_5", "Orange Avatar", R.drawable.avatar_5, 50));
        avatars.add(new Avatar("avatar_6", "Pink Avatar", R.drawable.avatar_6, 100));
        avatars.add(new Avatar("avatar_7", "Brown Avatar", R.drawable.avatar_7, 150));
        avatars.add(new Avatar("avatar_8", "Magenta Avatar", R.drawable.avatar_8, 200));
        avatars.add(new Avatar("avatar_9", "Lime Avatar", R.drawable.avatar_9, 300));
        avatars.add(new Avatar("avatar_10", "Cyan Avatar", R.drawable.avatar_10, 500));
        
        return avatars;
    }

    /**
     * Get available avatars based on user's points
     * @param userPoints User's current points
     * @return List of avatars the user can access
     */
    public static List<Avatar> getAvailableAvatars(int userPoints) {
        List<Avatar> allAvatars = getAllAvatars();
        List<Avatar> availableAvatars = new ArrayList<>();
        
        for (Avatar avatar : allAvatars) {
            if (avatar.getRequiredPoints() <= userPoints) {
                avatar.setUnlocked(true);
                availableAvatars.add(avatar);
            } else {
                avatar.setUnlocked(false);
                // Still add locked avatars to show them as locked
                availableAvatars.add(avatar);
            }
        }
        
        return availableAvatars;
    }

    /**
     * Find avatar by ID
     * @param avatarId Avatar ID
     * @return Avatar object or null if not found
     */
    public static Avatar getAvatarById(String avatarId) {
        if (avatarId == null || avatarId.trim().isEmpty()) {
            return getDefaultAvatar();
        }
        
        List<Avatar> allAvatars = getAllAvatars();
        for (Avatar avatar : allAvatars) {
            if (avatar.getId().equals(avatarId)) {
                return avatar;
            }
        }
        
        // Return default if not found
        return getDefaultAvatar();
    }

    /**
     * Get default avatar
     * @return Default avatar
     */
    public static Avatar getDefaultAvatar() {        return new Avatar("default", "Default Avatar", R.drawable.avatar_default);
    }

    /**
     * Load avatar image into ImageView
     * @param context Application context
     * @param imageView Target ImageView
     * @param avatar Avatar object to load
     */
    public static void loadAvatarIntoImageView(Context context, ImageView imageView, Avatar avatar) {
        if (imageView == null || avatar == null) return;
        
        Glide.with(context)
                .load(avatar.getDrawableResourceId())
                .placeholder(R.drawable.avatar_default)
                .error(R.drawable.avatar_default)
                .circleCrop()
                .into(imageView);
    }

    /**
     * Load avatar image into ImageView by ID
     * @param context Application context
     * @param imageView Target ImageView
     * @param avatarId Avatar ID to load
     */
    public static void loadAvatarIntoImageView(Context context, ImageView imageView, String avatarId) {
        if (imageView == null) return;
        
        Avatar avatar = getAvatarById(avatarId);
        
        Glide.with(context)
                .load(avatar.getDrawableResourceId())
                .placeholder(R.drawable.avatar_default)
                .error(R.drawable.avatar_default)
                .circleCrop()
                .into(imageView);
    }

    /**
     * Load avatar image into ImageView with custom placeholder
     * @param context Application context
     * @param imageView Target ImageView
     * @param avatarId Avatar ID to load
     * @param placeholderResource Placeholder resource
     */
    public static void loadAvatarIntoImageView(Context context, ImageView imageView, String avatarId, int placeholderResource) {
        if (imageView == null) return;
        
        Avatar avatar = getAvatarById(avatarId);
        
        Glide.with(context)
                .load(avatar.getDrawableResourceId())
                .placeholder(placeholderResource)
                .error(placeholderResource)
                .circleCrop()
                .into(imageView);    }

    /**
     * Check if avatar is unlocked for user
     * @param avatarId Avatar ID
     * @param userPoints User's current points
     * @return true if unlocked, false otherwise
     */
    public static boolean isAvatarUnlocked(String avatarId, int userPoints) {
        Avatar avatar = getAvatarById(avatarId);
        return avatar.getRequiredPoints() <= userPoints;
    }

    /**
     * Get required points for avatar
     * @param avatarId Avatar ID
     * @return Required points or 0 if already unlocked/not found
     */
    public static int getRequiredPoints(String avatarId) {
        Avatar avatar = getAvatarById(avatarId);
        return avatar.getRequiredPoints();
    }

    /**
     * Validate avatar ID
     * @param avatarId Avatar ID to validate
     * @return Valid avatar ID (returns "default" if invalid)
     */
    public static String validateAvatarId(String avatarId) {
        if (avatarId == null || avatarId.trim().isEmpty()) {
            return "default";
        }
        
        Avatar avatar = getAvatarById(avatarId);
        return avatar.getId();
    }
}

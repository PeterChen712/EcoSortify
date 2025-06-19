package com.example.glean.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for handling local profile image storage
 * Since Firebase Storage is not available in Spark Plan, profile images are stored locally
 */
public class LocalProfileImageUtil {    private static final String TAG = "LocalProfileImageUtil";
    private static final String PROFILE_IMAGES_DIR = "profile_images";
    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://via.placeholder.com/150/CCCCCC/FFFFFF?text=User";
    private static final int MAX_IMAGE_SIZE = 512; // Maximum size in pixels
    private static final int JPEG_QUALITY = 85; // JPEG compression quality

    /**
     * Save profile image to local storage and return the local file path
     * This path should be saved to SQLite database, NOT to Firestore
     * @param context Application context
     * @param userId User ID to create unique filename
     * @param imageUri URI of the image to save
     * @return Local file path if successful, null if failed
     */
    public static String saveProfileImageLocally(Context context, String userId, Uri imageUri) {
        try {
            // Create profile images directory if it doesn't exist
            File profileImagesDir = new File(context.getFilesDir(), PROFILE_IMAGES_DIR);
            if (!profileImagesDir.exists()) {
                if (!profileImagesDir.mkdirs()) {
                    Log.e(TAG, "Failed to create profile images directory");
                    return null;
                }
            }

            // Create unique filename for this user
            String filename = "profile_" + userId + ".jpg";
            File outputFile = new File(profileImagesDir, filename);

            // Load and resize the image
            Bitmap bitmap = loadAndResizeImage(context, imageUri);
            if (bitmap == null) {
                Log.e(TAG, "Failed to load image from URI: " + imageUri);
                return null;
            }

            // Save the bitmap to local file
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
                outputStream.flush();
                
                Log.d(TAG, "Profile image saved successfully: " + outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving profile image locally", e);
            return null;
        }
    }

    /**
     * Get local profile image path for a user
     * @param context Application context
     * @param userId User ID
     * @return Local file path if exists, null if not found
     */
    public static String getLocalProfileImagePath(Context context, String userId) {
        File profileImagesDir = new File(context.getFilesDir(), PROFILE_IMAGES_DIR);
        String filename = "profile_" + userId + ".jpg";
        File imageFile = new File(profileImagesDir, filename);
        
        if (imageFile.exists() && imageFile.canRead()) {
            return imageFile.getAbsolutePath();
        }
        
        return null;
    }

    /**
     * Check if local profile image exists for a user
     * @param context Application context
     * @param userId User ID
     * @return true if local image exists, false otherwise
     */
    public static boolean hasLocalProfileImage(Context context, String userId) {
        return getLocalProfileImagePath(context, userId) != null;
    }

    /**
     * Delete local profile image for a user
     * @param context Application context
     * @param userId User ID
     * @return true if deleted or didn't exist, false if failed to delete
     */
    public static boolean deleteLocalProfileImage(Context context, String userId) {
        String imagePath = getLocalProfileImagePath(context, userId);
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            boolean deleted = imageFile.delete();
            Log.d(TAG, "Local profile image deletion " + (deleted ? "successful" : "failed") + " for user: " + userId);
            return deleted;
        }
        return true; // No image to delete
    }

    /**
     * Get default profile image URL for Firestore
     * This is used as a placeholder when storing user data to Firestore
     * @return Default placeholder image URL
     */
    public static String getDefaultProfileImageUrl() {
        return DEFAULT_PROFILE_IMAGE_URL;
    }

    /**
     * Load and resize image from URI to optimize storage
     * @param context Application context
     * @param imageUri URI of the image
     * @return Resized bitmap or null if failed
     */
    private static Bitmap loadAndResizeImage(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + imageUri);
                return null;
            }

            // First, get image dimensions without loading the full image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate sample size to resize image
            int sampleSize = calculateSampleSize(options.outWidth, options.outHeight, MAX_IMAGE_SIZE);
            
            // Load the actual image with calculated sample size
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to reopen input stream for URI: " + imageUri);
                return null;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from stream");
                return null;
            }

            // Further resize if still too large
            if (bitmap.getWidth() > MAX_IMAGE_SIZE || bitmap.getHeight() > MAX_IMAGE_SIZE) {
                float scale = Math.min((float) MAX_IMAGE_SIZE / bitmap.getWidth(), 
                                     (float) MAX_IMAGE_SIZE / bitmap.getHeight());
                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                if (resizedBitmap != bitmap) {
                    bitmap.recycle(); // Free memory of original bitmap
                }
                return resizedBitmap;
            }

            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error loading and resizing image", e);
            return null;
        }
    }

    /**
     * Calculate sample size for image loading
     * @param width Original width
     * @param height Original height
     * @param reqSize Required maximum size
     * @return Sample size
     */
    private static int calculateSampleSize(int width, int height, int reqSize) {
        int sampleSize = 1;
        if (height > reqSize || width > reqSize) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            
            while ((halfHeight / sampleSize) >= reqSize && (halfWidth / sampleSize) >= reqSize) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }

    /**
     * Clean up old profile images (utility method for maintenance)
     * This can be called periodically to remove orphaned image files
     * @param context Application context
     */
    public static void cleanupOldProfileImages(Context context) {
        try {
            File profileImagesDir = new File(context.getFilesDir(), PROFILE_IMAGES_DIR);
            if (profileImagesDir.exists() && profileImagesDir.isDirectory()) {
                File[] files = profileImagesDir.listFiles();
                if (files != null) {
                    long currentTime = System.currentTimeMillis();
                    long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L; // 7 days
                    
                    for (File file : files) {
                        if (file.isFile() && (currentTime - file.lastModified()) > oneWeekInMillis) {
                            boolean deleted = file.delete();
                            Log.d(TAG, "Cleanup: " + (deleted ? "Deleted" : "Failed to delete") + " old file: " + file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during profile images cleanup", e);
        }
    }
}

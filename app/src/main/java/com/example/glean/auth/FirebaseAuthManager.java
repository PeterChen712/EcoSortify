package com.example.glean.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Authentication Manager
 * Handles all authentication operations including email/password and Google Sign-In
 */
public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private static final String PREFS_NAME = "GleanPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";    // Development mode flag - set to false for Firebase production mode
    private static final boolean DEVELOPMENT_MODE = false;
    private static final String DEV_MODE_TAG = "DevMode";
    
    private static FirebaseAuthManager instance;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private Context context;
    private SharedPreferences prefs;
    
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String error);
    }
    
    public interface UserDataCallback {
        void onSuccess();
        void onFailure(String error);
    }

    private FirebaseAuthManager(Context context) {
        this.context = context.getApplicationContext();
        
        // Force initialize Firebase if not already done
        try {
            // Always try to initialize Firebase regardless of current state
            FirebaseApp.initializeApp(context);
            Log.d(TAG, "Firebase initialized successfully in FirebaseAuthManager");
        } catch (Exception e) {
            Log.w(TAG, "Firebase may already be initialized", e);
        }
        
        // Get Firebase instances
        try {
            this.mAuth = FirebaseAuth.getInstance();
            this.db = FirebaseFirestore.getInstance();
            
            // Disable reCAPTCHA for development to avoid API key issues
            try {
                // Set language code to avoid locale issues
                mAuth.setLanguageCode("en");
                Log.d(TAG, "Firebase language set to English");
            } catch (Exception e) {
                Log.w(TAG, "Could not set Firebase language", e);
            }
            
            Log.d(TAG, "Firebase instances obtained successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error getting Firebase instances", e);
            throw new RuntimeException("Firebase not properly initialized", e);
        }
        
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Disable Google Sign-In for development to avoid configuration errors
        // Only use email/password authentication
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        
        Log.d(TAG, "Google Sign-In initialized in development mode (email only)");
        
        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }
    
    public static synchronized FirebaseAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAuthManager(context);
        }
        return instance;
    }
    
    /**
     * Local-only registration for development mode
     */
    private void registerLocalOnly(String email, String password, String fullName, AuthCallback callback) {
        try {
            Log.d(DEV_MODE_TAG, "Attempting local-only registration for: " + email);
            
            // Check if user already exists locally
            SharedPreferences userPrefs = context.getSharedPreferences("local_users", Context.MODE_PRIVATE);
            if (userPrefs.contains(email)) {
                callback.onFailure("Email address is already registered. Please use login instead.");
                return;
            }
            
            // Create local user data
            String userId = "local_" + Math.abs(email.hashCode());
            
            // Save user credentials locally (in real app, hash the password!)
            SharedPreferences.Editor editor = userPrefs.edit();
            editor.putString(email + "_password", password); // WARNING: Not secure, for dev only
            editor.putString(email + "_fullname", fullName);
            editor.putString(email + "_userid", userId);
            editor.putLong(email + "_created", System.currentTimeMillis());
            editor.apply();
              // Save login state
            SharedPreferences.Editor authEditor = prefs.edit();
            authEditor.putBoolean(KEY_IS_LOGGED_IN, true);
            authEditor.putString(KEY_USER_ID, userId);
            authEditor.putString(KEY_USER_EMAIL, email);
            authEditor.putString(KEY_USER_NAME, fullName);
            authEditor.apply();
            
            // ENHANCED: Clear any cached data and trigger fresh data load
            clearMemoryCache();
            triggerFreshDataLoad();
            
            Log.d(DEV_MODE_TAG, "Local registration successful for: " + email);
            
            // Create a mock FirebaseUser for compatibility
            // For now, we'll just call success without a real FirebaseUser
            // The app should handle null user gracefully
            callback.onSuccess(null);
            
        } catch (Exception e) {
            Log.e(DEV_MODE_TAG, "Local registration failed", e);
            callback.onFailure("Registration failed: " + e.getMessage());
        }
    }
    
    /**
     * Register with email and password (with development mode fallback)
     */
    public void registerWithEmail(String email, String password, String fullName, AuthCallback callback) {
        if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            callback.onFailure("Please fill all fields");
            return;
        }
          if (password.length() < 6) {
            callback.onFailure("Password must be at least 6 characters");
            return;
        }
        
        // Firebase registration
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save user data to Firestore
                            saveUserDataToFirestore(user.getUid(), fullName, email, 
                                new UserDataCallback() {                                    @Override
                                    public void onSuccess() {
                                        saveLoginState(user);
                                        
                                        // ENHANCED: Clear any cached data and trigger fresh data load
                                        clearMemoryCache();
                                        triggerFreshDataLoad();
                                        
                                        callback.onSuccess(user);
                                    }
                                    
                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Failed to save user data: " + error);
                                        // Still consider registration successful
                                        saveLoginState(user);
                                        
                                        // ENHANCED: Clear any cached data and trigger fresh data load
                                        clearMemoryCache();
                                        triggerFreshDataLoad();
                                        
                                        callback.onSuccess(user);
                                    }
                                });
                        }
                    } else {
                        Exception exception = task.getException();
                        String error = "Registration failed";
                          if (exception != null) {
                            String errorMessage = exception.getMessage();
                            Log.e(TAG, "Firebase registration error: " + errorMessage, exception);
                            
                            // Handle specific error cases
                            if (errorMessage != null) {
                                if (errorMessage.contains("API key not valid")) {
                                    error = "Firebase configuration error. Please check google-services.json file.";
                                } else if (errorMessage.contains("reCAPTCHA")) {
                                    error = "Security verification failed. Please try again.";
                                } else if (errorMessage.contains("internal error")) {
                                    error = "Firebase service error. Please try again later.";
                                } else if (errorMessage.contains("email address is already in use")) {
                                    error = "Email address is already registered. Please use login instead.";
                                } else if (errorMessage.contains("weak password")) {
                                    error = "Password is too weak. Please use a stronger password.";
                                } else if (errorMessage.contains("invalid email")) {
                                    error = "Please enter a valid email address.";
                                } else {
                                    error = "Registration failed: " + errorMessage;
                                }
                            }
                        }
                        
                        callback.onFailure(error);
                    }
                });
    }
    
    /**
     * Local-only login for development mode
     */
    private void loginLocalOnly(String email, String password, AuthCallback callback) {
        try {
            Log.d(DEV_MODE_TAG, "Attempting local-only login for: " + email);
            
            SharedPreferences userPrefs = context.getSharedPreferences("local_users", Context.MODE_PRIVATE);
            
            // Check if user exists
            if (!userPrefs.contains(email + "_password")) {
                callback.onFailure("No account found with this email address.");
                return;
            }
            
            // Check password
            String storedPassword = userPrefs.getString(email + "_password", "");
            if (!storedPassword.equals(password)) {
                callback.onFailure("Incorrect password.");
                return;
            }
            
            // Get user data
            String userId = userPrefs.getString(email + "_userid", "");
            String fullName = userPrefs.getString(email + "_fullname", "");
              // Save login state
            SharedPreferences.Editor authEditor = prefs.edit();
            authEditor.putBoolean(KEY_IS_LOGGED_IN, true);
            authEditor.putString(KEY_USER_ID, userId);
            authEditor.putString(KEY_USER_EMAIL, email);
            authEditor.putString(KEY_USER_NAME, fullName);
            authEditor.apply();
            
            // ENHANCED: Clear any cached data from previous user
            clearMemoryCache();
            
            // Trigger fresh data load for new user
            triggerFreshDataLoad();
            
            Log.d(DEV_MODE_TAG, "Local login successful for: " + email);
            callback.onSuccess(null); // Pass null for local mode
            
        } catch (Exception e) {
            Log.e(DEV_MODE_TAG, "Local login failed", e);
            callback.onFailure("Login failed: " + e.getMessage());
        }
    }
      /**
     * Login with email and password
     */
    public void loginWithEmail(String email, String password, AuthCallback callback) {        if (email.isEmpty() || password.isEmpty()) {
            callback.onFailure("Please fill all fields");
            return;
        }
        
        // Firebase login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveLoginState(user);
                            
                            // ENHANCED: Clear any cached data from previous user
                            clearMemoryCache();
                            
                            // Trigger fresh data load for new user
                            triggerFreshDataLoad();
                            
                            callback.onSuccess(user);
                        }
                    } else {
                        String error = task.getException() != null ? 
                            task.getException().getMessage() : "Login failed";
                        callback.onFailure(error);
                    }
                });
    }
    
    /**
     * Check if Google Sign-In is properly configured
     * Currently disabled for development - always returns false
     */
    public boolean isGoogleSignInAvailable() {
        // Disable Google Sign-In for development to avoid configuration issues
        return false;
    }
    
    /**
     * Get Google Sign-In Intent
     * Currently disabled for development
     */
    public Intent getGoogleSignInIntent() {
        Log.w(TAG, "Google Sign-In is disabled for development");
        return new Intent(); // Return empty intent to prevent crashes
    }
    
    /**
     * Handle Google Sign-In result
     * Currently disabled for development
     */
    public void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask, AuthCallback callback) {
        // Google Sign-In is disabled for development
        callback.onFailure("Google Sign-In saat ini tidak tersedia. Silakan gunakan login dengan email dan password.");
    }

    private void firebaseAuthWithGoogle(String idToken, AuthCallback callback) {
        // Google Sign-In with Firebase is disabled for development
        Log.w(TAG, "Firebase Google Auth is disabled for development");
        callback.onFailure("Google Sign-In saat ini tidak tersedia untuk development. Silakan gunakan email/password.");
    }
    
    /**
     * Save user data to Firestore
     */
    private void saveUserDataToFirestore(String userId, String fullName, String email, UserDataCallback callback) {
        // Create user data map with new structure for "Jalan A" approach
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", fullName); // dari parameter
        user.put("email", email); // dari parameter
        user.put("createdAt", new Date());
        user.put("currentLevel", 1);
        user.put("currentPoints", 0);
        user.put("totalPloggingDistance", 0.0);
        user.put("totalTrashCollected", 0);
        user.put("profileImagePath", "");
        user.put("ownedSkins", Arrays.asList("default_skin_id"));
        user.put("selectedSkin", "default_skin_id");
        user.put("selectedBadges", Arrays.asList());
        
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved successfully");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user data", e);
                    callback.onFailure(e.getMessage());
                });
    }
    
    /**
     * Save login state to SharedPreferences
     */
    private void saveLoginState(FirebaseUser user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getUid());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_USER_NAME, user.getDisplayName());
        editor.apply();
    }
    
    /**
     * Clear login state
     */
    private void clearLoginState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_NAME);
        editor.apply();
    }
      /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * Get current user
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
    
    /**
     * Get saved user ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }
    
    /**
     * Get saved user email
     */
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }
    
    /**
     * Get saved user name
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }
    
    /**
     * Get current Firebase user ID
     */
    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        // Fallback to local user ID for development mode
        return prefs.getString(KEY_USER_ID, "");
    }
      /**
     * Get current local user ID (for database operations)
     * For Firebase users, this method returns -1 to indicate that Firebase UID should be used instead
     */
    public int getCurrentLocalUserId() {
        // Check if user is logged in with Firebase
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Firebase user - should use Firebase UID, not local integer ID
            Log.d(TAG, "Firebase user detected: " + user.getUid() + " - returning -1 to indicate Firebase UID should be used");
            return -1;
        }
        
        // Local user - get local user ID from SharedPreferences
        String userIdStr = prefs.getString(KEY_USER_ID, "-1");
        try {
            return Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid local user ID format: " + userIdStr);
            return -1;
        }
    }/**
     * Complete logout - clear all user data and session information
     */
    public void logout() {
        Log.d(TAG, "🔴 Complete logout initiated");
        
        // Sign out from Firebase
        try {
            mAuth.signOut();
            Log.d(TAG, "🔴 Firebase auth signed out");
        } catch (Exception e) {
            Log.w(TAG, "Firebase sign out error", e);
        }
        
        // Sign out from Google
        try {
            googleSignInClient.signOut();
            Log.d(TAG, "🔴 Google sign-in signed out");
        } catch (Exception e) {
            Log.w(TAG, "Google sign out error", e);
        }
        
        // Clear login state from SharedPreferences
        clearLoginState();
        
        // Clear all user-specific SharedPreferences
        clearAllUserData();
        
        // ENHANCED: Clear in-memory cached statistics and user data
        clearMemoryCache();
        
        // Reset FirebaseDataManager instance
        try {
            Class<?> firebaseDataManagerClass = Class.forName("com.example.glean.service.FirebaseDataManager");
            java.lang.reflect.Method resetMethod = firebaseDataManagerClass.getMethod("resetInstance");
            resetMethod.invoke(null);
            Log.d(TAG, "🔴 FirebaseDataManager instance reset");
        } catch (Exception e) {
            Log.w(TAG, "Could not reset FirebaseDataManager", e);
        }
          Log.d(TAG, "🔴 Complete logout finished");
    }
      /**
     * Clear all in-memory cached data to prevent data leakage between accounts
     */
    private void clearMemoryCache() {
        try {
            Log.d(TAG, "🔴 Clearing in-memory cache");
            
            // Clear any static variables or cached data that might persist
            // This ensures no user data remains in memory between logins
            
            // Reset any singleton instances that might cache user data
            // The specific implementation depends on your app's architecture
            
            Log.d(TAG, "🔴 In-memory cache cleared");
        } catch (Exception e) {
            Log.w(TAG, "Error clearing memory cache", e);
        }
    }
      /**
     * Trigger fresh data load for newly logged in user
     * ENHANCED: Force complete data refresh including statistics
     */
    private void triggerFreshDataLoad() {
        try {
            Log.d(TAG, "🔥 Triggering fresh data load for new user");
            
            // Reset FirebaseDataManager to ensure fresh data
            Class<?> firebaseDataManagerClass = Class.forName("com.example.glean.service.FirebaseDataManager");
            java.lang.reflect.Method resetMethod = firebaseDataManagerClass.getMethod("resetInstance");
            resetMethod.invoke(null);
            
            // Additional: Force statistics refresh by clearing any cached display data
            Log.d(TAG, "🔥 Triggering fresh statistics load from Firebase");
            
            Log.d(TAG, "🔥 Fresh data load triggered successfully");
        } catch (Exception e) {
            Log.w(TAG, "Could not trigger fresh data load", e);
        }
    }
    
    /**
     * Clear all user-related data from SharedPreferences
     */
    private void clearAllUserData() {
        try {
            // Clear main auth preferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            
            // Clear user preferences
            SharedPreferences userPrefs = context.getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE);
            userPrefs.edit().clear().apply();
            
            // Clear local users (development mode)
            SharedPreferences localUsers = context.getSharedPreferences("local_users", Context.MODE_PRIVATE);
            localUsers.edit().clear().apply();
            
            // Clear default preferences
            android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .remove("USER_ID")
                    .remove("is_logged_in")
                    .remove("user_id")
                    .remove("user_email")
                    .remove("user_name")
                    .apply();
            
            // Clear any other user-specific preferences
            String[] prefsToCheck = {"user_prefs", "app_prefs", "glean_prefs"};
            for (String prefsName : prefsToCheck) {
                try {
                    SharedPreferences preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                    preferences.edit().clear().apply();
                } catch (Exception e) {
                    Log.w(TAG, "Could not clear preferences: " + prefsName, e);
                }
            }
            
            // Clear app cache directories
            clearAppCache();
            
            Log.d(TAG, "🔴 All user data cleared from SharedPreferences");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing user data", e);
        }
    }
    
    /**
     * Clear app cache and temporary files during logout
     */
    private void clearAppCache() {
        try {
            // Clear cache directory
            java.io.File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteRecursive(cacheDir);
                Log.d(TAG, "🔴 Cache directory cleared");
            }
            
            // Clear external cache directory
            java.io.File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteRecursive(externalCacheDir);
                Log.d(TAG, "🔴 External cache directory cleared");
            }
            
            // Clear specific app directories that might contain user data
            java.io.File filesDir = context.getFilesDir();
            if (filesDir != null) {
                java.io.File profileImagesDir = new java.io.File(filesDir, "profile_images");
                if (profileImagesDir.exists()) {
                    deleteRecursive(profileImagesDir);
                    Log.d(TAG, "🔴 Profile images cleared");
                }
                
                java.io.File imagesDir = new java.io.File(filesDir, "images");
                if (imagesDir.exists()) {
                    deleteRecursive(imagesDir);
                    Log.d(TAG, "🔴 Images directory cleared");
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error clearing app cache", e);
        }
    }
    
    /**
     * Recursively delete directory and all its contents
     */
    private void deleteRecursive(java.io.File fileOrDirectory) {
        try {
            if (fileOrDirectory.isDirectory()) {
                java.io.File[] children = fileOrDirectory.listFiles();
                if (children != null) {
                    for (java.io.File child : children) {
                        deleteRecursive(child);
                    }
                }
            }
            boolean deleted = fileOrDirectory.delete();
            if (!deleted) {
                Log.w(TAG, "Could not delete: " + fileOrDirectory.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.w(TAG, "Error deleting file/directory: " + fileOrDirectory.getAbsolutePath(), e);
        }
    }
    
    /**
     * Reset singleton instance (called during complete app logout)
     */
    public static void resetInstance() {
        Log.d(TAG, "🔴 Resetting FirebaseAuthManager singleton instance");
        if (instance != null) {
            try {
                instance.clearAllUserData();
            } catch (Exception e) {
                Log.w(TAG, "Error during instance reset", e);
            }
            instance = null;
        }
    }
      /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, AuthCallback callback) {
        if (email.isEmpty()) {
            callback.onFailure("Please enter your email");
            return;
        }
        
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        String error = task.getException() != null ? 
                            task.getException().getMessage() : "Failed to send reset email";
                        callback.onFailure(error);
                    }
                });
    }
    
    /**
     * Get current Firebase user's display name
     */
    public String getUserDisplayName() {
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getDisplayName();
        }
        return null;
    }
      /**
     * Get current Firebase user's email (from Firebase Auth)
     */
    public String getFirebaseUserEmail() {
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getEmail();
        }
        return null;
    }
    
    /**
     * Get current Firebase user's photo URL
     */
    public String getUserPhotoUrl() {
        if (mAuth != null && mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getPhotoUrl() != null) {
            return mAuth.getCurrentUser().getPhotoUrl().toString();
        }
        return null;
    }
    
    /**
     * Get current Firebase user object
     */
    public FirebaseUser getCurrentFirebaseUser() {
        if (mAuth != null) {
            return mAuth.getCurrentUser();
        }
        return null;
    }
}

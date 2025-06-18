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
                                new UserDataCallback() {
                                    @Override
                                    public void onSuccess() {
                                        saveLoginState(user);
                                        callback.onSuccess(user);
                                    }
                                    
                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Failed to save user data: " + error);
                                        // Still consider registration successful
                                        saveLoginState(user);
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
     * Logout user
     */
    public void logout() {
        mAuth.signOut();
        googleSignInClient.signOut();
        clearLoginState();
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
}

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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private static final String KEY_USER_NAME = "user_name";
    
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
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("YOUR_WEB_CLIENT_ID") // Replace with your actual web client ID
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }
    
    public static synchronized FirebaseAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAuthManager(context);
        }
        return instance;
    }
    
    /**
     * Register with email and password
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
                        String error = task.getException() != null ? 
                            task.getException().getMessage() : "Registration failed";
                        callback.onFailure(error);
                    }
                });
    }
    
    /**
     * Login with email and password
     */
    public void loginWithEmail(String email, String password, AuthCallback callback) {
        if (email.isEmpty() || password.isEmpty()) {
            callback.onFailure("Please fill all fields");
            return;
        }
        
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
     * Get Google Sign-In Intent
     */
    public Intent getGoogleSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }
    
    /**
     * Handle Google Sign-In result
     */
    public void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask, AuthCallback callback) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken(), callback);
            }
        } catch (ApiException e) {
            Log.w(TAG, "Google sign in failed", e);
            callback.onFailure("Google sign in failed: " + e.getMessage());
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save user data to Firestore if it's a new user
                            saveUserDataToFirestore(user.getUid(), 
                                user.getDisplayName() != null ? user.getDisplayName() : "User", 
                                user.getEmail() != null ? user.getEmail() : "",
                                new UserDataCallback() {
                                    @Override
                                    public void onSuccess() {
                                        saveLoginState(user);
                                        callback.onSuccess(user);
                                    }
                                    
                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Failed to save user data: " + error);
                                        saveLoginState(user);
                                        callback.onSuccess(user);
                                    }
                                });
                        }
                    } else {
                        String error = task.getException() != null ? 
                            task.getException().getMessage() : "Authentication failed";
                        callback.onFailure(error);
                    }
                });
    }
    
    /**
     * Save user data to Firestore
     */
    private void saveUserDataToFirestore(String userId, String fullName, String email, UserDataCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("totalPloggingDistance", 0.0);
        userData.put("totalPloggingTime", 0L);
        userData.put("totalTrashCollected", 0);
        userData.put("currentLevel", 1);
        userData.put("currentPoints", 0);
        
        db.collection("users").document(userId)
                .set(userData)
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
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getCurrentUser() != null;
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

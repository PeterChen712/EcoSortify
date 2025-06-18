package com.example.glean.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manager for guest mode functionality
 * Handles the distinction between guest users and logged-in users
 */
public class GuestModeManager {
    
    private static final String PREFS_NAME = "GuestModePrefs";
    private static final String KEY_IS_GUEST_MODE = "is_guest_mode";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_SHOW_LOGIN_TOAST = "show_login_toast";
    
    private static GuestModeManager instance;
    private SharedPreferences prefs;
    private Context context;
    
    private GuestModeManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized GuestModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new GuestModeManager(context);
        }
        return instance;
    }
    
    /**
     * Check if app is in guest mode
     * @return true if in guest mode, false if user is logged in
     */
    public boolean isGuestMode() {
        return prefs.getBoolean(KEY_IS_GUEST_MODE, true); // Default is guest mode
    }
    
    /**
     * Set guest mode state
     * @param isGuest true for guest mode, false for logged-in mode
     */
    public void setGuestMode(boolean isGuest) {
        prefs.edit().putBoolean(KEY_IS_GUEST_MODE, isGuest).apply();
    }
    
    /**
     * Check if this is the first app launch
     * @return true if first launch, false otherwise
     */
    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    /**
     * Mark that the app has been launched before
     */
    public void setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }
    
    /**
     * Check if we should show login toast
     * Used to prevent showing toast multiple times for same session
     * @return true if should show toast, false otherwise
     */
    public boolean shouldShowLoginToast() {
        return prefs.getBoolean(KEY_SHOW_LOGIN_TOAST, true);
    }
    
    /**
     * Mark that login toast has been shown for this session
     */
    public void setLoginToastShown() {
        prefs.edit().putBoolean(KEY_SHOW_LOGIN_TOAST, false).apply();
    }
    
    /**
     * Reset login toast flag (call when starting new session)
     */
    public void resetLoginToastFlag() {
        prefs.edit().putBoolean(KEY_SHOW_LOGIN_TOAST, true).apply();
    }
    
    /**
     * Check if feature requires login
     * @param featureName name of the feature
     * @return true if feature requires login, false if available for guests
     */
    public boolean featureRequiresLogin(String featureName) {
        switch (featureName.toLowerCase()) {
            case "plogging":
            case "profile":
            case "stats":
            case "ranking":
            case "community_posting":
                return true;
            case "home":
            case "explore":
            case "game":
            case "classify":
            case "news":
            case "about":
            case "ai_chat":
                return false;
            default:
                return false; // Default to guest-accessible for new features
        }
    }
    
    /**
     * Clear all guest mode preferences (useful for testing or reset)
     */
    public void clearAllPreferences() {
        prefs.edit().clear().apply();
    }
}

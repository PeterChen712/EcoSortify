package com.example.glean.auth;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.activity.AuthActivity;
import com.example.glean.activity.MainActivity;
import com.example.glean.util.GuestModeManager;
import com.example.glean.util.NetworkUtil;

/**
 * AuthGuard handles authentication checks and redirects for features that require login
 */
public class AuthGuard {
    
    private static final String TAG = "AuthGuard";
    
    public interface AuthCheckCallback {
        void onAuthenticationRequired();
        void onProceedWithFeature();
        void onNetworkRequired();
    }
    
    public interface AuthRequiredCallback {
        void onAuthConfirmed();
        void onAuthCancelled();
    }
      /**
     * Check if user can access a feature that requires authentication
     * @param context Application context
     * @param featureName Name of the feature being accessed
     * @param callback Callback to handle the result
     */
    public static void checkFeatureAccess(Context context, String featureName, AuthCheckCallback callback) {
        GuestModeManager guestManager = GuestModeManager.getInstance(context);
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(context);
        
        // Check if feature requires login
        if (guestManager.featureRequiresLogin(featureName)) {
            // Perform thorough authentication check
            if (!isUserProperlyAuthenticated(authManager)) {
                // User needs to login
                callback.onAuthenticationRequired();
                return;
            }
        }
        
        // Check if feature requires network (for online features)
        if (requiresNetwork(featureName)) {
            if (!NetworkUtil.isNetworkAvailable(context)) {
                callback.onNetworkRequired();
                return;
            }
        }
        
        // User can proceed with the feature
        callback.onProceedWithFeature();
    }
    
    /**
     * Perform thorough authentication check
     * @param authManager FirebaseAuthManager instance
     * @return true if user is properly authenticated, false otherwise
     */
    private static boolean isUserProperlyAuthenticated(FirebaseAuthManager authManager) {
        if (!authManager.isLoggedIn()) {
            return false;
        }
        
        // Check if we have valid user ID
        String userId = authManager.getUserId();
        if (userId == null || userId.isEmpty() || userId.equals("-1")) {
            return false;
        }
        
        // For Firebase users, check if Firebase user exists
        if (authManager.getCurrentUser() == null && !userId.startsWith("local_")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Redirect user to login screen with appropriate message
     * @param context Application context
     * @param featureName Name of the feature that triggered the redirect
     */
    public static void redirectToLogin(Context context, String featureName) {
        GuestModeManager guestManager = GuestModeManager.getInstance(context);
        
        // Show toast message if not shown before in this session
        if (guestManager.shouldShowLoginToast()) {
            String message = getLoginMessage(featureName);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            guestManager.setLoginToastShown();
        }
        
        // Start AuthActivity
        Intent intent = new Intent(context, AuthActivity.class);
        intent.putExtra("FEATURE_REQUESTED", featureName);
        intent.putExtra("RETURN_TO_HOME", true); // Flag to return to home after back press
        context.startActivity(intent);
    }
    
    /**
     * Navigate to login fragment within existing navigation
     * @param fragment Current fragment
     * @param featureName Name of the feature that triggered the redirect
     */
    public static void navigateToLogin(Fragment fragment, String featureName) {
        if (fragment.getContext() == null) return;
        
        GuestModeManager guestManager = GuestModeManager.getInstance(fragment.getContext());
        
        // Show toast message if not shown before in this session
        if (guestManager.shouldShowLoginToast()) {
            String message = getLoginMessage(featureName);
            Toast.makeText(fragment.getContext(), message, Toast.LENGTH_SHORT).show();
            guestManager.setLoginToastShown();
        }
        
        // Try to navigate to auth activity
        try {
            Intent intent = new Intent(fragment.getContext(), AuthActivity.class);
            intent.putExtra("FEATURE_REQUESTED", featureName);
            intent.putExtra("RETURN_TO_HOME", true);
            fragment.startActivity(intent);
        } catch (Exception e) {
            // Fallback: just show the message
            Toast.makeText(fragment.getContext(), 
                "Silakan login terlebih dahulu untuk mengakses fitur ini.", 
                Toast.LENGTH_LONG).show();
        }
    }
      /**
     * Legacy method for plogging - now uses the new system
     */
    public static boolean requireAuthForPlogging(Context context, Fragment fragment, AuthRequiredCallback callback) {
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(context);
        
        if (isUserProperlyAuthenticated(authManager)) {
            callback.onAuthConfirmed();
            return true;
        } else {
            // Use new redirect system instead of dialog
            navigateToLogin(fragment, "plogging");
            callback.onAuthCancelled();
            return false;
        }
    }
    
    /**
     * Check if a feature requires network connectivity
     * @param featureName Name of the feature
     * @return true if network is required, false otherwise
     */
    private static boolean requiresNetwork(String featureName) {
        switch (featureName.toLowerCase()) {
            case "stats":
            case "ranking":
            case "community_posting":
            case "profile_sync":
                return true;
            case "plogging":
            case "home":
            case "explore":
            case "game":
            case "classify":
            case "about":
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Get appropriate login message for a feature
     * @param featureName Name of the feature
     * @return Login message string
     */
    private static String getLoginMessage(String featureName) {
        switch (featureName.toLowerCase()) {
            case "plogging":
                return "Silakan login atau daftar untuk mengakses fitur Plogging.";
            case "profile":
                return "Silakan login atau daftar untuk mengakses profil Anda.";
            case "stats":
                return "Silakan login atau daftar untuk melihat statistik Anda.";
            case "ranking":
                return "Silakan login atau daftar untuk melihat ranking.";
            case "community_posting":
                return "Silakan login atau daftar untuk membuat post di komunitas.";
            default:
                return "Silakan login atau daftar untuk mengakses fitur ini.";
        }
    }
    
    /**
     * Show network required message
     * @param context Application context
     * @param featureName Name of the feature
     */
    public static void showNetworkRequiredMessage(Context context, String featureName) {
        String message = getNetworkMessage(featureName);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Get appropriate network required message for a feature
     * @param featureName Name of the feature
     * @return Network required message string
     */
    private static String getNetworkMessage(String featureName) {
        switch (featureName.toLowerCase()) {
            case "stats":
                return "Fitur statistik membutuhkan koneksi internet.";
            case "ranking":
                return "Fitur ranking membutuhkan koneksi internet.";
            case "community_posting":
                return "Fitur komunitas membutuhkan koneksi internet.";
            default:
                return "Fitur ini membutuhkan koneksi internet.";
        }
    }
}
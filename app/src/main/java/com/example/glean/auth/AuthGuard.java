package com.example.glean.auth;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.glean.activity.AuthActivity;

/**
 * Authentication Guard for protecting features that require login
 * Specifically designed for plogging features
 */
public class AuthGuard {
    
    public interface AuthRequiredCallback {
        void onAuthConfirmed();
        void onAuthCancelled();
    }
    
    /**
     * Check if user is authenticated for plogging features
     * @param context Application context
     * @param fragment Fragment requesting auth check
     * @param callback Callback for handling result
     * @return true if user is authenticated, false otherwise
     */
    public static boolean requireAuthForPlogging(Context context, Fragment fragment, AuthRequiredCallback callback) {
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(context);
        
        if (authManager.isLoggedIn()) {
            callback.onAuthConfirmed();
            return true;
        } else {
            showPloggingAuthDialog(context, fragment, callback);
            return false;
        }
    }
    
    /**
     * Show dialog explaining why authentication is required for plogging
     */
    private static void showPloggingAuthDialog(Context context, Fragment fragment, AuthRequiredCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("Join Plogging Community!")
                .setMessage("To track your plogging activities, compete in rankings, and save your progress, you need to create an account.\n\n" +
                           "✅ Track your plogging sessions\n" +
                           "✅ Compete with other users\n" +
                           "✅ Save your achievements\n" +
                           "✅ Access leaderboards\n\n" +
                           "Would you like to sign up or log in now?")
                .setPositiveButton("Sign Up / Login", (dialog, which) -> {
                    // Navigate to auth activity
                    Intent intent = new Intent(context, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    callback.onAuthCancelled();
                })
                .setNegativeButton("Maybe Later", (dialog, which) -> {
                    dialog.dismiss();
                    callback.onAuthCancelled();
                })
                .setCancelable(true)
                .setOnCancelListener(dialog -> callback.onAuthCancelled())
                .show();
    }
    
    /**
     * Quick check without dialog - useful for UI state management
     */
    public static boolean isAuthenticatedForPlogging(Context context) {
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(context);
        return authManager.isLoggedIn();
    }
    
    /**
     * Show simple message about protected feature
     */
    public static void showFeatureProtectedMessage(Context context, String featureName) {
        new AlertDialog.Builder(context)
                .setTitle("Authentication Required")
                .setMessage(featureName + " requires you to be logged in to track your progress and compete with others.")
                .setPositiveButton("Login", (dialog, which) -> {
                    Intent intent = new Intent(context, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}

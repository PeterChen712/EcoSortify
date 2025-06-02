package com.example.glean;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.glean.helper.NotificationHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class GleanApplication extends Application {
    
    private static final String TAG = "GleanApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Set up uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "Uncaught exception in thread: " + thread.getName(), throwable);
                
                // Check if it's a Google Play Services related error
                if (throwable.getMessage() != null && 
                    (throwable.getMessage().contains("gms") || 
                     throwable.getMessage().contains("GooglePlayServices"))) {
                    Log.e(TAG, "Google Play Services related error detected");
                }
                
                // Let the system handle the crash
                System.exit(1);
            }
        });
        
        try {
            // Check Google Play Services availability at app start
            checkGooglePlayServices();
            
            // Initialize notification channels
            NotificationHelper.createNotificationChannels(this);
            
            // Apply saved theme
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isDarkMode = prefs.getBoolean("DARK_MODE", false);
            
            AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                
        } catch (Exception e) {
            Log.e(TAG, "Error in Application onCreate", e);
        }
    }
    
    private void checkGooglePlayServices() {
        try {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
            
            if (resultCode == ConnectionResult.SUCCESS) {
                Log.d(TAG, "Google Play Services is available");
            } else {
                Log.w(TAG, "Google Play Services not available: " + resultCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking Google Play Services", e);
        }
    }
}
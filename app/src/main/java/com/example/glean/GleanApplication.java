package com.example.glean;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.glean.helper.NotificationHelper;

public class GleanApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize notification channels
        NotificationHelper.createNotificationChannels(this);
        
        // Apply saved theme
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = prefs.getBoolean("DARK_MODE", false);
        
        AppCompatDelegate.setDefaultNightMode(
            isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
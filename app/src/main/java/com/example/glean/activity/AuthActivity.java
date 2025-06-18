package com.example.glean.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.glean.R;
import com.example.glean.databinding.ActivityAuthBinding;
import com.example.glean.util.GuestModeManager;

public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;
    private boolean shouldReturnToHome = false;
    private String requestedFeature = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply dark mode preference before setting content view
        applyDarkModePreference();
        
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Check if this was called from a feature request
        Intent intent = getIntent();
        if (intent != null) {
            shouldReturnToHome = intent.getBooleanExtra("RETURN_TO_HOME", false);
            requestedFeature = intent.getStringExtra("FEATURE_REQUESTED");
        }
        
        // Setup navigation - Use the correct fragment ID
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_auth);
        
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
        }
    }

    @Override
    public void onBackPressed() {
        if (shouldReturnToHome) {
            // User came from a feature request, return to MainActivity with home fragment
            returnToHome();
        } else {
            // Normal back behavior
            super.onBackPressed();
        }
    }
    
    private void returnToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("NAVIGATE_TO", "home");
        startActivity(intent);
        finish();
    }
    
    /**
     * Called when user successfully logs in
     * Handles post-login navigation
     */
    public void onLoginSuccess() {
        GuestModeManager guestManager = GuestModeManager.getInstance(this);
        guestManager.setGuestMode(false); // User is now logged in
        guestManager.resetLoginToastFlag(); // Reset for next session
        
        // Navigate based on requested feature
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        if (requestedFeature != null && !requestedFeature.isEmpty()) {
            switch (requestedFeature.toLowerCase()) {
                case "plogging":
                    intent.putExtra("NAVIGATE_TO", "plogging");
                    break;
                case "stats":
                    intent.putExtra("NAVIGATE_TO", "stats");
                    break;
                case "ranking":
                    intent.putExtra("NAVIGATE_TO", "ranking");
                    break;
                default:
                    intent.putExtra("NAVIGATE_TO", "home");
                    break;
            }
        } else {
            intent.putExtra("NAVIGATE_TO", "home");
        }
        
        startActivity(intent);
        finish();
    }

    private void applyDarkModePreference() {
        SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = prefs.getBoolean("DARK_MODE", false);
        AppCompatDelegate.setDefaultNightMode(
            isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Prevent memory leaks
    }
}
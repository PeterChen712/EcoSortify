package com.example.glean.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.glean.R;
import com.example.glean.databinding.ActivityMainBinding;
import com.example.glean.helper.FirebaseHelper;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);
        
        EdgeToEdge.enable(this);

        // Check if user is logged in (local database)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("USER_ID", -1);
        
        if (userId == -1) {
            // User is not logged in, redirect to auth activity
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        
        // Setup bottom navigation
        setupNavigation();
        
        // Initialize Firebase if user wants online features
        checkOnlineFeatures();
    }
    
    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        
        // Updated navigation for 5-item menu (removed community)
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.homeFragment) {
                navController.navigate(R.id.homeFragment);
                return true;
            } else if (itemId == R.id.ploggingFragment) {
                navController.navigate(R.id.ploggingFragment);
                return true;
            } else if (itemId == R.id.statsFragment) {
                navController.navigate(R.id.statsFragment);
                return true;
            } else if (itemId == R.id.newsFragment) {
                navController.navigate(R.id.newsFragment);
                return true;
            } else if (itemId == R.id.profileFragment) {
                navController.navigate(R.id.profileFragment);
                return true;
            }
            return false;
        });
    }
    
    private void checkOnlineFeatures() {
        if (firebaseHelper.isOnline()) {
            // Enable online features
            enableOnlineMode();
        } else {
            // Disable online features, show offline mode
            enableOfflineMode();
        }
    }
    
    private void enableOnlineMode() {
        // Enable online features in existing fragments
        // Community access through Profile fragment
    }
    
    private void enableOfflineMode() {
        // Disable online-only features
        // Show offline indicators in UI
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
package com.example.glean;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.glean.activity.AuthActivity;
import com.example.glean.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);

        // Check if user is logged in
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("USER_ID", -1);
        
        if (userId == -1) {
            // User is not logged in, redirect to auth activity
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        
        // Setup bottom navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        
        // Make sure the BottomNavigationView setup uses the correct menu
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
}
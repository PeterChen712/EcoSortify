package com.example.glean.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.glean.R;
import com.example.glean.activity.AuthActivity;
import com.example.glean.auth.FirebaseAuthManager;
import com.example.glean.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());        // Initialize FirebaseAuthManager
        authManager = FirebaseAuthManager.getInstance(this);
        android.util.Log.d("MainActivity", "FirebaseAuthManager initialized");
        
        // Check if user is logged in using FirebaseAuthManager
        boolean isLoggedIn = authManager.isLoggedIn();
        android.util.Log.d("MainActivity", "User logged in: " + isLoggedIn);
        
        if (!isLoggedIn) {
            android.util.Log.d("MainActivity", "User not logged in, redirecting to AuthActivity");
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        
        android.util.Log.d("MainActivity", "User is logged in, setting up UI");
        
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        binding.bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);
        
        // Add error handling for the entire onCreate process
        try {
            setupNavigation();
            setupBottomNavigation();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Critical error in onCreate: " + e.getMessage(), e);
            // Create emergency fallback view
            createEmergencyView();
        }
    }    private void setupNavigation() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment == null) {
                android.util.Log.e("MainActivity", "NavHostFragment is null!");
                return;
            }
            
            navController = navHostFragment.getNavController();
            android.util.Log.d("MainActivity", "NavController obtained successfully");
            
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
            
            // Navigate to home fragment
            try {
                navController.navigate(R.id.homeFragment);
                android.util.Log.d("MainActivity", "Navigation to homeFragment successful");
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Navigation failed: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in setupNavigation: " + e.getMessage(), e);
        }
          binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.homeFragment) {
                navController.navigate(R.id.homeFragment);
                return true;
            } else if (itemId == R.id.ploggingTabsFragment) {
                navController.navigate(R.id.ploggingTabsFragment);
                return true;
            } else if (itemId == R.id.eksplorasiFragment) {
                navController.navigate(R.id.eksplorasiFragment);
                return true;
            } else if (itemId == R.id.gameFragment) {
                navController.navigate(R.id.gameFragment);
                return true;
            } else if (itemId == R.id.profileFragment) {
                navController.navigate(R.id.profileFragment);
                return true;
            }
            return false;
        });

        handleIntentExtras();
    }    public void navigateToPlgging() {
        if (navController != null) {
            try {
                navController.navigate(R.id.ploggingTabsFragment);
            } catch (Exception e) {
                navController.navigate(R.id.action_homeFragment_to_ploggingTabsFragment);
            }
        }
    }

    public void navigateToStats() {
        if (navController != null) {
            navController.navigate(R.id.ploggingTabsFragment);
        }
    }    public void navigateToCommunity() {
        if (navController != null) {
            navController.navigate(R.id.eksplorasiFragment);
        }
    }

    public void navigateToTrashMap() {
        if (navController != null) {
            try {
                navController.navigate(R.id.trashMapFragment);
            } catch (Exception e) {
                navController.navigate(R.id.action_homeFragment_to_trashMapFragment);
            }
        }
    }

    private void handleIntentExtras() {
        Intent intent = getIntent();        if (intent != null && intent.hasExtra("OPEN_FRAGMENT")) {
            String fragmentToOpen = intent.getStringExtra("OPEN_FRAGMENT");
            if ("stats".equals(fragmentToOpen) && navController != null) {
                navController.navigate(R.id.ploggingTabsFragment);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding = null;
        }
    }

    public NavController getNavController() {
        return navController;
    }
    
    private void createEmergencyView() {
        // Create simple fallback view if everything fails
        try {
            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setGravity(android.view.Gravity.CENTER);
            layout.setBackgroundColor(0xFFFFFFFF); // White background
            
            android.widget.TextView textView = new android.widget.TextView(this);
            textView.setText("EcoSortify");
            textView.setTextSize(24);
            textView.setTextColor(0xFF000000); // Black text
            textView.setGravity(android.view.Gravity.CENTER);
            
            android.widget.Button button = new android.widget.Button(this);
            button.setText("Go to Auth");
            button.setOnClickListener(v -> {
                startActivity(new Intent(this, AuthActivity.class));
                finish();
            });
            
            layout.addView(textView);
            layout.addView(button);
            setContentView(layout);
            
            android.util.Log.d("MainActivity", "Emergency view created");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Failed to create emergency view: " + e.getMessage(), e);
        }
    }
    
    private void setupBottomNavigation() {
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        binding.bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);
    }
}
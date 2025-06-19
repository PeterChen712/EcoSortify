package com.example.glean.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.glean.R;
import com.example.glean.activity.AuthActivity;
import com.example.glean.auth.FirebaseAuthManager;
import com.example.glean.auth.AuthGuard;
import com.example.glean.databinding.ActivityMainBinding;
import com.example.glean.util.GuestModeManager;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;
    private FirebaseAuthManager authManager;
    private GuestModeManager guestModeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize managers
        authManager = FirebaseAuthManager.getInstance(this);
        guestModeManager = GuestModeManager.getInstance(this);
        android.util.Log.d("MainActivity", "Managers initialized");
        
        // Set app mode based on login status
        boolean isLoggedIn = authManager.isLoggedIn();
        guestModeManager.setGuestMode(!isLoggedIn);
        
        android.util.Log.d("MainActivity", "User logged in: " + isLoggedIn + ", Guest mode: " + guestModeManager.isGuestMode());
        
        // Always proceed to main UI (no forced login)
        setupUI();
    }    private void setupUI() {
        android.util.Log.d("MainActivity", "Setting up main UI");
        
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        binding.bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_SELECTED);
        
        // Add error handling for the entire setup process
        try {
            setupNavigation();
            setupBottomNavigation();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Critical error in setupUI: " + e.getMessage(), e);
            // Create emergency fallback view
            createEmergencyView();
        }
    }private void setupNavigation() {
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
        }        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.homeFragment) {
                navController.navigate(R.id.homeFragment);
                return true;
            } else if (itemId == R.id.ploggingTabsFragment) {
                // Check authentication for plogging feature
                AuthGuard.checkFeatureAccess(this, "plogging", new AuthGuard.AuthCheckCallback() {
                    @Override
                    public void onAuthenticationRequired() {
                        // Redirect to login
                        AuthGuard.redirectToLogin(MainActivity.this, "plogging");
                    }
                    
                    @Override
                    public void onProceedWithFeature() {
                        // User is authenticated, proceed to plogging
                        navController.navigate(R.id.ploggingTabsFragment);
                    }
                    
                    @Override
                    public void onNetworkRequired() {
                        // Not applicable for plogging main screen
                        navController.navigate(R.id.ploggingTabsFragment);
                    }
                });
                return true;            } else if (itemId == R.id.aiChatFragment) {
                navController.navigate(R.id.aiChatFragment);
                return true;
            } else if (itemId == R.id.classifyFragment) {
                navController.navigate(R.id.classifyFragment);
                return true;
            } else if (itemId == R.id.gameFragment) {
                navController.navigate(R.id.gameFragment);
                return true;
            }
            return false;        });

        handleIntentExtras();
        // Handle initial navigation intent
        handleNavigationIntent(getIntent());
    }public void navigateToPlgging() {
        if (navController != null) {
            try {
                navController.navigate(R.id.ploggingTabsFragment);
            } catch (Exception e) {
                navController.navigate(R.id.action_homeFragment_to_ploggingTabsFragment);
            }
        }
    }    public void navigateToStats() {
        if (navController != null) {
            navController.navigate(R.id.ploggingTabsFragment);
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
    }    private void setupBottomNavigation() {
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        binding.bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_SELECTED);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNavigationIntent(intent);
    }
    
    private void handleNavigationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("NAVIGATE_TO")) {
            String navigateTo = intent.getStringExtra("NAVIGATE_TO");
            
            // Reset guest mode toast flag for new navigation
            guestModeManager.resetLoginToastFlag();
            
            if (navController != null && navigateTo != null) {
                switch (navigateTo.toLowerCase()) {
                    case "plogging":
                        try {
                            navController.navigate(R.id.ploggingTabsFragment);
                        } catch (Exception e) {
                            // Fallback to home if navigation fails
                            navController.navigate(R.id.homeFragment);
                        }
                        break;
                    case "stats":
                        // Navigate to stats through plogging
                        try {
                            navController.navigate(R.id.ploggingTabsFragment);
                        } catch (Exception e) {
                            navController.navigate(R.id.homeFragment);
                        }
                        break;
                    case "ranking":
                        // Navigate to ranking through appropriate route
                        try {
                            navController.navigate(R.id.rankingFragment);
                        } catch (Exception e) {
                            navController.navigate(R.id.homeFragment);
                        }
                        break;
                    case "home":
                    default:
                        navController.navigate(R.id.homeFragment);
                        break;
                }
            }
        }
    }
}
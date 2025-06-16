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
import com.example.glean.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("USER_ID", -1);
        
        if (userId == -1) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        binding.bottomNavigation.setVisibility(View.VISIBLE);
        binding.bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);
        
        setupNavigation();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return;
        }
        
        navController = navHostFragment.getNavController();
        
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        
        try {
            navController.navigate(R.id.homeFragment);
        } catch (Exception e) {
            // Navigation failed, continue silently
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
}
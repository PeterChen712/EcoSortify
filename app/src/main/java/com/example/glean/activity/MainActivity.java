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
import com.example.glean.databinding.ActivityMainBinding;
import com.example.glean.helper.FirebaseHelper;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseHelper firebaseHelper;
    private NavController navController;    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        firebaseHelper = new FirebaseHelper(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("USER_ID", -1);
        
        if (userId == -1) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        initializeHomeDashboard();

        binding.bottomNavigation.setVisibility(View.VISIBLE);
        binding.bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);
        
        setupNavigation();
        
        checkOnlineFeatures();

        setupQuickActions();
    }    private void initializeHomeDashboard() {
        updateWelcomeMessage();
        updateQuickStats();
        updateWeeklyChallenge();
        updateDailyTip();
    }    private void updateWelcomeMessage() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("USERNAME", "PloggingUser");
        
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        
        if (hour >= 5 && hour < 12) {
            greeting = "Selamat Pagi";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Selamat Siang";
        } else if (hour >= 17 && hour < 20) {
            greeting = "Selamat Sore";
        } else {
            greeting = "Selamat Malam";
        }
        
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, d MMMM yyyy", 
            new java.util.Locale("id", "ID"));
        String currentDate = dateFormat.format(new java.util.Date());
          prefs.edit()
            .putString("GREETING_MESSAGE", greeting + ", " + username + "!")
            .putString("CURRENT_DATE", currentDate)
            .putString("MOTIVATION_MESSAGE", "Mari bersihkan dunia hari ini!")
            .apply();
    }    private void updateQuickStats() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            
            float weeklyDistance;
            int weeklyTrash;
            int weeklyPoints;
            
            try {
                weeklyDistance = prefs.getFloat("WEEKLY_DISTANCE", 0f);
            } catch (ClassCastException e) {
                String distStr = prefs.getString("WEEKLY_DISTANCE", "0");
                try {
                    weeklyDistance = Float.parseFloat(distStr);
                } catch (NumberFormatException nfe) {
                    weeklyDistance = 0f;
                }
            }
            
            try {
                weeklyTrash = prefs.getInt("WEEKLY_TRASH", 0);
            } catch (ClassCastException e) {
                String trashStr = prefs.getString("WEEKLY_TRASH", "0");
                try {
                    weeklyTrash = Integer.parseInt(trashStr);
                } catch (NumberFormatException nfe) {
                    weeklyTrash = 0;
                }
            }
            
            try {
                weeklyPoints = prefs.getInt("WEEKLY_POINTS", 0);
            } catch (ClassCastException e) {
                String pointsStr = prefs.getString("WEEKLY_POINTS", "0");
                try {
                    weeklyPoints = Integer.parseInt(pointsStr);
                } catch (NumberFormatException nfe) {
                    weeklyPoints = 0;
                }
            }
            
            float distanceKm = weeklyDistance / 1000f;
            
            String badge = "Eco Beginner";
            if (weeklyPoints >= 500) {
                badge = "Eco Master";
            } else if (weeklyPoints >= 200) {
                badge = "Eco Hero";
            } else if (weeklyPoints >= 100) {
                badge = "Eco Warrior";
            } else if (weeklyPoints >= 50) {
                badge = "Eco Fighter";
            }
              SharedPreferences.Editor editor = prefs.edit();
            editor.putString("QUICK_STATS_TITLE", "STATISTIK MINGGU INI");
            editor.putString("QUICK_STATS_DISTANCE", "Total Lari: " + String.format("%.1f km", distanceKm));
            editor.putString("QUICK_STATS_TRASH", "Sampah Terkumpul: " + weeklyTrash);
            editor.putString("QUICK_STATS_POINTS", "Poin: " + weeklyPoints);
            editor.putString("QUICK_STATS_BADGE", "Badge: " + badge);
            
            editor.putFloat("WEEKLY_DISTANCE_VALUE", weeklyDistance);
            editor.putInt("WEEKLY_TRASH_VALUE", weeklyTrash);
            editor.putInt("WEEKLY_POINTS_VALUE", weeklyPoints);
            
            editor.apply();
            
        } catch (Exception e) {            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("QUICK_STATS_TITLE", "STATISTIK MINGGU INI");
            editor.putString("QUICK_STATS_DISTANCE", "Total Lari: 0 km");
            editor.putString("QUICK_STATS_TRASH", "Sampah Terkumpul: 0");
            editor.putString("QUICK_STATS_POINTS", "Poin: 0");
            editor.putString("QUICK_STATS_BADGE", "Badge: Eco Beginner");
            editor.apply();
        }
    }    private void updateWeeklyChallenge() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            
            int challengeProgress;
            int challengeTarget = 50;
            
            try {
                challengeProgress = prefs.getInt("CHALLENGE_PROGRESS", 0);
            } catch (ClassCastException e) {
                String progressStr = prefs.getString("CHALLENGE_PROGRESS", "0");
                try {
                    challengeProgress = Integer.parseInt(progressStr);
                } catch (NumberFormatException nfe) {
                    challengeProgress = 0;
                }
            }
            
            try {
                challengeTarget = prefs.getInt("CHALLENGE_TARGET", 50);
            } catch (ClassCastException e) {
                String targetStr = prefs.getString("CHALLENGE_TARGET", "50");
                try {
                    challengeTarget = Integer.parseInt(targetStr);
                } catch (NumberFormatException nfe) {
                    challengeTarget = 50;
                }
            }
            
            int percentage = challengeTarget > 0 ? (challengeProgress * 100) / challengeTarget : 0;
            
            StringBuilder progressBar = new StringBuilder();
            int filledBlocks = percentage / 10;
            
            for (int i = 0; i < 10; i++) {
                if (i < filledBlocks) {
                    progressBar.append("█");
                } else {
                    progressBar.append("░");
                }
            }
            
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
            int daysRemaining = 8 - dayOfWeek;
            if (daysRemaining <= 0) daysRemaining = 7;
              SharedPreferences.Editor editor = prefs.edit();
            editor.putString("CHALLENGE_TITLE", "CHALLENGE MINGGU INI");
            editor.putString("CHALLENGE_TARGET", "Target: Kumpulkan " + challengeTarget + " sampah");
            editor.putString("CHALLENGE_PROGRESS", "Progress: " + progressBar.toString() + " " + challengeProgress + "/" + challengeTarget + " (" + percentage + "%)");
            editor.putString("CHALLENGE_REMAINING", "Sisa: " + daysRemaining + " hari lagi");
            
            editor.putInt("CHALLENGE_PROGRESS_VALUE", challengeProgress);
            editor.putInt("CHALLENGE_TARGET_VALUE", challengeTarget);
            editor.putInt("CHALLENGE_PERCENTAGE", percentage);
            
            editor.apply();
            
        } catch (Exception e) {            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("CHALLENGE_TITLE", "CHALLENGE MINGGU INI");
            editor.putString("CHALLENGE_TARGET", "Target: Kumpulkan 50 sampah");
            editor.putString("CHALLENGE_PROGRESS", "Progress: ░░░░░░░░░░ 0/50 (0%)");
            editor.putString("CHALLENGE_REMAINING", "Sisa: 7 hari lagi");
            editor.putInt("CHALLENGE_PROGRESS_VALUE", 0);
            editor.putInt("CHALLENGE_TARGET_VALUE", 50);
            editor.putInt("CHALLENGE_PERCENTAGE", 0);
            editor.apply();
        }
    }    private void updateDailyTip() {
        String[] ecoTips = {
            "Tip: Bawa botol minum sendiri untuk mengurangi sampah plastik!",
            "Tip: Pilah sampah organik dan anorganik saat plogging!",
            "Tip: 1 botol plastik butuh 450 tahun untuk terurai di alam!",
            "Tip: Setiap langkah kecil membuat perubahan besar untuk bumi!",
            "Tip: Plogging membakar 300-400 kalori per 30 menit!",
            "Tip: Gunakan sarung tangan saat memungut sampah untuk keamanan!",
            "Tip: Foto sampah sebelum dipungut untuk dokumentasi yang baik!"
        };
        
        int dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);
        String dailyTip = ecoTips[dayOfWeek % ecoTips.length];
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("DAILY_TIP", dailyTip).apply();
    }    private void setupQuickActions() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
            .putString("QUICK_ACTION_1", "Mulai Plogging")
            .putString("QUICK_ACTION_2", "Lihat Statistik") 
            .putString("QUICK_ACTION_3", "Berita Lingkungan")
            .putString("QUICK_ACTION_4", "Peta Sampah")
            .apply();
    }private void setupNavigation() {
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
        }
        
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.homeFragment) {
                navController.navigate(R.id.homeFragment);
                refreshHomeData();
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

        handleIntentExtras();
    }    public void navigateToPlgging() {
        if (navController != null) {
            try {
                navController.navigate(R.id.ploggingFragment);
            } catch (Exception e) {
                navController.navigate(R.id.action_homeFragment_to_ploggingFragment);
            }
        }
    }

    public void navigateToStats() {
        if (navController != null) {
            navController.navigate(R.id.statsFragment);
        }
    }

    public void navigateToNews() {
        if (navController != null) {
            navController.navigate(R.id.newsFragment);
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

    private void refreshHomeData() {
        updateWelcomeMessage();
        updateQuickStats();
        updateWeeklyChallenge();
        updateDailyTip();
    }    private void handleIntentExtras() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("OPEN_FRAGMENT")) {
            String fragmentToOpen = intent.getStringExtra("OPEN_FRAGMENT");
            if ("stats".equals(fragmentToOpen) && navController != null) {
                navController.navigate(R.id.statsFragment);
            }
        }
    }

    private void checkOnlineFeatures() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useOnlineFeatures = prefs.getBoolean("USE_ONLINE_FEATURES", false);
        
        if (useOnlineFeatures && firebaseHelper != null) {
            try {
                
            } catch (Exception e) {
                prefs.edit().putBoolean("USE_ONLINE_FEATURES", false).apply();
            }
        }
    }    @Override
    protected void onResume() {
        super.onResume();
        refreshHomeData();
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

    public void updateStatsFromDatabase() {
        try {
            updateQuickStats();
            updateWeeklyChallenge();
        } catch (Exception e) {
        }
    }
}
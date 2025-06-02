package com.example.glean.activity; // UBAH dari com.example.glean

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.glean.R;
import com.example.glean.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private static final long SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Determine if the user is logged in
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isLoggedIn = prefs.getInt("USER_ID", -1) != -1;

        // Delay for splash screen
        new Handler().postDelayed(() -> {
            Intent intent;
            if (isLoggedIn) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, AuthActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Prevent memory leaks
    }
}
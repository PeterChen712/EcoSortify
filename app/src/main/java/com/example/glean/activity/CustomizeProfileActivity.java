package com.example.glean.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.glean.R;
import com.example.glean.databinding.ActivityCustomizeProfileBinding;
import com.example.glean.fragment.BadgeSelectionFragment;
import com.example.glean.fragment.SkinSelectionFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class CustomizeProfileActivity extends AppCompatActivity {
    private static final String TAG = "CustomizeProfileActivity";
    
    private ActivityCustomizeProfileBinding binding;
    private TabAdapter tabAdapter;
    private int userId;
    private boolean hasChanges = false;
      // Tab titles
    private String[] tabTitles;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomizeProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get user ID
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", 0);
        userId = prefs.getInt("USER_ID", -1);
        
        if (userId == -1) {
            Log.e(TAG, "No user ID found");
            Toast.makeText(this, getString(R.string.user_not_found_error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
          // Initialize tab titles
        tabTitles = new String[]{getString(R.string.badges_tab), getString(R.string.backgrounds_tab)};
        
        setupUI();
        setupTabs();
    }
    
    private void setupUI() {
        // Back button
        binding.btnBack.setOnClickListener(v -> {
            if (hasChanges) {
                showUnsavedChangesDialog();
            } else {
                finish();
            }
        });
        
        // Save button
        binding.btnSave.setOnClickListener(v -> saveChanges());
    }
    
    private void setupTabs() {
        // Setup ViewPager2 with adapter
        tabAdapter = new TabAdapter(this);
        binding.viewPager.setAdapter(tabAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
        
        // Listen for tab changes
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab selected: " + tab.getPosition());
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void saveChanges() {        // Save changes from both fragments
        FragmentStateAdapter adapter = (FragmentStateAdapter) binding.viewPager.getAdapter();
        
        // Save badge selection
        try {
            Fragment badgeFragment = getSupportFragmentManager().findFragmentByTag("f0");
            if (badgeFragment instanceof BadgeSelectionFragment) {
                ((BadgeSelectionFragment) badgeFragment).saveSelection();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving badge selection", e);
        }
        
        // Save skin selection  
        try {
            Fragment skinFragment = getSupportFragmentManager().findFragmentByTag("f1");
            if (skinFragment instanceof SkinSelectionFragment) {
                ((SkinSelectionFragment) skinFragment).saveSelection();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving skin selection", e);
        }
          // Set result and finish
        Intent resultIntent = new Intent();
        resultIntent.putExtra("profile_changed", hasChanges);
        setResult(RESULT_OK, resultIntent);
        
        Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void showUnsavedChangesDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save them?")
                .setPositiveButton("Save", (dialog, which) -> saveChanges())
                .setNegativeButton("Discard", (dialog, which) -> finish())
                .setNeutralButton("Cancel", null)
                .show();
    }
    
    public void setHasChanges(boolean hasChanges) {
        this.hasChanges = hasChanges;
        // Update save button state
        binding.btnSave.setEnabled(hasChanges);
        binding.btnSave.setAlpha(hasChanges ? 1.0f : 0.6f);
    }
    
    public int getUserId() {
        return userId;
    }
    
    @Override
    public void onBackPressed() {
        if (hasChanges) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }
    
    // ViewPager2 adapter for tabs
    private static class TabAdapter extends FragmentStateAdapter {
        
        public TabAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new BadgeSelectionFragment();
                case 1:
                    return new SkinSelectionFragment();
                default:
                    return new BadgeSelectionFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 2; // Badge and Skin tabs
        }
    }
}

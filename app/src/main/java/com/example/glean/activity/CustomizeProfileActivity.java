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
import com.example.glean.fragment.AvatarSelectionFragment;
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
        
        try {
            Log.d(TAG, "CustomizeProfileActivity onCreate started");
            
            binding = ActivityCustomizeProfileBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            Log.d(TAG, "Layout inflated successfully");
            
            // Show toast to confirm activity opened
            Toast.makeText(this, "Customize Profile opened", Toast.LENGTH_SHORT).show();
            
            // Get user ID from Intent first, then SharedPreferences
            userId = getIntent().getIntExtra("USER_ID", -1);
            if (userId == -1) {
                SharedPreferences prefs = getSharedPreferences("USER_PREFS", 0);
                userId = prefs.getInt("USER_ID", -1);
            }
            
            Log.d(TAG, "User ID: " + userId);
            
            if (userId == -1) {
                Log.e(TAG, "No user ID found");
                Toast.makeText(this, "User not found. Please try again.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error opening profile customization: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;        }
          try {            // Initialize tab titles with fallback
            try {
                tabTitles = new String[]{getString(R.string.avatars_tab), getString(R.string.badges_tab), getString(R.string.backgrounds_tab)};
            } catch (Exception stringException) {
                Log.w(TAG, "Error getting string resources, using fallback", stringException);
                tabTitles = new String[]{"Avatars", "Badges", "Backgrounds"};
            }
            
            Log.d(TAG, "Setting up UI");
            setupUI();
            
            Log.d(TAG, "Setting up tabs");
            setupTabs();
            
            Log.d(TAG, "CustomizeProfileActivity onCreate completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI or tabs", e);
            Toast.makeText(this, "Error setting up interface: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
      private void setupUI() {
        try {
            Log.d(TAG, "Setting up UI components");
            
            // Check if binding is not null
            if (binding == null) {
                Log.e(TAG, "Binding is null in setupUI");
                Toast.makeText(this, "Error: View binding is null", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // Back button
            if (binding.btnBack != null) {
                binding.btnBack.setOnClickListener(v -> {
                    Log.d(TAG, "Back button clicked");
                    if (hasChanges) {
                        showUnsavedChangesDialog();
                    } else {
                        finish();
                    }
                });
                Log.d(TAG, "Back button listener set");
            } else {
                Log.w(TAG, "Back button is null");
            }
            
            // Save button
            if (binding.btnSave != null) {
                binding.btnSave.setOnClickListener(v -> {
                    Log.d(TAG, "Save button clicked");
                    saveChanges();
                });
                Log.d(TAG, "Save button listener set");
            } else {
                Log.w(TAG, "Save button is null");
            }
            
            Log.d(TAG, "UI setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in setupUI", e);
            Toast.makeText(this, "Error setting up UI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
      private void setupTabs() {
        try {
            Log.d(TAG, "Setting up tabs");
            
            // Check if binding components are not null
            if (binding == null) {
                Log.e(TAG, "Binding is null in setupTabs");
                return;
            }
            
            if (binding.viewPager == null) {
                Log.e(TAG, "ViewPager is null");
                return;
            }
            
            if (binding.tabLayout == null) {
                Log.e(TAG, "TabLayout is null");
                return;
            }
            
            // Setup ViewPager2 with adapter
            tabAdapter = new TabAdapter(this);
            binding.viewPager.setAdapter(tabAdapter);
            Log.d(TAG, "ViewPager adapter set");
            
            // Connect TabLayout with ViewPager2
            new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                    (tab, position) -> {
                        if (position < tabTitles.length) {
                            tab.setText(tabTitles[position]);
                        }
                    }
            ).attach();
            Log.d(TAG, "TabLayoutMediator attached");
            
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
            Log.d(TAG, "Tab listener added");
            
            Log.d(TAG, "Tabs setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in setupTabs", e);
            Toast.makeText(this, "Error setting up tabs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveChanges() {        // Save changes from all fragments
        FragmentStateAdapter adapter = (FragmentStateAdapter) binding.viewPager.getAdapter();
        
        // Save avatar selection
        try {
            Fragment avatarFragment = getSupportFragmentManager().findFragmentByTag("f0");
            if (avatarFragment instanceof AvatarSelectionFragment) {
                ((AvatarSelectionFragment) avatarFragment).saveSelection();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving avatar selection", e);
        }
        
        // Save badge selection
        try {
            Fragment badgeFragment = getSupportFragmentManager().findFragmentByTag("f1");
            if (badgeFragment instanceof BadgeSelectionFragment) {
                ((BadgeSelectionFragment) badgeFragment).saveSelection();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving badge selection", e);
        }
        
        // Save skin selection  
        try {
            Fragment skinFragment = getSupportFragmentManager().findFragmentByTag("f2");
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
                    return new AvatarSelectionFragment();
                case 1:
                    return new BadgeSelectionFragment();
                case 2:
                    return new SkinSelectionFragment();
                default:
                    return new AvatarSelectionFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 3; // Avatar, Badge and Skin tabs
        }
    }
}

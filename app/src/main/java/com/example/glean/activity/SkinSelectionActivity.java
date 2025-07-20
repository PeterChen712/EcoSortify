package com.example.glean.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.adapter.SkinAdapter;
import com.example.glean.model.ProfileSkin;

import java.util.ArrayList;
import java.util.List;

public class SkinSelectionActivity extends AppCompatActivity implements SkinAdapter.OnSkinClickListener {
    
    private RecyclerView rvSkins;
    private TextView tvUserPoints;
    private android.view.View currentSkinPreview;
    private SkinAdapter skinAdapter;
    
    private SharedPreferences sharedPreferences;
    private List<ProfileSkin> skinList;
    private int userPoints;
    private String currentSkinId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skin_selection);
        
        initViews();
        setupData();
        setupRecyclerView();
        updateUI();
    }
    
    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        tvUserPoints = findViewById(R.id.tvUserPoints);
        currentSkinPreview = findViewById(R.id.currentSkinPreview);
        rvSkins = findViewById(R.id.rvSkins);
        
        btnBack.setOnClickListener(v -> finish());
        
        sharedPreferences = getSharedPreferences("profile_settings", MODE_PRIVATE);
    }
      private void setupData() {
        // Get user points from SharedPreferences or database
        userPoints = sharedPreferences.getInt("user_points", 0);
        currentSkinId = sharedPreferences.getString("selected_skin", "default");
        
        // Initialize skin list
        skinList = new ArrayList<>();
        
        // Add available skins
        skinList.add(new ProfileSkin("default", "Default", 0, R.drawable.profile_skin_default, true, currentSkinId.equals("default")));
        skinList.add(new ProfileSkin("nature", "Nature", 100, R.drawable.profile_skin_nature, isUnlocked("nature"), currentSkinId.equals("nature")));
        skinList.add(new ProfileSkin("ocean", "Ocean", 150, R.drawable.profile_skin_ocean, isUnlocked("ocean"), currentSkinId.equals("ocean")));
        skinList.add(new ProfileSkin("sunset", "Sunset", 200, R.drawable.profile_skin_sunset, isUnlocked("sunset"), currentSkinId.equals("sunset")));
        skinList.add(new ProfileSkin("galaxy", "Galaxy", 300, R.drawable.profile_skin_galaxy, isUnlocked("galaxy"), currentSkinId.equals("galaxy")));
    }
    
    private boolean isUnlocked(String skinId) {
        return sharedPreferences.getBoolean("skin_" + skinId + "_unlocked", false);
    }
    
    private void setupRecyclerView() {
        skinAdapter = new SkinAdapter(this, skinList, this);
        rvSkins.setLayoutManager(new GridLayoutManager(this, 2));
        rvSkins.setAdapter(skinAdapter);
    }
    
    private void updateUI() {
        tvUserPoints.setText(userPoints + " Points");
        
        // Update current skin preview
        ProfileSkin currentSkin = getCurrentSkin();
        if (currentSkin != null) {
            currentSkinPreview.setBackgroundResource(currentSkin.getDrawableResource());
        }
    }
    
    private ProfileSkin getCurrentSkin() {
        for (ProfileSkin skin : skinList) {
            if (skin.isSelected()) {
                return skin;
            }
        }
        return skinList.get(0); // Return default if none selected
    }
    
    @Override
    public void onSkinClick(ProfileSkin skin) {
        if (skin.isUnlocked()) {
            // Change current skin
            selectSkin(skin);
        } else {
            // Try to purchase skin
            purchaseSkin(skin);
        }
    }
    
    private void selectSkin(ProfileSkin skin) {
        // Update all skins selection status
        for (ProfileSkin s : skinList) {
            s.setSelected(s.getId().equals(skin.getId()));
        }
          // Save to SharedPreferences
        sharedPreferences.edit()
                .putString("selected_skin", skin.getId())
                .apply();
        
        // Update UI
        skinAdapter.notifyDataSetChanged();
        updateUI();
        
        Toast.makeText(this, "Skin changed to " + skin.getName(), Toast.LENGTH_SHORT).show();
        
        // Set result to notify ProfileFragment
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_skin", skin.getId());
        setResult(RESULT_OK, resultIntent);
    }
    
    private void purchaseSkin(ProfileSkin skin) {
        if (userPoints >= skin.getPrice()) {
            // Deduct points
            userPoints -= skin.getPrice();
            
            // Unlock skin
            skin.setUnlocked(true);
            
            // Save to SharedPreferences
            sharedPreferences.edit()
                    .putInt("user_points", userPoints)
                    .putBoolean("skin_" + skin.getId() + "_unlocked", true)
                    .apply();
            
            // Update UI
            skinAdapter.notifyDataSetChanged();
            updateUI();
            
            Toast.makeText(this, "Skin unlocked: " + skin.getName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Not enough points! Need " + (skin.getPrice() - userPoints) + " more points.", Toast.LENGTH_SHORT).show();
        }
    }
}

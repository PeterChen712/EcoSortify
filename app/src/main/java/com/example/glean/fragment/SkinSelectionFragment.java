package com.example.glean.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.glean.R;
import com.example.glean.activity.CustomizeProfileActivity;
import com.example.glean.adapter.SkinSelectionAdapter;
import com.example.glean.databinding.FragmentSkinSelectionBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.ProfileSkin;
import com.example.glean.model.UserEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkinSelectionFragment extends Fragment implements SkinSelectionAdapter.OnSkinClickListener {
    private static final String TAG = "SkinSelectionFragment";
    
    private FragmentSkinSelectionBinding binding;
    private SkinSelectionAdapter adapter;
    private AppDatabase db;
    private UserEntity currentUser;
    private List<ProfileSkin> availableSkins;
    private String selectedSkinId = null;
    private String originalSkinId = null;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSkinSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = AppDatabase.getInstance(requireContext());
        setupRecyclerView();
        loadUserData();
    }
    
    private void setupRecyclerView() {
        adapter = new SkinSelectionAdapter(requireContext(), new ArrayList<>(), this);
        binding.rvSkins.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvSkins.setAdapter(adapter);
    }
    
    private void loadUserData() {
        if (getActivity() instanceof CustomizeProfileActivity) {
            int userId = ((CustomizeProfileActivity) getActivity()).getUserId();
            
            db.userDao().getUserById(userId).observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    currentUser = user;
                    updateUserPointsDisplay();
                    loadCurrentSkin();
                    loadAvailableSkins();
                }
            });
        }
    }
    
    private void updateUserPointsDisplay() {
        if (binding.tvUserPoints != null && currentUser != null) {
            binding.tvUserPoints.setText(String.valueOf(currentUser.getPoints()));
        }
    }
    
    private void loadCurrentSkin() {
        // Get current active skin from user preferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
        String currentSkinId = prefs.getString("selected_skin", "default");
        originalSkinId = currentSkinId;
        selectedSkinId = currentSkinId;
        
        // Find and display current skin
        ProfileSkin currentSkin = findSkinById(currentSkinId);
        if (currentSkin != null) {
            updateCurrentSkinDisplay(currentSkin);
        }
    }
    
    private void loadAvailableSkins() {
        availableSkins = generateAvailableSkins(currentUser);
        
        if (availableSkins.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            adapter.updateSkins(availableSkins, selectedSkinId, currentUser.getPoints());
        }
    }
      private List<ProfileSkin> generateAvailableSkins(UserEntity user) {
        List<ProfileSkin> skins = new ArrayList<>();
        
        // Get owned skins from preferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
        String ownedSkinsStr = prefs.getString("owned_skins", "default");
        List<String> ownedSkins = new ArrayList<>(Arrays.asList(ownedSkinsStr.split(",")));
        
        // Default skin (always owned)
        skins.add(new ProfileSkin("default", "Default Green", 0, R.drawable.profile_skin_default, true, selectedSkinId.equals("default")));
        
        // Premium skins
        skins.add(new ProfileSkin("nature", "Nature", 100, R.drawable.profile_skin_nature, ownedSkins.contains("nature"), selectedSkinId.equals("nature")));
        skins.add(new ProfileSkin("ocean", "Ocean", 150, R.drawable.profile_skin_ocean, ownedSkins.contains("ocean"), selectedSkinId.equals("ocean")));
        skins.add(new ProfileSkin("sunset", "Sunset", 200, R.drawable.profile_skin_sunset, ownedSkins.contains("sunset"), selectedSkinId.equals("sunset")));
        skins.add(new ProfileSkin("galaxy", "Galaxy", 300, R.drawable.profile_skin_galaxy, ownedSkins.contains("galaxy"), selectedSkinId.equals("galaxy")));
        
        return skins;
    }
    
    private ProfileSkin findSkinById(String skinId) {
        if (availableSkins != null) {
            for (ProfileSkin skin : availableSkins) {
                if (skin.getId().equals(skinId)) {
                    return skin;
                }
            }
        }        // Return default skin if not found
        return new ProfileSkin("default", "Default Green", 0, R.drawable.profile_skin_default, true, false);
    }
    
    private void updateCurrentSkinDisplay(ProfileSkin skin) {
        if (binding.tvCurrentSkinName != null) {
            binding.tvCurrentSkinName.setText(skin.getName());
        }
        if (binding.viewCurrentSkinPreview != null) {
            binding.viewCurrentSkinPreview.setBackgroundResource(skin.getDrawableResource());
        }
    }
    
    private void showEmptyState() {
        binding.rvSkins.setVisibility(View.GONE);
        binding.layoutEmptySkins.setVisibility(View.VISIBLE);
    }
    
    private void hideEmptyState() {
        binding.rvSkins.setVisibility(View.VISIBLE);
        binding.layoutEmptySkins.setVisibility(View.GONE);
    }
    
    @Override
    public void onSkinClick(ProfileSkin skin) {
        if (skin.isUnlocked()) {
            selectedSkinId = skin.getId();
            adapter.updateSelectedSkin(selectedSkinId);
            updateCurrentSkinDisplay(skin);
            
            // Notify parent activity of changes
            if (getActivity() instanceof CustomizeProfileActivity) {
                boolean hasChanges = !selectedSkinId.equals(originalSkinId);
                ((CustomizeProfileActivity) getActivity()).setHasChanges(hasChanges);
            }
            
            Log.d(TAG, "Skin selected: " + skin.getName());
        }
    }
      @Override
    public void onSkinPurchase(ProfileSkin skin) {
        if (currentUser.getPoints() >= skin.getPrice()) {
            // Deduct points and unlock skin
            int newPoints = currentUser.getPoints() - skin.getPrice();
            
            // Update user points in database using background thread
            new Thread(() -> {
                db.userDao().updatePoints(currentUser.getId(), newPoints);
                
                // Update UI on main thread
                requireActivity().runOnUiThread(() -> {
                    // Update current user points locally
                    currentUser.setPoints(newPoints);
                    
                    // Update owned skins in preferences
                    SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
                    String ownedSkinsStr = prefs.getString("owned_skins", "default");
                    if (!ownedSkinsStr.contains(skin.getId())) {
                        ownedSkinsStr += "," + skin.getId();
                        prefs.edit().putString("owned_skins", ownedSkinsStr).apply();
                    }
                      // Update UI
                    skin.setUnlocked(true);
                    updateUserPointsDisplay();
                    adapter.notifyDataSetChanged();
                    
                    Toast.makeText(requireContext(), 
                            "Successfully purchased " + skin.getName() + "!", 
                            Toast.LENGTH_SHORT).show();
                    
                    Log.d(TAG, "Skin purchased: " + skin.getName() + ", Points remaining: " + newPoints);
                });
            }).start();
        } else {
            Toast.makeText(requireContext(), 
                    "Not enough points! You need " + skin.getPrice() + " points.", 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    public void saveSelection() {
        if (selectedSkinId != null && !selectedSkinId.equals(originalSkinId)) {
            // Save selected skin to preferences
            SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
            prefs.edit().putString("selected_skin", selectedSkinId).apply();
            
            Log.d(TAG, "Skin selection saved: " + selectedSkinId);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

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

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.activity.CustomizeProfileActivity;
import com.example.glean.adapter.SkinSelectionAdapter;
import com.example.glean.databinding.FragmentSkinSelectionBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.ProfileSkin;
import com.example.glean.model.UserEntity;
import com.example.glean.model.UserProfile;
import com.example.glean.service.FirebaseDataManager;

import java.util.ArrayList;
import java.util.List;

public class SkinSelectionFragment extends Fragment implements SkinSelectionAdapter.OnSkinClickListener {    private static final String TAG = "SkinSelectionFragment";
    
    private FragmentSkinSelectionBinding binding;
    private SkinSelectionAdapter adapter;
    private AppDatabase db;
    private UserEntity currentUser;
    private List<ProfileSkin> availableSkins;
    private String selectedSkinId = "default"; // Initialize with default value
    private String originalSkinId = null;    private FirebaseDataManager firebaseDataManager;
    private UserProfile userProfile;
    
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
        firebaseDataManager = FirebaseDataManager.getInstance(requireContext());
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
                if (user != null && isAdded() && !isRemoving()) {
                    currentUser = user;
                    updateUserPointsDisplay();
                    loadCurrentSkin();
                    loadAvailableSkins();
                }
            });
        }
    }
      private void updateUserPointsDisplay() {
        // Check if binding is null (fragment might be destroyed)
        if (binding == null || !isAdded() || isRemoving()) {
            Log.w(TAG, "Fragment not active or binding null, skipping points display update");
            return;
        }
        
        if (binding.tvUserPoints != null && currentUser != null) {
            binding.tvUserPoints.setText(String.valueOf(currentUser.getPoints()));
        }
    }
      private void loadCurrentSkin() {
        // Load profile customization from Firebase
        firebaseDataManager.loadProfileCustomization(new FirebaseDataManager.ProfileDataCallback() {            @Override
            public void onProfileLoaded(UserProfile profile) {
                userProfile = profile;
                
                String currentSkinId = profile.getActiveBackground();
                // Ensure we have a valid skin ID, default to "default" if null or empty
                if (currentSkinId == null || currentSkinId.trim().isEmpty()) {
                    currentSkinId = "default";
                }
                
                originalSkinId = currentSkinId;
                selectedSkinId = currentSkinId;
                  // Find and display current skin
                ProfileSkin currentSkin = findSkinById(currentSkinId);
                if (currentSkin != null && isAdded() && !isRemoving()) {
                    updateCurrentSkinDisplay(currentSkin);
                }
                  loadAvailableSkins();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading profile customization: " + error);
                // Fallback to SharedPreferences for backward compatibility
                loadCurrentSkinFromPreferences();
            }
        });
    }
      private void loadCurrentSkinFromPreferences() {
        // Fallback method using SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
        String currentSkinId = prefs.getString("selected_skin", "default");
        
        // Ensure we have a valid skin ID, default to "default" if null or empty
        if (currentSkinId == null || currentSkinId.trim().isEmpty()) {
            currentSkinId = "default";
        }
        
        originalSkinId = currentSkinId;
        selectedSkinId = currentSkinId;
          // Find and display current skin
        ProfileSkin currentSkin = findSkinById(currentSkinId);
        if (currentSkin != null && isAdded() && !isRemoving()) {
            updateCurrentSkinDisplay(currentSkin);                }
                
                loadAvailableSkins();
            }private void loadAvailableSkins() {
        // Check if fragment is still active
        if (!isAdded() || isRemoving() || binding == null) {
            Log.w(TAG, "Fragment not active or binding null, skipping skin loading");
            return;
        }
        
        if (currentUser != null) {
            availableSkins = generateAvailableSkins(currentUser);
            
            if (availableSkins.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
                adapter.updateSkins(availableSkins, selectedSkinId, currentUser.getPoints());
            }
        }
    }
      private List<ProfileSkin> generateAvailableSkins(UserEntity user) {
        List<ProfileSkin> skins = new ArrayList<>();
        
        // Get owned skins from Firebase profile, fallback to SharedPreferences
        List<String> ownedSkins = new ArrayList<>();
        if (userProfile != null) {
            ownedSkins = userProfile.getOwnedBackgrounds();
        } else {
            // Fallback to SharedPreferences
            SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
            String ownedSkinsStr = prefs.getString("owned_skins", "default");
            String[] skinArray = ownedSkinsStr.split(",");
            for (String skin : skinArray) {
                if (!skin.trim().isEmpty()) {
                    ownedSkins.add(skin.trim());
                }
            }
        }
        
        // Ensure default skin is always owned
        if (!ownedSkins.contains("default")) {
            ownedSkins.add("default");
        }
          // Ensure selectedSkinId is not null, default to "default" if null
        String currentSelectedSkin = (selectedSkinId != null) ? selectedSkinId : "default";
          // Default skin (always owned)
        skins.add(new ProfileSkin("default", "Default Green", 0, R.drawable.profile_skin_default, true, currentSelectedSkin.equals("default")));
        
        // Premium static skins
        skins.add(new ProfileSkin("nature", "Nature", 100, R.drawable.profile_skin_nature, ownedSkins.contains("nature"), currentSelectedSkin.equals("nature")));
        skins.add(new ProfileSkin("ocean", "Ocean", 150, R.drawable.profile_skin_ocean, ownedSkins.contains("ocean"), currentSelectedSkin.equals("ocean")));
        skins.add(new ProfileSkin("sunset", "Sunset", 200, R.drawable.profile_skin_sunset, ownedSkins.contains("sunset"), currentSelectedSkin.equals("sunset")));
        skins.add(new ProfileSkin("galaxy", "Galaxy", 300, R.drawable.profile_skin_galaxy, ownedSkins.contains("galaxy"), currentSelectedSkin.equals("galaxy")));
          // Premium animated GIF skins
        skins.add(new ProfileSkin("animated_nature", "Animated Nature", 250, R.raw.bg_animated_nature, ownedSkins.contains("animated_nature"), currentSelectedSkin.equals("animated_nature"), true));
        skins.add(new ProfileSkin("animated_ocean", "Animated Ocean", 350, R.raw.bg_animated_ocean, ownedSkins.contains("animated_ocean"), currentSelectedSkin.equals("animated_ocean"), true));
        skins.add(new ProfileSkin("animated_christmas", "Animated Christmas", 400, R.raw.bg_animated_christhmas, ownedSkins.contains("animated_christmas"), currentSelectedSkin.equals("animated_christmas"), true));
        
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
    }    private void updateCurrentSkinDisplay(ProfileSkin skin) {
        // Check if binding is null (fragment might be destroyed)
        if (binding == null) {
            Log.w(TAG, "Binding is null, fragment might be destroyed. Skipping skin display update.");
            return;
        }
        
        if (binding.tvCurrentSkinName != null) {
            binding.tvCurrentSkinName.setText(skin.getName());
        }
        if (binding.viewCurrentSkinPreview != null) {
            if (skin.isGif()) {
                // Use Glide for GIF animations
                Glide.with(this)
                        .asGif()
                        .load(skin.getDrawableResource())
                        .centerCrop()
                        .into(binding.viewCurrentSkinPreview);
            } else {
                // Use Glide for static images too for consistency
                Glide.with(this)
                        .load(skin.getDrawableResource())
                        .centerCrop()
                        .into(binding.viewCurrentSkinPreview);
            }
        }
    }
      private void showEmptyState() {
        // Check if binding is null (fragment might be destroyed)
        if (binding == null) {
            Log.w(TAG, "Binding is null, fragment might be destroyed. Skipping empty state display.");
            return;
        }
        
        binding.rvSkins.setVisibility(View.GONE);
        binding.layoutEmptySkins.setVisibility(View.VISIBLE);
    }
    
    private void hideEmptyState() {
        // Check if binding is null (fragment might be destroyed)
        if (binding == null) {
            Log.w(TAG, "Binding is null, fragment might be destroyed. Skipping empty state hide.");
            return;
        }
        
        binding.rvSkins.setVisibility(View.VISIBLE);
        binding.layoutEmptySkins.setVisibility(View.GONE);
    }
      @Override
    public void onSkinClick(ProfileSkin skin) {
        if (skin.isUnlocked()) {
            selectedSkinId = skin.getId();
            adapter.updateSelectedSkin(selectedSkinId);
            
            // Only update display if fragment is still active
            if (isAdded() && !isRemoving()) {
                updateCurrentSkinDisplay(skin);
            }
            
            // Notify parent activity of changes
            if (getActivity() instanceof CustomizeProfileActivity) {
                boolean hasChanges = !selectedSkinId.equals(originalSkinId);
                ((CustomizeProfileActivity) getActivity()).setHasChanges(hasChanges);
            }
            
            Log.d(TAG, "Skin selected: " + skin.getName());
        }
    }    @Override
    public void onSkinPurchase(ProfileSkin skin) {
        Log.d(TAG, "Attempting to purchase skin: " + skin.getName() + " for " + skin.getPrice() + " points. Current points: " + currentUser.getPoints());
        
        if (currentUser.getPoints() >= skin.getPrice()) {
            // Use Firebase to purchase background
            firebaseDataManager.purchaseBackground(skin.getId(), skin.getPrice(), 
                new FirebaseDataManager.ProfileCustomizationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Purchase successful, reloading user data to get updated points");
                        
                        // Points are already deducted by Firebase, just reload user data
                        // This will automatically update currentUser object and UI
                        loadUserData();
                        
                        // Update profile data
                        if (userProfile != null) {
                            List<String> ownedBackgrounds = userProfile.getOwnedBackgrounds();
                            if (!ownedBackgrounds.contains(skin.getId())) {
                                ownedBackgrounds.add(skin.getId());
                            }
                        }
                        
                        // Update UI
                        skin.setUnlocked(true);
                        adapter.notifyDataSetChanged();
                        
                        Toast.makeText(requireContext(), 
                                "Successfully purchased " + skin.getName() + "!", 
                                Toast.LENGTH_SHORT).show();
                        
                        Log.d(TAG, "Skin purchased: " + skin.getName() + " - UI updated");
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error purchasing skin: " + error);
                        Toast.makeText(requireContext(), 
                                "Purchase failed: " + error, 
                                Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            Toast.makeText(requireContext(), 
                    "Not enough points! You need " + skin.getPrice() + " points.", 
                    Toast.LENGTH_SHORT).show();
        }
    }
      public void saveSelection() {
        if (selectedSkinId != null && !selectedSkinId.equals(originalSkinId)) {
            // Save selected skin to Firebase
            if (userProfile != null) {
                userProfile.setActiveBackground(selectedSkinId);
                
                firebaseDataManager.updateProfileCustomization(
                    userProfile.getSelectedBadges(),
                    userProfile.getOwnedBackgrounds(),
                    selectedSkinId,                    new FirebaseDataManager.ProfileCustomizationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Skin selection saved to Firebase: " + selectedSkinId);
                            
                            // Check if fragment is still attached before accessing activity
                            if (isAdded() && getActivity() != null) {
                                // Also save to SharedPreferences for backward compatibility
                                SharedPreferences prefs = getActivity().getSharedPreferences("profile_settings", 0);
                                prefs.edit().putString("selected_skin", selectedSkinId).apply();
                            } else {
                                Log.w(TAG, "Fragment not attached, skipping SharedPreferences save");
                            }
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error saving skin selection to Firebase: " + error);
                            
                            // Check if fragment is still attached before accessing activity
                            if (isAdded() && getActivity() != null) {
                                // Fallback to SharedPreferences only
                                SharedPreferences prefs = getActivity().getSharedPreferences("profile_settings", 0);
                                prefs.edit().putString("selected_skin", selectedSkinId).apply();
                                Log.d(TAG, "Skin selection saved to SharedPreferences as fallback: " + selectedSkinId);
                            } else {
                                Log.w(TAG, "Fragment not attached, cannot save to SharedPreferences as fallback");
                            }
                        }
                    });            } else {
                // Check if fragment is still attached before accessing activity
                if (isAdded() && getActivity() != null) {
                    // Fallback to SharedPreferences only
                    SharedPreferences prefs = getActivity().getSharedPreferences("profile_settings", 0);
                    prefs.edit().putString("selected_skin", selectedSkinId).apply();
                    Log.d(TAG, "Skin selection saved to SharedPreferences: " + selectedSkinId);
                } else {
                    Log.w(TAG, "Fragment not attached, cannot save to SharedPreferences");
                }
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

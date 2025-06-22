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
import com.example.glean.adapter.AvatarSelectionAdapter;
import com.example.glean.databinding.FragmentAvatarSelectionBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.Avatar;
import com.example.glean.model.UserEntity;
import com.example.glean.service.FirebaseDataManager;
import com.example.glean.util.AvatarManager;

import com.example.glean.model.UserStats;
import com.example.glean.model.RankingUser;
import com.example.glean.model.UserProfile;

import java.util.List;

public class AvatarSelectionFragment extends Fragment implements AvatarSelectionAdapter.OnAvatarClickListener {
    private static final String TAG = "AvatarSelectionFragment";
    
    private FragmentAvatarSelectionBinding binding;
    private AvatarSelectionAdapter adapter;
    private AppDatabase db;
    private UserEntity currentUser;
    private List<Avatar> availableAvatars;
    private String selectedAvatarId = "default";
    private String originalAvatarId = null;
    private FirebaseDataManager firebaseDataManager;
    private UserProfile userProfile;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAvatarSelectionBinding.inflate(inflater, container, false);
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
        binding.rvAvatars.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        // Adapter will be set after loading user data
    }
    
    private void loadUserData() {
        if (getActivity() instanceof CustomizeProfileActivity) {
            int userId = ((CustomizeProfileActivity) getActivity()).getUserId();
            
            db.userDao().getUserById(userId).observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    currentUser = user;
                    updateUserPointsDisplay();
                    loadCurrentAvatar();
                    loadAvailableAvatars();
                }
            });
        }
    }
    
    private void updateUserPointsDisplay() {
        if (binding.tvUserPoints != null && currentUser != null) {
            binding.tvUserPoints.setText(String.valueOf(currentUser.getPoints()));
        }
    }
      private void loadCurrentAvatar() {
        // Load current avatar from user entity
        String currentAvatarId = currentUser.getActiveAvatar();
        if (currentAvatarId == null || currentAvatarId.trim().isEmpty()) {
            currentAvatarId = "default";
        }
        
        originalAvatarId = currentAvatarId;
        selectedAvatarId = currentAvatarId;
        
        Log.d(TAG, "Current avatar ID: " + currentAvatarId);
    }
    
    private void loadAvailableAvatars() {
        if (currentUser != null) {
            // Get available avatars based on user's points
            availableAvatars = AvatarManager.getAvailableAvatars(currentUser.getPoints());
            
            // Set up adapter with available avatars
            adapter = new AvatarSelectionAdapter(requireContext(), availableAvatars, this);
            binding.rvAvatars.setAdapter(adapter);
            
            // Set selected avatar
            adapter.setSelectedAvatarId(selectedAvatarId);
            
            // Update preview
            updateAvatarPreview();
            
            Log.d(TAG, "Loaded " + availableAvatars.size() + " available avatars");
        }
    }
    
    private void updateAvatarPreview() {
        if (binding.ivAvatarPreview != null) {
            Avatar selectedAvatar = AvatarManager.getAvatarById(selectedAvatarId);
            if (selectedAvatar != null) {
                AvatarManager.loadAvatarIntoImageView(requireContext(), binding.ivAvatarPreview, selectedAvatar);
                
                // Update avatar name
                if (binding.tvAvatarName != null) {
                    binding.tvAvatarName.setText(selectedAvatar.getName());
                }
            }
        }
    }
    
    @Override
    public void onAvatarClick(Avatar avatar) {
        if (avatar != null) {
            selectedAvatarId = avatar.getId();
            adapter.setSelectedAvatarId(selectedAvatarId);
            updateAvatarPreview();
            
            // Notify parent activity of changes
            if (getActivity() instanceof CustomizeProfileActivity) {
                boolean hasChanges = !selectedAvatarId.equals(originalAvatarId);
                ((CustomizeProfileActivity) getActivity()).setHasChanges(hasChanges);
            }
            
            Log.d(TAG, "Avatar selected: " + avatar.getName() + " (ID: " + avatar.getId() + ")");
        }
    }
    
    /**
     * Save the selected avatar to database and Firestore
     */
    public void saveSelection() {
        if (currentUser != null && !selectedAvatarId.equals(originalAvatarId)) {
            // Update local database
            currentUser.setActiveAvatar(selectedAvatarId);
            
            // Save to local database
            new Thread(() -> {
                try {
                    db.userDao().update(currentUser);
                    
                    // Save to Firestore
                    saveToFirestore();
                    
                    // Update UI on main thread
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Avatar updated successfully!", Toast.LENGTH_SHORT).show();
                        originalAvatarId = selectedAvatarId;
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error saving avatar to database", e);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error saving avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }      private void saveToFirestore() {
        try {
            // Update profile customization in Firestore using the new method
            // Avatar selection is treated as background selection in the Firebase data model
            if (userProfile != null) {
                firebaseDataManager.updateProfileCustomization(
                    userProfile.getSelectedBadges(),  // keep existing badges
                    userProfile.getOwnedBackgrounds(),  // keep existing owned backgrounds
                    selectedAvatarId,  // set avatar as active background
                    new FirebaseDataManager.ProfileCustomizationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Avatar saved to Firestore successfully");
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error saving avatar to Firestore: " + error);
                        }
                    }
                );
            } else {
                Log.w(TAG, "UserProfile is null, cannot save to Firestore");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating Firestore", e);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

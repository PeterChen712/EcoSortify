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

import java.util.ArrayList;
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
                    
                    // Load UserProfile from Firebase untuk save ke Firestore
                    loadUserProfileFromFirebase();
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
            Log.d(TAG, "💾 Saving avatar selection: " + selectedAvatarId);
            
            // Update local database
            currentUser.setActiveAvatar(selectedAvatarId);
            
            // Save to local database
            new Thread(() -> {
                try {
                    db.userDao().update(currentUser);
                    Log.d(TAG, "✅ Avatar saved to local database: " + selectedAvatarId);
                    
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
    } 
      private void saveToFirestore() {
        try {
            // Cek apakah userProfile sudah ter-load
            if (userProfile != null) {
                Log.d(TAG, "💾 UserProfile available, saving avatar to Firestore: " + selectedAvatarId);
                
                // Gunakan method updateActiveAvatar yang baru
                firebaseDataManager.updateActiveAvatar(
                    selectedAvatarId,
                    new FirebaseDataManager.ProfileCustomizationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "✅ Avatar saved to Firestore successfully: " + selectedAvatarId);
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "❌ Error saving avatar to Firestore: " + error);
                        }
                    }
                );
            } else {
                Log.w(TAG, "⚠️ UserProfile is null, trying to save with direct Firestore update");
                
                // Fallback: Save langsung ke Firestore tanpa userProfile
                saveAvatarDirectlyToFirestore();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating Firestore", e);
        }
    }
    
    /**
     * Fallback method untuk save avatar langsung ke Firestore 
     */
    private void saveAvatarDirectlyToFirestore() {
        try {
            // Panggil method updateActiveAvatar langsung
            firebaseDataManager.updateActiveAvatar(
                selectedAvatarId,
                new FirebaseDataManager.ProfileCustomizationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ Avatar saved directly to Firestore: " + selectedAvatarId);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Failed to save avatar directly to Firestore: " + error);
                        
                        // Show error message to user
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), 
                                "Avatar tersimpan lokal, tapi gagal sync ke cloud: " + error, 
                                Toast.LENGTH_LONG).show();
                        });
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in saveAvatarDirectlyToFirestore", e);
        }
    }
    
    /**
     * Load UserProfile dari Firebase untuk memastikan save ke Firestore berhasil
     */
    private void loadUserProfileFromFirebase() {
        if (firebaseDataManager != null) {
            firebaseDataManager.subscribeToUserProfile(new FirebaseDataManager.ProfileDataCallback() {
                @Override
                public void onProfileLoaded(UserProfile profile) {
                    userProfile = profile;
                    Log.d(TAG, "✅ UserProfile loaded successfully for avatar save");
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Failed to load UserProfile: " + error);
                    // Tetap set userProfile dengan data minimal agar save bisa jalan
                    userProfile = new UserProfile();
                    if (currentUser != null) {
                        userProfile.setSelectedBadges(new ArrayList<>());
                        userProfile.setOwnedBackgrounds(new ArrayList<>());
                    }
                }
            });
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.example.glean.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.glean.R;
import com.example.glean.activity.CustomizeProfileActivity;
import com.example.glean.adapter.BadgeSelectionAdapter;
import com.example.glean.databinding.FragmentBadgeSelectionBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.Badge;
import com.example.glean.model.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class BadgeSelectionFragment extends Fragment implements BadgeSelectionAdapter.OnBadgeClickListener {
    private static final String TAG = "BadgeSelectionFragment";
    
    private FragmentBadgeSelectionBinding binding;
    private BadgeSelectionAdapter adapter;
    private AppDatabase db;
    private UserEntity currentUser;
    private List<Badge> availableBadges;
    private String selectedBadgeId = null;
    private String originalBadgeId = null;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBadgeSelectionBinding.inflate(inflater, container, false);
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
        adapter = new BadgeSelectionAdapter(requireContext(), new ArrayList<>(), this);
        binding.rvBadges.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.rvBadges.setAdapter(adapter);
    }
    
    private void loadUserData() {
        if (getActivity() instanceof CustomizeProfileActivity) {
            int userId = ((CustomizeProfileActivity) getActivity()).getUserId();
            
            db.userDao().getUserById(userId).observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    currentUser = user;
                    loadCurrentBadge();
                    loadAvailableBadges();
                }
            });
        }
    }
    
    private void loadCurrentBadge() {
        // Get current active badge from user preferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
        String currentBadgeId = prefs.getString("active_badge", "starter");
        originalBadgeId = currentBadgeId;
        selectedBadgeId = currentBadgeId;
        
        // Find and display current badge
        Badge currentBadge = findBadgeById(currentBadgeId);
        if (currentBadge != null) {
            updateCurrentBadgeDisplay(currentBadge);
        }
    }
    
    private void loadAvailableBadges() {
        availableBadges = generateUserBadges(currentUser);
        
        if (availableBadges.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            adapter.updateBadges(availableBadges, selectedBadgeId);
        }
    }
      private List<Badge> generateUserBadges(UserEntity user) {
        List<Badge> badges = new ArrayList<>();
        int points = user.getPoints();
        int badgeIdCounter = 1;
        
        // Always available badges
        Badge starter = new Badge(badgeIdCounter++, "Starter", "Your first badge", "starter", 1, true);
        starter.setIconResource(R.drawable.ic_star);
        badges.add(starter);
        
        // Achievement-based badges
        if (points >= 50) {
            Badge greenHelper = new Badge(badgeIdCounter++, "Green Helper", "Eco-friendly contributor", "green_helper", 1, true);
            greenHelper.setIconResource(R.drawable.ic_leaf);
            badges.add(greenHelper);
        }
        if (points >= 100) {
            Badge ecoWarrior = new Badge(badgeIdCounter++, "Eco Warrior", "Environmental champion", "eco_warrior", 2, true);
            ecoWarrior.setIconResource(R.drawable.ic_award);
            badges.add(ecoWarrior);
        }
        if (points >= 200) {
            Badge greenChampion = new Badge(badgeIdCounter++, "Green Champion", "Green environmental champion", "green_champion", 2, true);
            greenChampion.setIconResource(R.drawable.ic_award);
            badges.add(greenChampion);
        }
        if (points >= 500) {
            Badge earthGuardian = new Badge(badgeIdCounter++, "Earth Guardian", "Protector of the environment", "earth_guardian", 3, true);
            earthGuardian.setIconResource(R.drawable.ic_globe);
            badges.add(earthGuardian);
        }
        if (points >= 1000) {
            Badge expertPlogger = new Badge(badgeIdCounter++, "Expert Plogger", "Master of plogging", "expert_plogger", 3, true);
            expertPlogger.setIconResource(R.drawable.ic_crown);
            badges.add(expertPlogger);
        }
        if (points >= 2000) {
            Badge ecoLegend = new Badge(badgeIdCounter++, "Eco Legend", "Legendary environmental hero", "eco_legend", 3, true);
            ecoLegend.setIconResource(R.drawable.ic_crown);
            badges.add(ecoLegend);
        }
        
        // Special badges
        if (points >= 1500) {
            Badge masterCleaner = new Badge(badgeIdCounter++, "Master Cleaner", "Expert in cleanup activities", "master_cleaner", 3, true);
            masterCleaner.setIconResource(R.drawable.ic_cleaning);
            badges.add(masterCleaner);
        }
        
        return badges;
    }
      private Badge findBadgeById(String badgeId) {
        if (availableBadges != null) {
            for (Badge badge : availableBadges) {
                if (badge.getType().equals(badgeId)) {
                    return badge;
                }
            }
        }
        // Return default badge if not found
        Badge defaultBadge = new Badge(1, "Starter", "Your first badge", "starter", 1, true);
        defaultBadge.setIconResource(R.drawable.ic_star);
        return defaultBadge;
    }
    
    private void updateCurrentBadgeDisplay(Badge badge) {
        if (binding.tvCurrentBadgeName != null) {
            binding.tvCurrentBadgeName.setText(badge.getName());
        }
        if (binding.tvCurrentBadgeDescription != null) {
            binding.tvCurrentBadgeDescription.setText(badge.getDescription());
        }
        if (binding.ivCurrentBadge != null) {
            // Set badge icon - you can customize this based on badge type
            binding.ivCurrentBadge.setImageResource(R.drawable.ic_star);
        }
    }
    
    private void showEmptyState() {
        binding.rvBadges.setVisibility(View.GONE);
        binding.layoutEmptyBadges.setVisibility(View.VISIBLE);
    }
    
    private void hideEmptyState() {
        binding.rvBadges.setVisibility(View.VISIBLE);
        binding.layoutEmptyBadges.setVisibility(View.GONE);
    }
      @Override
    public void onBadgeClick(Badge badge) {
        if (badge.isEarned()) {
            selectedBadgeId = badge.getType();
            adapter.updateSelectedBadge(selectedBadgeId);
            updateCurrentBadgeDisplay(badge);
            
            // Notify parent activity of changes
            if (getActivity() instanceof CustomizeProfileActivity) {
                boolean hasChanges = !selectedBadgeId.equals(originalBadgeId);
                ((CustomizeProfileActivity) getActivity()).setHasChanges(hasChanges);
            }
            
            Log.d(TAG, "Badge selected: " + badge.getName());
        }
    }
    
    public void saveSelection() {
        if (selectedBadgeId != null && !selectedBadgeId.equals(originalBadgeId)) {
            // Save selected badge to preferences
            SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
            prefs.edit().putString("active_badge", selectedBadgeId).apply();
            
            Log.d(TAG, "Badge selection saved: " + selectedBadgeId);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

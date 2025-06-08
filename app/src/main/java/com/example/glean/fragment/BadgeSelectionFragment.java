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
import com.example.glean.adapter.BadgeSelectionAdapter;
import com.example.glean.databinding.FragmentBadgeSelectionBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.Badge;
import com.example.glean.model.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class BadgeSelectionFragment extends Fragment implements BadgeSelectionAdapter.OnBadgeClickListener {
    private static final String TAG = "BadgeSelectionFragment";
    private static final int MAX_SELECTED_BADGES = 3;
    
    private FragmentBadgeSelectionBinding binding;
    private BadgeSelectionAdapter adapter;
    private AppDatabase db;
    private UserEntity currentUser;
    private List<Badge> availableBadges;
    private List<String> selectedBadgeIds = new ArrayList<>();
    private List<String> originalBadgeIds = new ArrayList<>();
    
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
        // Get current selected badges from user preferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
        String selectedBadgesJson = prefs.getString("selected_badges", "");
        
        if (selectedBadgesJson.isEmpty()) {
            // Migrate from old single badge system
            String legacyBadge = prefs.getString("active_badge", "starter");
            selectedBadgeIds.add(legacyBadge);
        } else {
            // Parse JSON array of selected badges
            try {
                String[] badgeArray = selectedBadgesJson.split(",");
                for (String badge : badgeArray) {
                    if (!badge.trim().isEmpty() && selectedBadgeIds.size() < MAX_SELECTED_BADGES) {
                        selectedBadgeIds.add(badge.trim());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing selected badges", e);
                selectedBadgeIds.add("starter"); // Fallback
            }
        }
        
        if (selectedBadgeIds.isEmpty()) {
            selectedBadgeIds.add("starter"); // Ensure at least one badge
        }
        
        originalBadgeIds = new ArrayList<>(selectedBadgeIds);
        
        // Update current badge display to show selected count
        updateCurrentBadgeDisplay();
    }
      private void loadAvailableBadges() {
        availableBadges = generateUserBadges(currentUser);
        
        if (availableBadges.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            adapter.updateBadges(availableBadges, selectedBadgeIds);
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
      private void updateCurrentBadgeDisplay() {
        if (binding.tvCurrentBadgeName != null) {
            if (selectedBadgeIds.size() == 1) {
                Badge badge = findBadgeById(selectedBadgeIds.get(0));
                binding.tvCurrentBadgeName.setText(badge.getName());
                binding.tvCurrentBadgeDescription.setText(badge.getDescription());
            } else {
                binding.tvCurrentBadgeName.setText("Selected Badges (" + selectedBadgeIds.size() + "/" + MAX_SELECTED_BADGES + ")");
                binding.tvCurrentBadgeDescription.setText("Choose up to " + MAX_SELECTED_BADGES + " badges to display in your profile");
            }
        }
        if (binding.ivCurrentBadge != null) {
            // Set icon for first selected badge or default
            Badge firstBadge = findBadgeById(selectedBadgeIds.get(0));
            if (firstBadge.getIconResource() != 0) {
                binding.ivCurrentBadge.setImageResource(firstBadge.getIconResource());
            } else {
                binding.ivCurrentBadge.setImageResource(R.drawable.ic_star);
            }
        }
    }
    
    private void showEmptyState() {
        binding.rvBadges.setVisibility(View.GONE);
        binding.layoutEmptyBadges.setVisibility(View.VISIBLE);
    }
    
    private void hideEmptyState() {
        binding.rvBadges.setVisibility(View.VISIBLE);
        binding.layoutEmptyBadges.setVisibility(View.GONE);
    }      @Override
    public void onBadgeClick(Badge badge) {
        if (!badge.isEarned()) {
            return; // Can't select unearned badges
        }
        
        String badgeId = badge.getType();
        
        if (selectedBadgeIds.contains(badgeId)) {
            // Deselect badge (but ensure at least one remains selected)
            if (selectedBadgeIds.size() > 1) {
                selectedBadgeIds.remove(badgeId);
                adapter.updateSelectedBadges(selectedBadgeIds);
                updateCurrentBadgeDisplay();
                
                // Notify parent activity of changes
                if (getActivity() instanceof CustomizeProfileActivity) {
                    boolean hasChanges = !selectedBadgeIds.equals(originalBadgeIds);
                    ((CustomizeProfileActivity) getActivity()).setHasChanges(hasChanges);
                }
                
                Log.d(TAG, "Badge deselected: " + badge.getName() + ". Selected count: " + selectedBadgeIds.size());
            } else {
                Toast.makeText(requireContext(), "At least one badge must be selected", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Select badge (if not at max limit)
            if (selectedBadgeIds.size() < MAX_SELECTED_BADGES) {
                selectedBadgeIds.add(badgeId);
                adapter.updateSelectedBadges(selectedBadgeIds);
                updateCurrentBadgeDisplay();
                
                // Notify parent activity of changes
                if (getActivity() instanceof CustomizeProfileActivity) {
                    boolean hasChanges = !selectedBadgeIds.equals(originalBadgeIds);
                    ((CustomizeProfileActivity) getActivity()).setHasChanges(hasChanges);
                }
                
                Log.d(TAG, "Badge selected: " + badge.getName() + ". Selected count: " + selectedBadgeIds.size());
            } else {
                Toast.makeText(requireContext(), "Maximum " + MAX_SELECTED_BADGES + " badges can be selected", Toast.LENGTH_SHORT).show();
            }
        }
    }
      public void saveSelection() {
        if (!selectedBadgeIds.equals(originalBadgeIds)) {
            // Save selected badges to preferences as comma-separated string
            SharedPreferences prefs = requireActivity().getSharedPreferences("profile_settings", 0);
            String selectedBadgesJson = String.join(",", selectedBadgeIds);
            prefs.edit().putString("selected_badges", selectedBadgesJson).apply();
            
            // Also maintain legacy single badge for backward compatibility (use first selected)
            prefs.edit().putString("active_badge", selectedBadgeIds.get(0)).apply();
            
            Log.d(TAG, "Badge selection saved: " + selectedBadgesJson);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

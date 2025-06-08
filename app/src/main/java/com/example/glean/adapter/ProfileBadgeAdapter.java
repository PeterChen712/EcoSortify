package com.example.glean.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.Badge;

import java.util.ArrayList;
import java.util.List;

public class ProfileBadgeAdapter extends RecyclerView.Adapter<ProfileBadgeAdapter.BadgeViewHolder> {
    
    private final Context context;
    private List<Badge> badges;
    private List<String> selectedBadgeIds = new ArrayList<>();
    private final OnBadgeClickListener listener;
    
    public interface OnBadgeClickListener {
        void onBadgeClick(Badge badge);
    }
      public ProfileBadgeAdapter(Context context, List<Badge> badges, OnBadgeClickListener listener) {
        this.context = context;
        this.badges = badges;
        this.listener = listener;
        loadSelectedBadges();
    }
    
    public ProfileBadgeAdapter(Context context, List<Badge> badges) {
        this.context = context;
        this.badges = badges;
        this.listener = null;
        loadSelectedBadges();
    }
    
    private void loadSelectedBadges() {
        SharedPreferences prefs = context.getSharedPreferences("profile_settings", 0);
        String selectedBadgesJson = prefs.getString("selected_badges", "");
        
        selectedBadgeIds.clear();
        if (selectedBadgesJson.isEmpty()) {
            // Migrate from old single badge system
            String legacyBadge = prefs.getString("active_badge", "starter");
            selectedBadgeIds.add(legacyBadge);
        } else {
            // Parse comma-separated selected badges
            try {
                String[] badgeArray = selectedBadgesJson.split(",");
                for (String badge : badgeArray) {
                    if (!badge.trim().isEmpty()) {
                        selectedBadgeIds.add(badge.trim());
                    }
                }
            } catch (Exception e) {
                selectedBadgeIds.add("starter"); // Fallback
            }
        }
        
        if (selectedBadgeIds.isEmpty()) {
            selectedBadgeIds.add("starter"); // Ensure at least one badge
        }
        
        // Filter badges to only show selected ones
        filterSelectedBadges();
    }
    
    private void filterSelectedBadges() {
        if (badges == null) return;
        
        List<Badge> filteredBadges = new ArrayList<>();
        for (String selectedId : selectedBadgeIds) {
            for (Badge badge : badges) {
                if (badge.getType().equals(selectedId) && filteredBadges.size() < 3) {
                    filteredBadges.add(badge);
                    break;
                }
            }
        }
        badges = filteredBadges;
    }
    
    public void updateBadges(List<Badge> newBadges) {
        this.badges = newBadges;
        loadSelectedBadges(); // Refresh selected badges and filter
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_badge_selection, parent, false);
        return new BadgeViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = badges.get(position);
        
        // Set badge name
        holder.tvBadgeName.setText(badge.getName());
        
        // Set rarity based on level
        String rarity = getRarityText(badge.getLevel());
        holder.tvBadgeRarity.setText(rarity);
        
        // Set badge icon from the badge's icon resource
        if (badge.getIconResource() != 0) {
            holder.ivBadgeIcon.setImageResource(badge.getIconResource());
        } else {
            // Fallback icon based on badge type
            holder.ivBadgeIcon.setImageResource(getDefaultIcon(badge.getType()));
        }
          // Show "Selected" indicator for all badges (since these are all selected badges)
        boolean isSelected = selectedBadgeIds.contains(badge.getType());
        holder.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        
        // Always hide lock indicator since these are earned badges in profile view
        holder.ivLocked.setVisibility(View.GONE);
        
        // Set click listener for navigation to Customize Profile
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBadgeClick(badge);
            }
        });
          // All badges shown in profile are earned, so full opacity
        holder.itemView.setAlpha(1.0f);
        holder.itemView.setEnabled(true);
        
        // No visual scaling needed since all are selected
        holder.itemView.setScaleX(1.0f);
        holder.itemView.setScaleY(1.0f);
    }
      @Override
    public int getItemCount() {
        return Math.min(badges.size(), 3); // Show maximum 3 selected badges only
    }
    
    private String getRarityText(int level) {
        switch (level) {
            case 1:
                return "Common";
            case 2:
                return "Rare";
            case 3:
                return "Epic";
            default:
                return "Common";
        }
    }
    
    private int getDefaultIcon(String badgeType) {
        switch (badgeType) {
            case "starter":
                return R.drawable.ic_star;
            case "green_helper":
                return R.drawable.ic_leaf;
            case "eco_warrior":
                return R.drawable.ic_award;
            case "green_champion":
                return R.drawable.ic_award;
            case "earth_guardian":
                return R.drawable.ic_globe;
            case "expert_plogger":
                return R.drawable.ic_crown;
            case "eco_legend":
                return R.drawable.ic_crown;
            case "master_cleaner":
                return R.drawable.ic_cleaning;
            default:
                return R.drawable.ic_star;
        }
    }
    
    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBadgeIcon;
        ImageView ivSelected;
        ImageView ivLocked;
        TextView tvBadgeName;
        TextView tvBadgeRarity;
        
        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBadgeIcon = itemView.findViewById(R.id.ivBadgeIcon);
            ivSelected = itemView.findViewById(R.id.ivSelected);
            ivLocked = itemView.findViewById(R.id.ivLocked);
            tvBadgeName = itemView.findViewById(R.id.tvBadgeName);
            tvBadgeRarity = itemView.findViewById(R.id.tvBadgeRarity);
        }
    }
}

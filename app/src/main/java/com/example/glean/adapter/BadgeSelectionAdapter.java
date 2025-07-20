package com.example.glean.adapter;

import android.content.Context;
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

public class BadgeSelectionAdapter extends RecyclerView.Adapter<BadgeSelectionAdapter.BadgeViewHolder> {
    
    private final Context context;
    private List<Badge> badges;
    private List<String> selectedBadgeIds = new ArrayList<>();
    private final OnBadgeClickListener listener;
    
    public interface OnBadgeClickListener {
        void onBadgeClick(Badge badge);
    }
    
    public BadgeSelectionAdapter(Context context, List<Badge> badges, OnBadgeClickListener listener) {
        this.context = context;
        this.badges = badges;
        this.listener = listener;
    }
      public void updateBadges(List<Badge> newBadges, List<String> selectedIds) {
        this.badges = newBadges;
        this.selectedBadgeIds = new ArrayList<>(selectedIds);
        notifyDataSetChanged();
    }
    
    public void updateSelectedBadges(List<String> selectedIds) {
        this.selectedBadgeIds = new ArrayList<>(selectedIds);
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
        holder.ivBadgeIcon.setImageResource(badge.getIconResource());
          // Show/hide selection indicator
        boolean isSelected = selectedBadgeIds.contains(badge.getType());
        holder.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        
        // Show/hide lock indicator
        holder.ivLocked.setVisibility(badge.isEarned() ? View.GONE : View.VISIBLE);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBadgeClick(badge);
            }
        });
        
        // Update visual state
        float alpha = badge.isEarned() ? 1.0f : 0.6f;
        holder.itemView.setAlpha(alpha);
        holder.itemView.setEnabled(badge.isEarned());
    }
    
    @Override
    public int getItemCount() {
        return badges.size();
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

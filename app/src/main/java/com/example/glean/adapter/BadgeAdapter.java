package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.databinding.ItemBadgeBinding;

import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private final List<String> badges;
    
    public BadgeAdapter(List<String> badges) {
        this.badges = badges;
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBadgeBinding binding = ItemBadgeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BadgeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        String badge = badges.get(position);
        
        // Set badge image based on badge type
        switch (badge.toLowerCase()) {
            case "gold":
                holder.binding.ivBadge.setImageResource(R.drawable.badge_gold);
                holder.binding.tvBadgeName.setText("Gold");
                break;
            case "silver":
                holder.binding.ivBadge.setImageResource(R.drawable.badge_silver);
                holder.binding.tvBadgeName.setText("Silver");
                break;
            case "bronze":
                holder.binding.ivBadge.setImageResource(R.drawable.badge_bronze);
                holder.binding.tvBadgeName.setText("Bronze");
                break;
            case "first_run":
                holder.binding.ivBadge.setImageResource(R.drawable.badge_first_run);
                holder.binding.tvBadgeName.setText("First Run");
                break;
            case "eco_warrior":
                holder.binding.ivBadge.setImageResource(R.drawable.badge_eco_warrior);
                holder.binding.tvBadgeName.setText("Eco Warrior");
                break;
            case "trash_collector":
                holder.binding.ivBadge.setImageResource(R.drawable.badge_trash_collector);
                holder.binding.tvBadgeName.setText("Collector");
                break;
            default:
                holder.binding.ivBadge.setImageResource(R.drawable.badge_default);
                holder.binding.tvBadgeName.setText(badge);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        private final ItemBadgeBinding binding;
        
        public BadgeViewHolder(ItemBadgeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
package com.example.glean.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.Badge;

import java.util.ArrayList;
import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private List<Badge> badges;
    private Context context;
    private OnBadgeClickListener listener;

    public interface OnBadgeClickListener {
        void onBadgeClick(Badge badge, int position);
    }

    public BadgeAdapter(Context context, List<Badge> badges) {
        this.context = context;
        this.badges = badges != null ? badges : new ArrayList<>();
    }

    public BadgeAdapter(Context context, List<Badge> badges, OnBadgeClickListener listener) {
        this.context = context;
        this.badges = badges != null ? badges : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = badges.get(position);
        holder.bind(badge, position);
    }

    @Override
    public int getItemCount() {
        return badges != null ? badges.size() : 0;
    }

    public void updateBadges(List<Badge> newBadges) {
        if (newBadges == null) {
            newBadges = new ArrayList<>();
        }
        
        BadgeDiffCallback diffCallback = new BadgeDiffCallback(this.badges, newBadges);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        this.badges.clear();
        this.badges.addAll(newBadges);
        diffResult.dispatchUpdatesTo(this);
    }

    public void addBadge(Badge badge) {
        if (badge != null) {
            badges.add(badge);
            notifyItemInserted(badges.size() - 1);
        }
    }

    public void removeBadge(int position) {
        if (position >= 0 && position < badges.size()) {
            badges.remove(position);
            notifyItemRemoved(position);
        }
    }

    public List<Badge> getBadges() {
        return new ArrayList<>(badges);
    }

    class BadgeViewHolder extends RecyclerView.ViewHolder {
        private TextView tvBadgeName;
        private ImageView ivBadgeIcon;
        private View badgeContainer;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            // Try to find the TextView by the correct ID from item_badge.xml
            tvBadgeName = itemView.findViewById(R.id.tvBadgeName);
            ivBadgeIcon = itemView.findViewById(R.id.ivBadgeIcon);
            badgeContainer = itemView;
            
            // If tvBadgeName is null, try alternative IDs or create a simple fallback
            if (tvBadgeName == null) {
                // Look for any TextView in the layout as fallback
                tvBadgeName = itemView.findViewById(android.R.id.text1);
                if (tvBadgeName == null) {
                    // Create a simple TextView programmatically as last resort
                    tvBadgeName = new TextView(itemView.getContext());
                    tvBadgeName.setText("Badge");
                    tvBadgeName.setTextSize(12);
                    tvBadgeName.setTextColor(0xFF000000); // Black text
                    tvBadgeName.setPadding(8, 8, 8, 8);
                    
                    // Add to the parent if it's a ViewGroup
                    if (itemView instanceof ViewGroup) {
                        ((ViewGroup) itemView).addView(tvBadgeName);
                    }
                }
            }
        }

        public void bind(Badge badge, int position) {
            if (badge == null) return;

            // Set badge name safely
            if (tvBadgeName != null) {
                tvBadgeName.setText(badge.getName() != null ? badge.getName() : "Badge");
            }

            // Set badge icon if available
            if (ivBadgeIcon != null) {
                try {
                    int iconRes = getBadgeIcon(badge.getType(), badge.getLevel());
                    ivBadgeIcon.setImageResource(iconRes);
                    ivBadgeIcon.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    ivBadgeIcon.setVisibility(View.GONE);
                }
            }

            // Set badge background based on type and earned status
            setBadgeBackground(badge);

            // Set earned state styling
            float alpha = badge.isEarned() ? 1.0f : 0.5f;
            itemView.setAlpha(alpha);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    // Add click animation
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start();
                        })
                        .start();
                    
                    listener.onBadgeClick(badge, position);
                }
            });
        }

        private void setBadgeBackground(Badge badge) {
            if (badgeContainer != null) {
                try {
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setShape(GradientDrawable.RECTANGLE);
                    drawable.setCornerRadius(12f);
                    
                    int backgroundColor = getBadgeColor(badge.getType(), badge.getLevel(), badge.isEarned());
                    drawable.setColor(ContextCompat.getColor(context, backgroundColor));
                    
                    if (badge.isEarned()) {
                        drawable.setStroke(3, ContextCompat.getColor(context, R.color.badge_earned_border));
                    } else {
                        drawable.setStroke(2, ContextCompat.getColor(context, R.color.badge_locked_border));
                    }
                    
                    badgeContainer.setBackground(drawable);
                } catch (Exception e) {
                    // Fallback to simple background color
                    try {
                        badgeContainer.setBackgroundColor(ContextCompat.getColor(context, 
                            badge.isEarned() ? R.color.badge_default : R.color.badge_locked));
                    } catch (Exception ex) {
                        // Last resort fallback
                        badgeContainer.setBackgroundColor(badge.isEarned() ? 0xFF4CAF50 : 0xFFBDBDBD);
                    }
                }
            }
        }

        private int getBadgeIcon(String badgeType, int level) {
            // Always return a safe drawable that exists
            try {
                return R.drawable.ic_star;
            } catch (Exception e) {
                // Return Android's built-in star if our custom one doesn't exist
                return android.R.drawable.btn_star;
            }
        }

        private int getBadgeColor(String badgeType, int level, boolean isEarned) {
            if (!isEarned) {
                try {
                    return R.color.badge_locked;
                } catch (Exception e) {
                    return android.R.color.darker_gray;
                }
            }
            
            try {
                if (badgeType == null) return R.color.badge_default;
                
                switch (badgeType.toLowerCase()) {
                    case "distance":
                        return level >= 3 ? R.color.badge_gold : 
                               level >= 2 ? R.color.badge_silver : R.color.badge_bronze;
                    case "time":
                        return level >= 3 ? R.color.badge_gold : 
                               level >= 2 ? R.color.badge_silver : R.color.badge_bronze;
                    case "cleanup":
                        return level >= 3 ? R.color.badge_gold : 
                               level >= 2 ? R.color.badge_silver : R.color.badge_bronze;
                    case "streak":
                        return R.color.badge_streak;
                    case "achievement":
                        return R.color.badge_achievement;
                    default:
                        return R.color.badge_default;
                }
            } catch (Exception e) {
                // Fallback to Android built-in colors
                return android.R.color.holo_green_light;
            }
        }
    }

    // DiffUtil callback for efficient updates
    private static class BadgeDiffCallback extends DiffUtil.Callback {
        private final List<Badge> oldList;
        private final List<Badge> newList;

        public BadgeDiffCallback(List<Badge> oldList, List<Badge> newList) {
            this.oldList = oldList != null ? oldList : new ArrayList<>();
            this.newList = newList != null ? newList : new ArrayList<>();
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            try {
                Badge oldBadge = oldList.get(oldItemPosition);
                Badge newBadge = newList.get(newItemPosition);
                return oldBadge.getId() == newBadge.getId();
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            try {
                Badge oldBadge = oldList.get(oldItemPosition);
                Badge newBadge = newList.get(newItemPosition);
                
                return (oldBadge.getName() != null ? oldBadge.getName().equals(newBadge.getName()) : newBadge.getName() == null) &&
                       oldBadge.isEarned() == newBadge.isEarned() &&
                       oldBadge.getLevel() == newBadge.getLevel() &&
                       (oldBadge.getType() != null ? oldBadge.getType().equals(newBadge.getType()) : newBadge.getType() == null);
            } catch (Exception e) {
                return false;
            }
        }
    }
}
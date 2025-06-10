package com.example.glean.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.model.RankingEntity;
import de.hdodenhof.circleimageview.CircleImageView;

import java.io.File;
import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {
    
    private List<RankingEntity> rankings;
    
    public RankingAdapter(List<RankingEntity> rankings) {
        this.rankings = rankings;
    }
    
    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking, parent, false);
        return new RankingViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        RankingEntity ranking = rankings.get(position);
        holder.bind(ranking);
    }
    
    @Override
    public int getItemCount() {
        return rankings.size();
    }    class RankingViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUsername, tvPoints, tvRankingPosition;
        private CardView cardRanking;
        private CircleImageView ivUserProfile;
        private ImageView ivCrownIcon;
        
        public RankingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUserName);
            tvPoints = itemView.findViewById(R.id.tvPoints);
            tvRankingPosition = itemView.findViewById(R.id.tvRankingPosition);
            ivUserProfile = itemView.findViewById(R.id.ivUserProfile);
            ivCrownIcon = itemView.findViewById(R.id.ivCrownIcon);
            cardRanking = itemView.findViewById(R.id.cardRanking);
        }public void bind(RankingEntity ranking) {
            tvUsername.setText(ranking.getUsername());
            tvPoints.setText(ranking.getPoints() + " poin");
            
            // Display ranking position
            tvRankingPosition.setText(String.valueOf(ranking.getPosition()));
            
            // Load profile image
            if (ranking.getAvatar() != null && !ranking.getAvatar().isEmpty()) {
                // Load from file path
                File imageFile = new File(ranking.getAvatar());
                if (imageFile.exists()) {
                    Glide.with(ivUserProfile.getContext())
                            .load(imageFile)
                            .placeholder(R.drawable.profile_placeholder)
                            .error(R.drawable.profile_placeholder)
                            .into(ivUserProfile);
                } else {
                    // File doesn't exist, load default
                    ivUserProfile.setImageResource(R.drawable.profile_placeholder);
                }
            } else {
                // No profile image set, load default
                ivUserProfile.setImageResource(R.drawable.profile_placeholder);
            }
              // Set border color based on ranking position
            Context context = itemView.getContext();
            int borderColor;
            if (ranking.getPosition() == 1) {
                borderColor = ContextCompat.getColor(context, R.color.badge_gold);
                tvRankingPosition.setTextColor(ContextCompat.getColor(context, R.color.badge_gold));
            } else if (ranking.getPosition() == 2) {
                borderColor = ContextCompat.getColor(context, R.color.badge_silver);
                tvRankingPosition.setTextColor(ContextCompat.getColor(context, R.color.badge_silver));
            } else if (ranking.getPosition() == 3) {
                borderColor = ContextCompat.getColor(context, R.color.badge_bronze);
                tvRankingPosition.setTextColor(ContextCompat.getColor(context, R.color.badge_bronze));
            } else {
                borderColor = ContextCompat.getColor(context, R.color.divider_color);
                tvRankingPosition.setTextColor(ContextCompat.getColor(context, R.color.card_text_secondary));
            }            // Apply border color to CircleImageView
            ivUserProfile.setBorderColor(borderColor);
            ivUserProfile.setBorderWidth(ranking.getPosition() <= 3 ? 6 : 3); // Thicker border for top 3
            
            // Show appropriate crown icon for top 3 positions
            if (ranking.getPosition() == 1) {
                ivCrownIcon.setVisibility(View.VISIBLE);
                ivCrownIcon.setImageResource(R.drawable.ic_crown_gold);
            } else if (ranking.getPosition() == 2) {
                ivCrownIcon.setVisibility(View.VISIBLE);
                ivCrownIcon.setImageResource(R.drawable.ic_crown_silver);
            } else if (ranking.getPosition() == 3) {
                ivCrownIcon.setVisibility(View.VISIBLE);
                ivCrownIcon.setImageResource(R.drawable.ic_crown_bronze);
            } else {
                ivCrownIcon.setVisibility(View.GONE);
            }
            
            // Highlight current user
            if (ranking.isCurrentUser()) {
                // Keep original background but add slight highlight
                itemView.setAlpha(0.9f);
            } else {
                itemView.setAlpha(1.0f);
            }
        }
    }
}

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
import com.example.glean.model.RankingUser;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {
    
    private Context context;
    private List<RankingUser> rankingList;
    private boolean isPointsRanking; // true for points, false for distance
    private String currentUserId;
    
    public RankingAdapter(Context context, List<RankingUser> rankingList, boolean isPointsRanking, String currentUserId) {
        this.context = context;
        this.rankingList = rankingList;
        this.isPointsRanking = isPointsRanking;
        this.currentUserId = currentUserId;
    }
    
    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ranking, parent, false);
        return new RankingViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        RankingUser user = rankingList.get(position);
        holder.bind(user, position + 1);
    }
    
    @Override
    public int getItemCount() {
        return rankingList.size();
    }
    
    public void updateData(List<RankingUser> newRankingList) {
        this.rankingList = newRankingList;
        notifyDataSetChanged();
    }    class RankingViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUsername, tvPoints, tvRankingPosition, tvStats, tvScoreUnit;
        private CardView cardRanking;
        private CircleImageView ivUserProfile;
        private ImageView ivCrown;
        
        public RankingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUserName);
            tvPoints = itemView.findViewById(R.id.tvPoints);
            tvRankingPosition = itemView.findViewById(R.id.tvRankingPosition);
            tvStats = itemView.findViewById(R.id.tvStats);
            tvScoreUnit = itemView.findViewById(R.id.tvScoreUnit);
            ivUserProfile = itemView.findViewById(R.id.ivUserProfile);
            ivCrown = itemView.findViewById(R.id.ivCrown);
            cardRanking = itemView.findViewById(R.id.cardRanking);
        }

        public void bind(RankingUser user, int position) {
            // Set position
            tvRankingPosition.setText("#" + position);
            
            // Show crown for top 3
            if (position <= 3) {
                ivCrown.setVisibility(View.VISIBLE);
                // Different crown colors for different positions
                switch (position) {
                    case 1: // Gold
                        ivCrown.setColorFilter(ContextCompat.getColor(context, R.color.gold));
                        break;
                    case 2: // Silver
                        ivCrown.setColorFilter(ContextCompat.getColor(context, R.color.silver));
                        break;
                    case 3: // Bronze
                        ivCrown.setColorFilter(ContextCompat.getColor(context, R.color.bronze));
                        break;
                }
            } else {
                ivCrown.setVisibility(View.GONE);
            }
            
            // Set username
            String username = user.getUsername();
            if (username == null || username.trim().isEmpty()) {
                username = "User Baru";
            }
            tvUsername.setText(username);
            
            // Set profile image
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_user_avatar)
                        .error(R.drawable.ic_user_avatar)
                        .into(ivUserProfile);
            } else {
                ivUserProfile.setImageResource(R.drawable.ic_user_avatar);
            }
            
            // Set stats
            tvStats.setText(user.getFormattedStats());
            
            // Set score based on ranking type
            if (isPointsRanking) {
                tvPoints.setText(String.valueOf(user.getTotalPoints()));
                tvScoreUnit.setText("poin");
            } else {
                tvPoints.setText(String.format("%.1f", user.getTotalDistance() / 1000.0));
                tvScoreUnit.setText("km");
            }
            
            // Set border color based on ranking position
            int borderColor;
            if (position == 1) {
                borderColor = ContextCompat.getColor(context, R.color.gold);
                tvRankingPosition.setTextColor(ContextCompat.getColor(context, R.color.gold));
            } else if (position == 2) {
                borderColor = ContextCompat.getColor(context, R.color.silver);
                tvRankingPosition.setTextColor(ContextCompat.getColor(context, R.color.silver));
            } else if (position == 3) {
                borderColor = ContextCompat.getColor(context, R.color.bronze);
                tvRankingPosition.setTextColor(ContextCompat.getColor(context, R.color.bronze));
            } else {
                borderColor = ContextCompat.getColor(context, R.color.primary_color);
                tvRankingPosition.setTextColor(ContextCompat.getColor(context, R.color.primary_color));
            }

            // Apply border color to CircleImageView
            ivUserProfile.setBorderColor(borderColor);
            ivUserProfile.setBorderWidth(position <= 3 ? 6 : 3); // Thicker border for top 3
            
            // Highlight current user
            if (currentUserId != null && currentUserId.equals(user.getUserId())) {
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.highlight_background));
                itemView.setAlpha(0.95f);
            } else {
                itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                itemView.setAlpha(1.0f);
            }
        }
    }
}

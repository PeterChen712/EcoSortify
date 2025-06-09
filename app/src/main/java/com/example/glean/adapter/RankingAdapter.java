package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.model.RankingEntity;

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
        private TextView tvPosition, tvUsername, tvPoints;
        private ImageView ivAvatar;
        private CardView cardRanking;
        
        public RankingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPosition = itemView.findViewById(R.id.tvRank);
            tvUsername = itemView.findViewById(R.id.tvUserName);
            tvPoints = itemView.findViewById(R.id.tvPoints);
            ivAvatar = itemView.findViewById(R.id.ivUserProfile);
            cardRanking = itemView.findViewById(R.id.cardRanking);
        }
          public void bind(RankingEntity ranking) {
            tvPosition.setText("#" + ranking.getPosition());
            tvUsername.setText(ranking.getUsername());
            tvPoints.setText(ranking.getPoints() + " poin");
            
            // Load profile image
            if (ranking.getAvatar() != null && !ranking.getAvatar().isEmpty()) {
                // Load from file path
                File imageFile = new File(ranking.getAvatar());
                if (imageFile.exists()) {
                    Glide.with(ivAvatar.getContext())
                            .load(imageFile)
                            .placeholder(R.drawable.profile_placeholder)
                            .error(R.drawable.profile_placeholder)
                            .circleCrop()
                            .into(ivAvatar);
                } else {
                    // File doesn't exist, load default
                    ivAvatar.setImageResource(R.drawable.profile_placeholder);
                }
            } else {
                // No profile image set, load default
                ivAvatar.setImageResource(R.drawable.profile_placeholder);
            }
            
            // Set border color based on ranking position
            if (ranking.getPosition() == 1) {
                cardRanking.setBackgroundResource(R.drawable.ranking_gold_border);
            } else if (ranking.getPosition() == 2) {
                cardRanking.setBackgroundResource(R.drawable.ranking_silver_border);
            } else if (ranking.getPosition() == 3) {
                cardRanking.setBackgroundResource(R.drawable.ranking_bronze_border);
            } else {
                cardRanking.setBackgroundResource(R.drawable.ranking_normal_border);
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

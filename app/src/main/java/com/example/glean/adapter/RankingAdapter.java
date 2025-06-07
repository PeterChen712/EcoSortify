package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.RankingEntity;

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
    }
    
    class RankingViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPosition, tvUsername, tvPoints, tvStats;
        private ImageView ivAvatar, ivBadge;
          public RankingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPosition = itemView.findViewById(R.id.tvRank);
            tvUsername = itemView.findViewById(R.id.tvUserName);
            tvPoints = itemView.findViewById(R.id.tvPoints);
            tvStats = itemView.findViewById(R.id.tvTrashCollected);
            ivAvatar = itemView.findViewById(R.id.ivUserProfile);
            ivBadge = itemView.findViewById(R.id.ivBadge);
        }
        
        public void bind(RankingEntity ranking) {
            tvPosition.setText("#" + ranking.getPosition());
            tvUsername.setText(ranking.getUsername());
            tvPoints.setText(ranking.getPoints() + " poin");
            
            String stats = String.format("%.1fkg • %.1fkm • %d badge", 
                ranking.getTrashCollected(), 
                ranking.getDistance(), 
                ranking.getBadges());
            tvStats.setText(stats);
            
            // Show special badge for top 3
            if (ranking.getPosition() <= 3) {
                ivBadge.setVisibility(View.VISIBLE);
                switch (ranking.getPosition()) {
                    case 1:
                        ivBadge.setImageResource(R.drawable.ic_medal_gold);
                        break;
                    case 2:
                        ivBadge.setImageResource(R.drawable.ic_medal_silver);
                        break;
                    case 3:
                        ivBadge.setImageResource(R.drawable.ic_medal_bronze);
                        break;
                }
            } else {
                ivBadge.setVisibility(View.GONE);
            }
            
            // Highlight current user
            if (ranking.isCurrentUser()) {
                itemView.setBackgroundResource(R.drawable.ranking_current_user_background);
            } else {
                itemView.setBackgroundResource(R.drawable.ranking_normal_background);
            }
        }
    }
}

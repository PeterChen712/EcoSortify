package com.example.glean.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.Tip;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class TipsAdapter extends RecyclerView.Adapter<TipsAdapter.TipViewHolder> {
    
    private List<Tip> tips;
    private int[] cardColors = {
        R.color.tip_card_green,
        R.color.tip_card_blue,
        R.color.tip_card_yellow,
        R.color.tip_card_teal,
        R.color.tip_card_purple,
        R.color.tip_card_indigo
    };
    
    public TipsAdapter(List<Tip> tips) {
        this.tips = tips;
    }
    
    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tip_card, parent, false);
        return new TipViewHolder(view);
    }
      @Override
    public void onBindViewHolder(@NonNull TipViewHolder holder, int position) {
        Tip tip = tips.get(position);
        holder.bind(tip, position, cardColors);
    }
    
    @Override
    public int getItemCount() {
        return tips.size();
    }
      static class TipViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView tipCard;
        private TextView tipTitle;
        private TextView tipDescription;
        
        public TipViewHolder(@NonNull View itemView) {
            super(itemView);
            tipCard = itemView.findViewById(R.id.tip_card);
            tipTitle = itemView.findViewById(R.id.tip_title);
            tipDescription = itemView.findViewById(R.id.tip_description);
        }
        
        public void bind(Tip tip, int position, int[] cardColors) {
            tipTitle.setText(tip.getTitle());
            tipDescription.setText(tip.getDescription());
            
            // Set different background color based on position
            int colorIndex = position % cardColors.length;
            int color = ContextCompat.getColor(itemView.getContext(), cardColors[colorIndex]);
            tipCard.setCardBackgroundColor(ColorStateList.valueOf(color));
        }
    }
}

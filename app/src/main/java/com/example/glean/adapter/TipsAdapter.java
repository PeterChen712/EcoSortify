package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.Tip;

import java.util.List;

public class TipsAdapter extends RecyclerView.Adapter<TipsAdapter.TipViewHolder> {
    
    private List<Tip> tips;
    
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
        holder.bind(tip);
    }
    
    @Override
    public int getItemCount() {
        return tips.size();
    }
    
    static class TipViewHolder extends RecyclerView.ViewHolder {
        private ImageView tipIcon;
        private TextView tipTitle;
        private TextView tipDescription;
        
        public TipViewHolder(@NonNull View itemView) {
            super(itemView);
            tipIcon = itemView.findViewById(R.id.tip_icon);
            tipTitle = itemView.findViewById(R.id.tip_title);
            tipDescription = itemView.findViewById(R.id.tip_description);
        }
        
        public void bind(Tip tip) {
            tipIcon.setImageResource(tip.getIconResId());
            tipTitle.setText(tip.getTitle());
            tipDescription.setText(tip.getDescription());
        }
    }
}

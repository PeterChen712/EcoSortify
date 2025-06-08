package com.example.glean.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.ProfileSkin;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class SkinSelectionAdapter extends RecyclerView.Adapter<SkinSelectionAdapter.SkinViewHolder> {
    
    private final Context context;
    private List<ProfileSkin> skins;
    private String selectedSkinId;
    private int userPoints;
    private final OnSkinClickListener listener;
    
    public interface OnSkinClickListener {
        void onSkinClick(ProfileSkin skin);
        void onSkinPurchase(ProfileSkin skin);
    }
    
    public SkinSelectionAdapter(Context context, List<ProfileSkin> skins, OnSkinClickListener listener) {
        this.context = context;
        this.skins = skins;
        this.listener = listener;
    }
    
    public void updateSkins(List<ProfileSkin> newSkins, String selectedId, int points) {
        this.skins = newSkins;
        this.selectedSkinId = selectedId;
        this.userPoints = points;
        notifyDataSetChanged();
    }
    
    public void updateSelectedSkin(String selectedId) {
        this.selectedSkinId = selectedId;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public SkinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_skin_selection, parent, false);
        return new SkinViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SkinViewHolder holder, int position) {
        ProfileSkin skin = skins.get(position);
        
        // Set skin preview
        holder.viewSkinPreview.setBackgroundResource(skin.getDrawableResource());
        
        // Set skin name
        holder.tvSkinName.setText(skin.getName());
        
        // Show/hide selection indicator
        boolean isSelected = skin.getId().equals(selectedSkinId);
        holder.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        
        if (skin.isUnlocked()) {
            // Skin is owned
            holder.layoutLocked.setVisibility(View.GONE);
            holder.tvStatus.setText("Owned");
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.environmental_green));
            holder.btnPurchase.setVisibility(View.GONE);
            
            // Set click listener for selection
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSkinClick(skin);
                }
            });
            
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setEnabled(true);
            
        } else {
            // Skin is locked
            holder.layoutLocked.setVisibility(View.VISIBLE);
            holder.tvPrice.setText(skin.getPrice() + " pts");
            
            // Check if user can afford it
            boolean canAfford = userPoints >= skin.getPrice();
            
            if (canAfford) {
                holder.tvStatus.setText(skin.getPrice() + " points");
                holder.tvStatus.setTextColor(context.getResources().getColor(R.color.accent));
                holder.btnPurchase.setVisibility(View.VISIBLE);
                holder.btnPurchase.setEnabled(true);
                holder.btnPurchase.setText("Buy");
                
                // Set purchase click listener
                holder.btnPurchase.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSkinPurchase(skin);
                    }
                });
                
                holder.itemView.setAlpha(1.0f);
                
            } else {
                holder.tvStatus.setText("Need " + skin.getPrice() + " points");
                holder.tvStatus.setTextColor(context.getResources().getColor(R.color.text_secondary));
                holder.btnPurchase.setVisibility(View.VISIBLE);
                holder.btnPurchase.setEnabled(false);
                holder.btnPurchase.setText("Locked");
                
                holder.itemView.setAlpha(0.6f);
            }
            
            // Disable selection for locked skins
            holder.itemView.setOnClickListener(null);
            holder.itemView.setEnabled(canAfford);
        }
    }
    
    @Override
    public int getItemCount() {
        return skins.size();
    }
    
    static class SkinViewHolder extends RecyclerView.ViewHolder {
        View viewSkinPreview;
        ImageView ivSelected;
        LinearLayout layoutLocked;
        TextView tvPrice;
        TextView tvSkinName;
        TextView tvStatus;
        MaterialButton btnPurchase;
        
        public SkinViewHolder(@NonNull View itemView) {
            super(itemView);
            viewSkinPreview = itemView.findViewById(R.id.viewSkinPreview);
            ivSelected = itemView.findViewById(R.id.ivSelected);
            layoutLocked = itemView.findViewById(R.id.layoutLocked);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSkinName = itemView.findViewById(R.id.tvSkinName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnPurchase = itemView.findViewById(R.id.btnPurchase);
        }
    }
}

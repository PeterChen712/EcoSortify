package com.example.glean.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.model.ProfileSkin;

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
    
    private void showPurchaseConfirmationDialog(ProfileSkin skin) {
        new AlertDialog.Builder(context)
                .setTitle("Konfirmasi Pembelian")
                .setMessage("Apakah Anda yakin ingin membeli skin \"" + skin.getName() + "\" seharga " + skin.getPrice() + " poin?")
                .setPositiveButton("Ya, Beli", (dialog, which) -> {
                    if (listener != null) {
                        listener.onSkinPurchase(skin);
                    }
                })
                .setNegativeButton("Batal", null)
                .setIcon(R.drawable.ic_shopping_cart)
                .show();
    }
    
    @NonNull
    @Override
    public SkinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_skin_selection, parent, false);
        return new SkinViewHolder(view);
    }    @Override
    public void onBindViewHolder(@NonNull SkinViewHolder holder, int position) {
        ProfileSkin skin = skins.get(position);
        
        // Set skin preview with Glide support for GIF
        if (skin.isGif()) {
            // Use Glide for GIF animations
            Glide.with(context)
                    .asGif()
                    .load(skin.getDrawableResource())
                    .centerCrop()
                    .into(holder.viewSkinPreview);
        } else {
            // Use regular method for static images
            Glide.with(context)
                    .load(skin.getDrawableResource())
                    .centerCrop()
                    .into(holder.viewSkinPreview);
        }
        
        // Set skin name
        holder.tvSkinName.setText(skin.getName());
        
        // Show/hide selection indicator
        boolean isSelected = skin.getId().equals(selectedSkinId);
        holder.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
          if (skin.isUnlocked()) {
            // Skin is owned - show checkmark icon
            holder.tvPriceInfo.setVisibility(View.GONE);
            
            // Show owned status icon
            holder.ivStatusIcon.setVisibility(View.VISIBLE);
            holder.ivStatusIcon.setImageResource(R.drawable.ic_check);
            holder.ivStatusIcon.setBackgroundResource(R.drawable.circle_green_background);
            holder.ivStatusIcon.setContentDescription("Owned");
            
            // Add tooltip on click
            holder.ivStatusIcon.setOnClickListener(v -> {
                android.widget.Toast.makeText(context, "Skin dimiliki", android.widget.Toast.LENGTH_SHORT).show();
            });
            
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
            // Check if user can afford it
            boolean canAfford = userPoints >= skin.getPrice();
            
            if (canAfford) {
                // Show price info only
                holder.tvPriceInfo.setVisibility(View.VISIBLE);
                holder.tvPriceInfo.setText(skin.getPrice() + " points");
                holder.tvPriceInfo.setTextColor(context.getResources().getColor(R.color.accent));
                
                // Show shopping cart status icon
                holder.ivStatusIcon.setVisibility(View.VISIBLE);
                holder.ivStatusIcon.setImageResource(R.drawable.ic_shopping_cart);
                holder.ivStatusIcon.setBackgroundResource(R.drawable.circle_white_background);
                holder.ivStatusIcon.setContentDescription("Available for Purchase");
                
                // Set purchase click listener on icon with confirmation dialog
                holder.ivStatusIcon.setOnClickListener(v -> {
                    showPurchaseConfirmationDialog(skin);
                });
                
                holder.itemView.setAlpha(1.0f);
                
            } else {
                // Show need more points info
                holder.tvPriceInfo.setVisibility(View.VISIBLE);
                holder.tvPriceInfo.setText("Need " + skin.getPrice() + " points");
                holder.tvPriceInfo.setTextColor(context.getResources().getColor(R.color.text_secondary));
                
                // Show lock status icon
                holder.ivStatusIcon.setVisibility(View.VISIBLE);
                holder.ivStatusIcon.setImageResource(R.drawable.ic_lock);
                holder.ivStatusIcon.setBackgroundResource(R.drawable.circle_gray_background);
                holder.ivStatusIcon.setContentDescription("Locked");
                
                // Add tooltip on click
                holder.ivStatusIcon.setOnClickListener(v -> {
                    android.widget.Toast.makeText(context, "Terkunci - Butuh " + skin.getPrice() + " poin", android.widget.Toast.LENGTH_SHORT).show();
                });
                
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
    }    static class SkinViewHolder extends RecyclerView.ViewHolder {
        ImageView viewSkinPreview;
        ImageView ivSelected;
        ImageView ivStatusIcon;
        TextView tvSkinName;
        TextView tvPriceInfo;
        
        public SkinViewHolder(@NonNull View itemView) {
            super(itemView);
            viewSkinPreview = itemView.findViewById(R.id.viewSkinPreview);
            ivSelected = itemView.findViewById(R.id.ivSelected);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            tvSkinName = itemView.findViewById(R.id.tvSkinName);
            tvPriceInfo = itemView.findViewById(R.id.tvPriceInfo);
        }
    }
}

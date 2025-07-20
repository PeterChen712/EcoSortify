package com.example.glean.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.ProfileSkin;

import java.util.List;

public class SkinAdapter extends RecyclerView.Adapter<SkinAdapter.SkinViewHolder> {
    
    private Context context;
    private List<ProfileSkin> skinList;
    private OnSkinClickListener listener;
    
    public interface OnSkinClickListener {
        void onSkinClick(ProfileSkin skin);
    }
    
    public SkinAdapter(Context context, List<ProfileSkin> skinList, OnSkinClickListener listener) {
        this.context = context;
        this.skinList = skinList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public SkinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_skin, parent, false);
        return new SkinViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SkinViewHolder holder, int position) {
        ProfileSkin skin = skinList.get(position);
        holder.bind(skin);
    }
    
    @Override
    public int getItemCount() {
        return skinList.size();
    }
    
    class SkinViewHolder extends RecyclerView.ViewHolder {
        private View skinPreview;
        private TextView tvSkinName;
        private TextView tvSkinPrice;
        private ImageView ivSkinStatus;
        
        public SkinViewHolder(@NonNull View itemView) {
            super(itemView);
            skinPreview = itemView.findViewById(R.id.skinPreview);
            tvSkinName = itemView.findViewById(R.id.tvSkinName);
            tvSkinPrice = itemView.findViewById(R.id.tvSkinPrice);
            ivSkinStatus = itemView.findViewById(R.id.ivSkinStatus);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onSkinClick(skinList.get(position));
                    }
                }
            });
        }
        
        public void bind(ProfileSkin skin) {
            // Set skin preview background
            skinPreview.setBackgroundResource(skin.getDrawableResource());
            
            // Set skin name
            tvSkinName.setText(skin.getName());
            
            // Set skin price
            tvSkinPrice.setText(skin.getPriceText());
            
            // Set status icon
            if (skin.isSelected()) {
                ivSkinStatus.setImageResource(R.drawable.ic_check);
                ivSkinStatus.setColorFilter(context.getResources().getColor(R.color.primary_green, null));
            } else if (skin.isUnlocked()) {
                ivSkinStatus.setImageResource(R.drawable.ic_check);
                ivSkinStatus.setColorFilter(context.getResources().getColor(R.color.text_secondary, null));
            } else {
                ivSkinStatus.setImageResource(R.drawable.ic_lock);
                ivSkinStatus.setColorFilter(context.getResources().getColor(R.color.text_secondary, null));
            }
            
            // Set card appearance based on status
            float alpha = skin.isUnlocked() ? 1.0f : 0.6f;
            itemView.setAlpha(alpha);
        }
    }
}

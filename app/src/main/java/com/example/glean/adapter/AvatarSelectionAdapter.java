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
import com.example.glean.model.Avatar;
import com.example.glean.util.AvatarManager;

import java.util.List;

public class AvatarSelectionAdapter extends RecyclerView.Adapter<AvatarSelectionAdapter.AvatarViewHolder> {
    
    private Context context;
    private List<Avatar> avatars;
    private OnAvatarClickListener listener;
    private String selectedAvatarId = "default";
    
    public interface OnAvatarClickListener {
        void onAvatarClick(Avatar avatar);
    }
    
    public AvatarSelectionAdapter(Context context, List<Avatar> avatars, OnAvatarClickListener listener) {
        this.context = context;
        this.avatars = avatars;
        this.listener = listener;
    }
    
    public void setSelectedAvatarId(String avatarId) {
        this.selectedAvatarId = avatarId;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_avatar_selection, parent, false);
        return new AvatarViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        Avatar avatar = avatars.get(position);
        holder.bind(avatar);
    }
    
    @Override
    public int getItemCount() {
        return avatars != null ? avatars.size() : 0;
    }
    
    class AvatarViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatar;
        private TextView tvAvatarName;
        private TextView tvRequiredPoints;
        private View selectionIndicator;
        private View lockOverlay;
        
        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvAvatarName = itemView.findViewById(R.id.tv_avatar_name);
            tvRequiredPoints = itemView.findViewById(R.id.tv_required_points);
            selectionIndicator = itemView.findViewById(R.id.selection_indicator);
            lockOverlay = itemView.findViewById(R.id.lock_overlay);
        }
        
        public void bind(Avatar avatar) {
            // Load avatar image
            AvatarManager.loadAvatarIntoImageView(context, ivAvatar, avatar);
            
            // Set avatar name
            tvAvatarName.setText(avatar.getName());
            
            // Handle selection state
            boolean isSelected = avatar.getId().equals(selectedAvatarId);
            selectionIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            
            // Handle unlock state
            if (avatar.isUnlocked()) {
                lockOverlay.setVisibility(View.GONE);
                tvRequiredPoints.setVisibility(View.GONE);
                itemView.setAlpha(1.0f);
                itemView.setClickable(true);
                
                // Set click listener for unlocked avatars
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAvatarClick(avatar);
                    }
                });
            } else {
                lockOverlay.setVisibility(View.VISIBLE);
                tvRequiredPoints.setVisibility(View.VISIBLE);
                tvRequiredPoints.setText(String.format("%d points required", avatar.getRequiredPoints()));
                itemView.setAlpha(0.6f);
                itemView.setClickable(false);
                itemView.setOnClickListener(null);
            }
            
            // Add border for selected item
            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.avatar_selection_border);
            } else {
                itemView.setBackgroundResource(R.drawable.avatar_normal_border);
            }
        }
    }
}

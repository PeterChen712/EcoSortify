package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.databinding.ItemTrashBinding;
import com.example.glean.model.TrashEntity;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.TrashViewHolder> {

    private final List<TrashEntity> trashList;
    private final OnTrashClickListener listener;

    public interface OnTrashClickListener {
        void onTrashItemClick(TrashEntity trash);
    }

    public TrashAdapter(List<TrashEntity> trashList, OnTrashClickListener listener) {
        this.trashList = trashList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrashBinding binding = ItemTrashBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TrashViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashViewHolder holder, int position) {
        TrashEntity trash = trashList.get(position);
        holder.bind(trash);
    }

    @Override
    public int getItemCount() {
        return trashList.size();
    }

    class TrashViewHolder extends RecyclerView.ViewHolder {
        private final ItemTrashBinding binding;

        public TrashViewHolder(ItemTrashBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TrashEntity trash) {
            // Set trash type
            String trashType = trash.getTrashType();
            if (trashType == null || trashType.isEmpty()) {
                trashType = "Unknown";
            }
            binding.tvTrashType.setText(trashType);

            // Set description if available
            String description = trash.getDescription();
            if (description != null && !description.isEmpty()) {
                binding.tvDescription.setText(description);
                binding.tvDescription.setVisibility(ViewGroup.VISIBLE);
            } else {
                binding.tvDescription.setVisibility(ViewGroup.GONE);
            }

            // Set timestamp
            String timeStr = formatTimestamp(trash.getTimestamp());
            binding.tvTimestamp.setText(timeStr);

            // Set location if available
            if (trash.getLatitude() != 0 && trash.getLongitude() != 0) {
                String location = String.format(Locale.getDefault(), 
                    "%.6f, %.6f", trash.getLatitude(), trash.getLongitude());
                binding.tvLocation.setText(location);
                binding.tvLocation.setVisibility(ViewGroup.VISIBLE);
            } else {
                binding.tvLocation.setVisibility(ViewGroup.GONE);
            }

            // Load trash image if available
            String imagePath = trash.getImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Picasso.get()
                            .load(imageFile)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(binding.ivTrashImage);
                } else {
                    binding.ivTrashImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                binding.ivTrashImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Set ML prediction info if available
            if (trash.getMlLabel() != null && !trash.getMlLabel().isEmpty()) {
                String mlInfo = String.format(Locale.getDefault(),
                    "ML: %s (%.1f%%)", trash.getMlLabel(), trash.getConfidence() * 100);
                binding.tvMlPrediction.setText(mlInfo);
                binding.tvMlPrediction.setVisibility(ViewGroup.VISIBLE);
            } else {
                binding.tvMlPrediction.setVisibility(ViewGroup.GONE);
            }

            // Set classification status
            if (trashType.equals("Unknown")) {
                binding.tvStatus.setText("Needs Classification");
                binding.tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
            } else {
                binding.tvStatus.setText("Classified");
                binding.tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
            }

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTrashItemClick(trash);
                }
            });
        }

        private String formatTimestamp(long timestamp) {
            try {
                Date date = new Date(timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                return sdf.format(date);
            } catch (Exception e) {
                return "Unknown time";
            }
        }
    }
}
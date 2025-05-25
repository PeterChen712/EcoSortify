package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.databinding.ItemRecentActivityBinding;
import com.example.glean.model.RecordEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    private final List<RecordEntity> recordList;
    private final OnRecordClickListener listener;

    public interface OnRecordClickListener {
        void onRecordClick(RecordEntity record);
    }

    public RecordAdapter(List<RecordEntity> recordList, OnRecordClickListener listener) {
        this.recordList = recordList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecentActivityBinding binding = ItemRecentActivityBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RecordViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        RecordEntity record = recordList.get(position);
        holder.bind(record, listener);
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecentActivityBinding binding;

        public RecordViewHolder(ItemRecentActivityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RecordEntity record, OnRecordClickListener listener) {
            // Format date
            binding.tvDate.setText(record.getDate());
            
            // Format location - use notes or a placeholder since getLocation() doesn't exist
            String location = record.getNotes() != null && !record.getNotes().isEmpty() 
                ? record.getNotes() 
                : "Plogging Session";
            binding.tvLocation.setText(location);
            
            // Format distance - use totalDistance instead of getDistance()
            float distanceKm = record.getTotalDistance() / 1000f;
            binding.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));
            
            // Format duration - convert milliseconds to seconds first
            long durationSeconds = record.getDuration() / 1000;
            int hours = (int) (durationSeconds / 3600);
            int minutes = (int) ((durationSeconds % 3600) / 60);
            int seconds = (int) (durationSeconds % 60);
            String durationStr;
            if (hours > 0) {
                durationStr = String.format(Locale.getDefault(), "%d hr %d min", hours, minutes);
            } else {
                durationStr = String.format(Locale.getDefault(), "%d min %d sec", minutes, seconds);
            }
            binding.tvDuration.setText(durationStr);
            
            // Format points - use trashCount as points since getPlogPoints() doesn't exist
            binding.tvPoints.setText(String.valueOf(record.getTrashCount()));
            
            // Set click listener
            binding.cardActivity.setOnClickListener(v -> listener.onRecordClick(record));
        }
    }
}
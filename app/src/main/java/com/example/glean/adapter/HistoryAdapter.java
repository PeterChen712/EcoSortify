package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.databinding.ItemHistoryBinding;
import com.example.glean.model.RecordEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<RecordEntity> recordList;
    private final OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onHistoryItemClick(RecordEntity record);
    }

    public HistoryAdapter(List<RecordEntity> recordList, OnHistoryClickListener listener) {
        this.recordList = recordList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryBinding binding = ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        RecordEntity record = recordList.get(position);
        holder.bind(record, listener);
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryBinding binding;

        public HistoryViewHolder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RecordEntity record, OnHistoryClickListener listener) {
            // Set date
            binding.tvDate.setText(record.getDate());
            
            // Set location - use notes or a placeholder since getLocation() doesn't exist
            String location = record.getNotes() != null && !record.getNotes().isEmpty() 
                ? record.getNotes() 
                : "Plogging Session";
            binding.tvLocation.setText(location);
            
            // Format start time
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String startTime = sdf.format(new Date(record.getStartTime()));
            binding.tvTime.setText(startTime);
            
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
            
            // Set points - use trashCount as points since getPlogPoints() doesn't exist
            binding.tvPoints.setText(String.valueOf(record.getTrashCount()));
            
            // Set click listener
            binding.cardHistory.setOnClickListener(v -> listener.onHistoryItemClick(record));
        }
    }
}
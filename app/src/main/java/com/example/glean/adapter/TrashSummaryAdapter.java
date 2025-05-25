package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.databinding.ItemTrashSummaryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrashSummaryAdapter extends RecyclerView.Adapter<TrashSummaryAdapter.TrashSummaryViewHolder> {

    private final List<TrashTypeItem> trashItems = new ArrayList<>();

    public TrashSummaryAdapter(Map<String, Integer> trashTypeCounts) {
        // Convert map to list of items
        for (Map.Entry<String, Integer> entry : trashTypeCounts.entrySet()) {
            trashItems.add(new TrashTypeItem(entry.getKey(), entry.getValue()));
        }
    }

    @NonNull
    @Override
    public TrashSummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrashSummaryBinding binding = ItemTrashSummaryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TrashSummaryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashSummaryViewHolder holder, int position) {
        TrashTypeItem item = trashItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return trashItems.size();
    }

    static class TrashSummaryViewHolder extends RecyclerView.ViewHolder {
        private final ItemTrashSummaryBinding binding;

        public TrashSummaryViewHolder(ItemTrashSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TrashTypeItem item) {
            binding.tvTrashType.setText(item.type);
            binding.tvTrashCount.setText(String.valueOf(item.count));
        }
    }

    private static class TrashTypeItem {
        String type;
        int count;

        TrashTypeItem(String type, int count) {
            this.type = type;
            this.count = count;
        }
    }
}
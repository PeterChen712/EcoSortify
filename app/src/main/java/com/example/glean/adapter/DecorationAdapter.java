package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.databinding.ItemDecorationBinding;
import com.example.glean.model.Decoration;

import java.util.List;

public class DecorationAdapter extends RecyclerView.Adapter<DecorationAdapter.DecorationViewHolder> {

    private final List<Decoration> decorations;
    private final OnDecorationClickListener listener;

    public interface OnDecorationClickListener {
        void onDecorationClick(Decoration decoration);
    }

    public DecorationAdapter(List<Decoration> decorations, OnDecorationClickListener listener) {
        this.decorations = decorations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DecorationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDecorationBinding binding = ItemDecorationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DecorationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DecorationViewHolder holder, int position) {
        Decoration decoration = decorations.get(position);
        holder.bind(decoration, listener);
    }

    @Override
    public int getItemCount() {
        return decorations.size();
    }

    static class DecorationViewHolder extends RecyclerView.ViewHolder {
        private final ItemDecorationBinding binding;

        public DecorationViewHolder(ItemDecorationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Decoration decoration, OnDecorationClickListener listener) {
            binding.tvDecorationName.setText(decoration.getName());
            binding.tvDecorationDescription.setText(decoration.getDescription());
            binding.ivDecorationImage.setImageResource(decoration.getImageResourceId());
            
            if (decoration.isOwned()) {
                binding.tvDecorationPrice.setText("Owned");
                binding.btnAction.setText("Apply");
            } else {
                binding.tvDecorationPrice.setText(decoration.getPrice() + " points");
                binding.btnAction.setText("Purchase");
            }
            
            binding.btnAction.setOnClickListener(v -> listener.onDecorationClick(decoration));
            
            // Make entire item clickable
            binding.cardDecoration.setOnClickListener(v -> listener.onDecorationClick(decoration));
        }
    }
}
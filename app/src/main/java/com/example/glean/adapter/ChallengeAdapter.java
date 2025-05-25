package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.databinding.ItemChallengeBinding;
import com.example.glean.model.Challenge;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder> {

    private final List<Challenge> challenges;
    private final OnChallengeClickListener listener;

    public interface OnChallengeClickListener {
        void onChallengeClick(Challenge challenge);
    }

    public ChallengeAdapter(List<Challenge> challenges, OnChallengeClickListener listener) {
        this.challenges = challenges;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChallengeBinding binding = ItemChallengeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ChallengeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChallengeViewHolder holder, int position) {
        Challenge challenge = challenges.get(position);
        holder.bind(challenge, listener);
    }

    @Override
    public int getItemCount() {
        return challenges.size();
    }

    static class ChallengeViewHolder extends RecyclerView.ViewHolder {
        private final ItemChallengeBinding binding;

        public ChallengeViewHolder(ItemChallengeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Challenge challenge, OnChallengeClickListener listener) {
            binding.tvTitle.setText(challenge.getTitle());
            binding.tvDescription.setText(challenge.getDescription());
            
            // Format date range
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
            
            try {
                Date startDate = inputFormat.parse(challenge.getStartDate());
                Date endDate = inputFormat.parse(challenge.getEndDate());
                
                String formattedStart = outputFormat.format(startDate);
                String formattedEnd = outputFormat.format(endDate);
                
                binding.tvDateRange.setText(formattedStart + " - " + formattedEnd);
            } catch (ParseException e) {
                binding.tvDateRange.setText(challenge.getStartDate() + " - " + challenge.getEndDate());
            }
            
            // Set progress if active
            if (challenge.getProgress() > 0) {
                binding.progressBar.setVisibility(android.view.View.VISIBLE);
                binding.progressBar.setMax(100);
                int progressPercentage = challenge.getProgress();
                binding.progressBar.setProgress(progressPercentage);
                binding.tvProgress.setText(progressPercentage + "%");
            } else {
                binding.progressBar.setVisibility(android.view.View.GONE);
                binding.tvProgress.setText("Coming soon");
            }
            
            // Set points
            binding.tvPoints.setText(challenge.getPoints() + " pts");
            
            // Set click listener
            binding.cardChallenge.setOnClickListener(v -> listener.onChallengeClick(challenge));
        }
    }
}
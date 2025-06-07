package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.DonationEntity;

import java.util.List;

public class DonationAdapter extends RecyclerView.Adapter<DonationAdapter.DonationViewHolder> {
    
    private List<DonationEntity> donationList;
    private OnDonationClickListener listener;
      public interface OnDonationClickListener {
        void onDonationClick(DonationEntity donation);
        void onDonateClick(DonationEntity donation);
        void onLearnMoreClick(DonationEntity donation);
        void onJoinProgramClick(DonationEntity donation);
        void onShareClick(DonationEntity donation);
    }
    
    public DonationAdapter(List<DonationEntity> donationList, OnDonationClickListener listener) {
        this.donationList = donationList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public DonationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_donation, parent, false);
        return new DonationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DonationViewHolder holder, int position) {
        DonationEntity donation = donationList.get(position);
        holder.bind(donation);
    }
    
    @Override
    public int getItemCount() {
        return donationList.size();
    }
    
    public class DonationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvType, tvTitle, tvOrganizer, tvDescription, tvTargetAmount, tvCurrentAmount, tvDaysLeft, tvDonors;
        private ProgressBar progressBar;
        private Button btnLearnMore, btnDonate;
        
        public DonationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvDonationType);
            tvTitle = itemView.findViewById(R.id.tvDonationTitle);
            tvOrganizer = itemView.findViewById(R.id.tvOrganizer);
            tvDescription = itemView.findViewById(R.id.tvDonationDescription);
            tvTargetAmount = itemView.findViewById(R.id.tvTargetAmount);
            tvCurrentAmount = itemView.findViewById(R.id.tvCurrentAmount);
            tvDaysLeft = itemView.findViewById(R.id.tvDaysLeft);
            tvDonors = itemView.findViewById(R.id.tvDonors);
            progressBar = itemView.findViewById(R.id.progressBar);
            btnLearnMore = itemView.findViewById(R.id.btnLearnMore);
            btnDonate = itemView.findViewById(R.id.btnDonate);
        }
          public void bind(DonationEntity donation) {
            tvType.setText(donation.getType());
            tvTitle.setText(donation.getTitle());
            tvOrganizer.setText(donation.getOrganization());
            tvDescription.setText(donation.getDescription());
            tvTargetAmount.setText("Target: Rp " + String.format("%,d", donation.getTargetAmount()));
            tvCurrentAmount.setText("Terkumpul: Rp " + String.format("%,d", donation.getCurrentAmount()));
            
            // Convert timeLeft (in milliseconds) to days
            long daysLeft = donation.getTimeLeft() / (24 * 60 * 60 * 1000);
            tvDaysLeft.setText(daysLeft + " hari tersisa");
            tvDonors.setText(donation.getDonorCount() + " donatur");
            
            // Set progress
            int progress = (int) ((donation.getCurrentAmount() * 100) / donation.getTargetAmount());
            progressBar.setProgress(progress);
            
            // Set click listeners
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onDonationClick(donation));
                btnLearnMore.setOnClickListener(v -> listener.onLearnMoreClick(donation));
                btnDonate.setOnClickListener(v -> listener.onDonateClick(donation));
            }
        }
    }
}

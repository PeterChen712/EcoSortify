package com.example.glean.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.EventEntity;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    
    private List<EventEntity> eventList;
    private OnEventClickListener listener;
    
    public interface OnEventClickListener {
        void onEventClick(EventEntity event);
        void onJoinClick(EventEntity event);
        void onShareClick(EventEntity event);
    }
    
    public EventAdapter(List<EventEntity> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventEntity event = eventList.get(position);
        holder.bind(event);
    }
    
    @Override
    public int getItemCount() {
        return eventList.size();
    }
      public class EventViewHolder extends RecyclerView.ViewHolder {
        private TextView tvEventTitle, tvEventDescription, tvEventDate, tvLocation;
        private TextView tvParticipants, tvProgressText;
        private ProgressBar progressBar;
        private LinearLayout llProgress;
        private Button btnEventAction;
        
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvParticipants = itemView.findViewById(R.id.tvParticipants);
            tvProgressText = itemView.findViewById(R.id.tvProgressText);
            progressBar = itemView.findViewById(R.id.progressBar);
            llProgress = itemView.findViewById(R.id.llProgress);
            btnEventAction = itemView.findViewById(R.id.btnEventAction);
        }
          public void bind(EventEntity event) {
            tvEventTitle.setText(event.getTitle());
            tvEventDescription.setText(event.getDescription());
            
            // Format date from long to readable string
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
            String dateString = sdf.format(new java.util.Date(event.getDate()));
            tvEventDate.setText(dateString);
            
            tvLocation.setText(event.getLocation());
            tvParticipants.setText(event.getParticipants() + " peserta");
            
            // Show progress for challenges and contests
            if ("challenge".equals(event.getType()) || "contest".equals(event.getType())) {
                llProgress.setVisibility(View.VISIBLE);
                tvProgressText.setText(event.getProgress() + "%");
                progressBar.setProgress(event.getProgress());
            } else {
                llProgress.setVisibility(View.GONE);
            }
              // Set button text based on event type
            if ("event".equals(event.getType())) {
                btnEventAction.setText(itemView.getContext().getString(R.string.register_event));
            } else if ("challenge".equals(event.getType())) {
                btnEventAction.setText(itemView.getContext().getString(R.string.join_challenge));
            } else if ("contest".equals(event.getType())) {
                btnEventAction.setText(itemView.getContext().getString(R.string.join_contest));
            }
            
            // Set click listeners
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onEventClick(event));
                btnEventAction.setOnClickListener(v -> listener.onJoinClick(event));
            }
        }
    }
}

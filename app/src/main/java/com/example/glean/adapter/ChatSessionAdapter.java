package com.example.glean.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.model.ChatSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.SessionViewHolder> {
    private List<ChatSession> sessions;
    private Context context;
    private OnSessionClickListener listener;
    private SimpleDateFormat dateFormat;
    
    public interface OnSessionClickListener {
        void onSessionClick(ChatSession session);
        void onSessionDelete(ChatSession session);
        void onSessionRename(ChatSession session);
    }
    
    public ChatSessionAdapter(Context context, List<ChatSession> sessions) {
        this.context = context;
        this.sessions = sessions;
        this.dateFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
    }
    
    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_session, parent, false);
        return new SessionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSession session = sessions.get(position);
        
        holder.titleText.setText(session.getTitle());
        holder.timeText.setText(dateFormat.format(new Date(session.getLastMessageAt())));
        
        // Set background untuk session yang aktif
        if (session.isActive()) {
            holder.itemView.setBackgroundResource(R.drawable.bg_session_active);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_session_normal);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSessionClick(session);
            }
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSessionDelete(session);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onSessionRename(session);
            }
            return true;
        });
    }
    
    @Override
    public int getItemCount() {
        return sessions.size();
    }
    
    public void updateSessions(List<ChatSession> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }
    
    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView timeText;
        ImageButton deleteButton;
          SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.session_title);
            timeText = itemView.findViewById(R.id.session_time);
            deleteButton = itemView.findViewById(R.id.session_delete_button);
        }
    }
}

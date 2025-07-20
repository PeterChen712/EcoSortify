package com.example.glean.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model untuk pesan chat antara user dan AI
 */
public class ChatMessage {
    public static final int TYPE_USER = 1;
    public static final int TYPE_AI = 2;
    
    private String message;
    private boolean isFromUser;
    private long timestamp;
    private boolean isTypingIndicator = false;
    
    public ChatMessage(String message, boolean isFromUser, long timestamp) {
        this.message = message;
        this.isFromUser = isFromUser;
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
      public boolean isFromUser() {
        return isFromUser;
    }
    
    public boolean isUser() {
        return isFromUser;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public int getType() {
        return isFromUser ? TYPE_USER : TYPE_AI;
    }
    
    public boolean isTypingIndicator() {
        return isTypingIndicator;
    }
    
    public void setTypingIndicator(boolean typingIndicator) {
        isTypingIndicator = typingIndicator;
    }
}

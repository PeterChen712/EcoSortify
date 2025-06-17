package com.example.glean.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatSession {
    private String sessionId;
    private String title;
    private long createdAt;
    private long lastMessageAt;
    private List<ChatMessage> messages;
    private boolean isActive;
    
    public ChatSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.lastMessageAt = System.currentTimeMillis();
        this.messages = new ArrayList<>();
        this.isActive = false;
        this.title = "Chat Baru";
    }
    
    public ChatSession(String sessionId, String title) {
        this.sessionId = sessionId;
        this.title = title;
        this.createdAt = System.currentTimeMillis();
        this.lastMessageAt = System.currentTimeMillis();
        this.messages = new ArrayList<>();
        this.isActive = false;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getLastMessageAt() {
        return lastMessageAt;
    }
    
    public void setLastMessageAt(long lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.lastMessageAt = message.getTimestamp();
        
        // Auto-generate title from first user message
        if (this.title.equals("Chat Baru") && message.isUser() && !message.getMessage().trim().isEmpty()) {
            String firstMessage = message.getMessage().trim();
            if (firstMessage.length() > 30) {
                this.title = firstMessage.substring(0, 30) + "...";
            } else {
                this.title = firstMessage;
            }
        }
    }
    
    public String getPreviewText() {
        if (messages.isEmpty()) {
            return "Belum ada pesan";
        }
        
        ChatMessage lastMessage = messages.get(messages.size() - 1);
        String preview = lastMessage.getMessage();
        
        if (preview.length() > 50) {
            preview = preview.substring(0, 50) + "...";
        }
        
        return preview;
    }
    
    public int getMessageCount() {
        return messages.size();
    }
}
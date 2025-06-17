package com.example.glean.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.glean.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryManager {
    private static final String TAG = "ChatHistoryManager";
    private static final String PREFS_NAME = "chat_history_prefs";
    private static final String KEY_CHAT_HISTORY = "chat_history";
    private static final int MAX_HISTORY_SIZE = 100; // Limit history to prevent memory issues
    
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    
    public ChatHistoryManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * Save chat history to SharedPreferences
     */
    public void saveChatHistory(List<ChatMessage> messages) {
        try {
            // Filter out typing indicators and limit size
            List<ChatMessage> filteredMessages = new ArrayList<>();
            
            for (ChatMessage message : messages) {
                if (!message.isTypingIndicator()) {
                    filteredMessages.add(message);
                }
            }
            
            // Keep only the most recent messages to prevent storage bloat
            if (filteredMessages.size() > MAX_HISTORY_SIZE) {
                filteredMessages = filteredMessages.subList(
                    filteredMessages.size() - MAX_HISTORY_SIZE, 
                    filteredMessages.size()
                );
            }
            
            String json = gson.toJson(filteredMessages);
            sharedPreferences.edit()
                    .putString(KEY_CHAT_HISTORY, json)
                    .apply();
            
            Log.d(TAG, "Chat history saved. Messages count: " + filteredMessages.size());
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving chat history", e);
        }
    }
    
    /**
     * Load chat history from SharedPreferences
     */
    public List<ChatMessage> loadChatHistory() {
        try {
            String json = sharedPreferences.getString(KEY_CHAT_HISTORY, null);
            
            if (json != null && !json.isEmpty()) {
                Type listType = new TypeToken<List<ChatMessage>>(){}.getType();
                List<ChatMessage> messages = gson.fromJson(json, listType);
                
                if (messages != null) {
                    Log.d(TAG, "Chat history loaded. Messages count: " + messages.size());
                    return messages;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading chat history", e);
        }
        
        Log.d(TAG, "No chat history found or error occurred. Returning empty list.");
        return new ArrayList<>();
    }
    
    /**
     * Clear all chat history
     */
    public void clearChatHistory() {
        try {
            sharedPreferences.edit()
                    .remove(KEY_CHAT_HISTORY)
                    .apply();
            
            Log.d(TAG, "Chat history cleared");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing chat history", e);
        }
    }
    
    /**
     * Check if chat history exists
     */
    public boolean hasHistory() {
        String json = sharedPreferences.getString(KEY_CHAT_HISTORY, null);
        return json != null && !json.isEmpty();
    }
    
    /**
     * Get the size of stored chat history
     */
    public int getHistorySize() {
        List<ChatMessage> history = loadChatHistory();
        return history.size();
    }
}
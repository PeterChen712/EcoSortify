package com.example.glean.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.glean.model.ChatMessage;
import com.example.glean.model.ChatSession;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatSessionManager {
    private static final String TAG = "ChatSessionManager";
    private static final String PREFS_NAME = "chat_sessions_prefs";
    private static final String KEY_CHAT_SESSIONS = "chat_sessions";
    private static final String KEY_ACTIVE_SESSION = "active_session_id";
    private static final int MAX_SESSIONS = 50; // Limit total sessions
    
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    
    public ChatSessionManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * Create new chat session
     */
    public ChatSession createNewSession() {
        ChatSession newSession = new ChatSession();
        
        // Deactivate all other sessions
        List<ChatSession> sessions = loadAllSessions();
        for (ChatSession session : sessions) {
            session.setActive(false);
        }
        
        // Add new session and make it active
        newSession.setActive(true);
        sessions.add(0, newSession); // Add to beginning
        
        // Limit total sessions
        if (sessions.size() > MAX_SESSIONS) {
            sessions = sessions.subList(0, MAX_SESSIONS);
        }
        
        saveAllSessions(sessions);
        setActiveSession(newSession.getSessionId());
        
        Log.d(TAG, "New session created: " + newSession.getSessionId());
        return newSession;
    }
    
    /**
     * Load all chat sessions
     */
    public List<ChatSession> loadAllSessions() {
        try {
            String json = sharedPreferences.getString(KEY_CHAT_SESSIONS, null);
            
            if (json != null && !json.isEmpty()) {
                Type listType = new TypeToken<List<ChatSession>>(){}.getType();
                List<ChatSession> sessions = gson.fromJson(json, listType);
                
                if (sessions != null) {
                    // Sort by last message time (newest first)
                    Collections.sort(sessions, new Comparator<ChatSession>() {
                        @Override
                        public int compare(ChatSession s1, ChatSession s2) {
                            return Long.compare(s2.getLastMessageAt(), s1.getLastMessageAt());
                        }
                    });
                    
                    Log.d(TAG, "Loaded " + sessions.size() + " chat sessions");
                    return sessions;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading chat sessions", e);
        }
        
        Log.d(TAG, "No sessions found, creating default session");
        return new ArrayList<>();
    }
    
    /**
     * Save all chat sessions
     */
    public void saveAllSessions(List<ChatSession> sessions) {
        try {
            // Filter out empty sessions (except active one)
            List<ChatSession> filteredSessions = new ArrayList<>();
            
            for (ChatSession session : sessions) {
                if (session.isActive() || session.getMessageCount() > 0) {
                    // Remove typing indicators before saving
                    List<ChatMessage> filteredMessages = new ArrayList<>();
                    for (ChatMessage message : session.getMessages()) {
                        if (!message.isTypingIndicator()) {
                            filteredMessages.add(message);
                        }
                    }
                    session.setMessages(filteredMessages);
                    filteredSessions.add(session);
                }
            }
            
            String json = gson.toJson(filteredSessions);
            sharedPreferences.edit()
                    .putString(KEY_CHAT_SESSIONS, json)
                    .apply();
            
            Log.d(TAG, "Saved " + filteredSessions.size() + " chat sessions");
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving chat sessions", e);
        }
    }
    
    /**
     * Get specific session by ID
     */
    public ChatSession getSessionById(String sessionId) {
        List<ChatSession> sessions = loadAllSessions();
        
        for (ChatSession session : sessions) {
            if (session.getSessionId().equals(sessionId)) {
                return session;
            }
        }
        
        return null;
    }
    
    /**
     * Get current active session
     */
    public ChatSession getActiveSession() {
        String activeSessionId = getActiveSessionId();
        
        if (activeSessionId != null) {
            ChatSession session = getSessionById(activeSessionId);
            if (session != null) {
                return session;
            }
        }
        
        // No active session found, create new one
        return createNewSession();
    }
    
    /**
     * Set active session
     */
    public void setActiveSession(String sessionId) {
        List<ChatSession> sessions = loadAllSessions();
        
        // Deactivate all sessions
        for (ChatSession session : sessions) {
            session.setActive(session.getSessionId().equals(sessionId));
        }
        
        saveAllSessions(sessions);
        
        sharedPreferences.edit()
                .putString(KEY_ACTIVE_SESSION, sessionId)
                .apply();
        
        Log.d(TAG, "Active session set to: " + sessionId);
    }
    
    /**
     * Get active session ID
     */
    public String getActiveSessionId() {
        return sharedPreferences.getString(KEY_ACTIVE_SESSION, null);
    }
    
    /**
     * Update session with new message
     */
    public void updateSession(ChatSession session, ChatMessage message) {
        List<ChatSession> sessions = loadAllSessions();
        
        // Find and update the session
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getSessionId().equals(session.getSessionId())) {
                session.addMessage(message);
                sessions.set(i, session);
                break;
            }
        }
        
        saveAllSessions(sessions);
    }
    
    /**
     * Delete session
     */
    public void deleteSession(String sessionId) {
        List<ChatSession> sessions = loadAllSessions();
        
        sessions.removeIf(session -> session.getSessionId().equals(sessionId));
        
        saveAllSessions(sessions);
        
        // If deleted session was active, set another as active
        String activeSessionId = getActiveSessionId();
        if (sessionId.equals(activeSessionId)) {
            if (!sessions.isEmpty()) {
                setActiveSession(sessions.get(0).getSessionId());
            } else {
                createNewSession();
            }
        }
        
        Log.d(TAG, "Session deleted: " + sessionId);
    }
    
    /**
     * Clear all sessions
     */
    public void clearAllSessions() {
        try {
            sharedPreferences.edit()
                    .remove(KEY_CHAT_SESSIONS)
                    .remove(KEY_ACTIVE_SESSION)
                    .apply();
            
            Log.d(TAG, "All chat sessions cleared");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing chat sessions", e);
        }
    }
    
    /**
     * Rename session
     */
    public void renameSession(String sessionId, String newTitle) {
        List<ChatSession> sessions = loadAllSessions();
        
        for (ChatSession session : sessions) {
            if (session.getSessionId().equals(sessionId)) {
                session.setTitle(newTitle);
                break;
            }
        }
        
        saveAllSessions(sessions);
        Log.d(TAG, "Session renamed: " + sessionId + " -> " + newTitle);
    }
}
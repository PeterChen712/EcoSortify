package com.example.glean.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.adapter.ChatAdapter;
import com.example.glean.adapter.ChatSessionAdapter;

import java.util.ArrayList;
import com.example.glean.api.GeminiApi;
import com.example.glean.model.ChatMessage;
import com.example.glean.model.ChatSession;
import com.example.glean.util.ChatSessionManager;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class AiChatFragment extends Fragment implements ChatSessionAdapter.OnSessionClickListener {
    private static final String TAG = "AiChatFragment";
    
    // UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar chatToolbar;
    private TextView chatTitle;
    private RecyclerView chatRecyclerView;
    private RecyclerView sessionsRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton menuButton;
    private ImageButton newChatButton;
    private ImageButton clearAllSessionsButton;
    
    // Adapters
    private ChatAdapter chatAdapter;
    private ChatSessionAdapter sessionAdapter;
    
    // Data & Managers
    private ChatSession currentSession;
    private GeminiApi geminiApi;
    private ChatSessionManager sessionManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Remove setHasOptionsMenu(true); - we don't need menu anymore
        return inflater.inflate(R.layout.fragment_ai_chat, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupToolbar();
        setupRecyclerViews();
        setupClickListeners();
        
        // Initialize managers
        geminiApi = new GeminiApi();
        sessionManager = new ChatSessionManager(requireContext());
        
        // Load or create session
        loadCurrentSession();
        loadChatSessions();
    }
    
    private void initViews(View view) {
        // Main layout components
        drawerLayout = view.findViewById(R.id.drawer_layout);
        navigationView = view.findViewById(R.id.navigation_view);
        chatToolbar = view.findViewById(R.id.chat_toolbar);
        chatTitle = view.findViewById(R.id.chat_title);
        
        // Chat components
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        messageEditText = view.findViewById(R.id.message_edit_text);
        sendButton = view.findViewById(R.id.send_button);
        
        // Toolbar buttons
        menuButton = view.findViewById(R.id.menu_button);
        newChatButton = view.findViewById(R.id.new_chat_button);
        
        // Sidebar components
        sessionsRecyclerView = view.findViewById(R.id.sessions_recycler_view);
        clearAllSessionsButton = view.findViewById(R.id.clear_all_sessions_button);
    }
    
    private void setupToolbar() {
        if (getActivity() != null) {
            ((androidx.appcompat.app.AppCompatActivity) getActivity()).setSupportActionBar(chatToolbar);
        }
    }
    
    private void setupRecyclerViews() {
        // Setup chat messages recycler view
        chatAdapter = new ChatAdapter(requireContext());
        LinearLayoutManager chatLayoutManager = new LinearLayoutManager(requireContext());
        chatLayoutManager.setStackFromEnd(true);
        
        chatRecyclerView.setLayoutManager(chatLayoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
          // Setup sessions recycler view
        sessionAdapter = new ChatSessionAdapter(requireContext(), new ArrayList<>());
        sessionAdapter.setOnSessionClickListener(this);
        
        LinearLayoutManager sessionsLayoutManager = new LinearLayoutManager(requireContext());
        sessionsRecyclerView.setLayoutManager(sessionsLayoutManager);
        sessionsRecyclerView.setAdapter(sessionAdapter);
    }
    
    private void setupClickListeners() {
        // Send message
        sendButton.setOnClickListener(v -> sendMessage());
        
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
        
        // Menu button to open drawer
        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        
        // New chat button
        newChatButton.setOnClickListener(v -> createNewChat());
        
        // Clear all sessions - This is now the main way to clear history
        clearAllSessionsButton.setOnClickListener(v -> showClearAllSessionsDialog());
    }
    
    /**
     * Load current active session or create new one
     */
    private void loadCurrentSession() {
        currentSession = sessionManager.getActiveSession();
        
        if (currentSession == null) {
            createNewChat();
        } else {
            loadSessionMessages();
            updateChatTitle();
        }
    }
    
    /**
     * Load messages for current session
     */
    private void loadSessionMessages() {
        if (currentSession == null) return;
        
        chatAdapter.clear();
        
        for (ChatMessage message : currentSession.getMessages()) {
            if (!message.isTypingIndicator()) {
                chatAdapter.addMessage(message);
            }
        }
        
        // Add welcome message if session is empty
        if (currentSession.getMessages().isEmpty()) {
            addWelcomeMessage();
        }
        
        scrollToBottom();
    }
    
    /**
     * Load all chat sessions for sidebar
     */
    private void loadChatSessions() {
        List<ChatSession> sessions = sessionManager.loadAllSessions();
        sessionAdapter.updateSessions(sessions);
    }
    
    /**
     * Create new chat session
     */
    private void createNewChat() {
        // Save current session first
        if (currentSession != null) {
            saveCurrentSession();
        }
        
        // Create new session
        currentSession = sessionManager.createNewSession();
        
        // Clear chat UI and load new session
        chatAdapter.clear();
        addWelcomeMessage();
        updateChatTitle();
        
        // Refresh sessions list
        loadChatSessions();
        
        // Close drawer if open
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        
        Toast.makeText(requireContext(), "Chat baru telah dibuat", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Update chat title based on current session
     */
    private void updateChatTitle() {
        if (currentSession != null) {
            chatTitle.setText(currentSession.getTitle());
        } else {
            chatTitle.setText("EcoBot Chat");
        }
    }
    
    /**
     * Save current session
     */
    private void saveCurrentSession() {
        if (currentSession != null) {
            // Get messages from adapter (excluding typing indicators)
            List<ChatMessage> currentMessages = new ArrayList<>();
            
            // Get messages from the session object instead of adapter
            for (ChatMessage message : currentSession.getMessages()) {
                if (!message.isTypingIndicator()) {
                    currentMessages.add(message);
                }
            }
            
            currentSession.setMessages(currentMessages);
            
            // Save session
            List<ChatSession> sessions = sessionManager.loadAllSessions();
            
            // Update existing session or add new one
            boolean found = false;
            for (int i = 0; i < sessions.size(); i++) {
                if (sessions.get(i).getSessionId().equals(currentSession.getSessionId())) {
                    sessions.set(i, currentSession);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                sessions.add(0, currentSession);
            }
            
            sessionManager.saveAllSessions(sessions);
        }
    }
    
    private void addWelcomeMessage() {
        String welcomeText = "🌱 Halo! Saya EcoBot, asisten AI untuk pertanyaan lingkungan.\n\n" +
                "Anda bisa bertanya tentang:\n" +
                "• Cara memilah sampah\n" +
                "• Tips ramah lingkungan\n" +
                "• Daur ulang dan komposting\n" +
                "• Solusi lingkungan lainnya\n\n" +
                "Silakan ketik pertanyaan Anda! 💚";
        
        ChatMessage welcomeMessage = new ChatMessage(welcomeText, false, System.currentTimeMillis());
        chatAdapter.addMessage(welcomeMessage);
        
        if (currentSession != null) {
            currentSession.addMessage(welcomeMessage);
            saveCurrentSession();
        }
        
        scrollToBottom();
    }
    
    private void sendMessage() {
        String message = messageEditText.getText().toString().trim();
        
        if (message.isEmpty()) {
            Toast.makeText(requireContext(), "Silakan ketik pesan terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Clear input
        messageEditText.setText("");
        
        // Add user message
        ChatMessage userMessage = new ChatMessage(message, true, System.currentTimeMillis());
        chatAdapter.addMessage(userMessage);
        
        if (currentSession != null) {
            currentSession.addMessage(userMessage);
            updateChatTitle(); // Update title if this is first message
        }
        
        scrollToBottom();
        
        // Show typing indicator
        showTypingIndicator();
        
        // Get conversation history for context
        List<ChatMessage> conversationHistory = getConversationHistory();
        
        // Send to Gemini API with history
        geminiApi.sendMessageWithHistory(message, conversationHistory, new GeminiApi.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hideTypingIndicator();
                        
                        // Add AI response
                        ChatMessage aiMessage = new ChatMessage(response, false, System.currentTimeMillis());
                        chatAdapter.addMessage(aiMessage);
                        
                        if (currentSession != null) {
                            currentSession.addMessage(aiMessage);
                            saveCurrentSession();
                        }
                        
                        scrollToBottom();
                        
                        // Refresh sessions list to update preview
                        loadChatSessions();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Gemini API error: " + error);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hideTypingIndicator();
                        
                        String errorMessage = "😕 Maaf, terjadi kesalahan. Silakan coba lagi.\n\n" +
                                "Error: " + error;
                        
                        ChatMessage errorMsg = new ChatMessage(errorMessage, false, System.currentTimeMillis());
                        chatAdapter.addMessage(errorMsg);
                        
                        if (currentSession != null) {
                            currentSession.addMessage(errorMsg);
                            saveCurrentSession();
                        }
                        
                        scrollToBottom();
                    });
                }
            }
        });
    }
    
    /**
     * Get conversation history for current session
     */
    private List<ChatMessage> getConversationHistory() {
        if (currentSession == null) {
            return new ArrayList<>();
        }
        
        List<ChatMessage> history = new ArrayList<>();
        List<ChatMessage> messages = currentSession.getMessages();
        
        // Limit history to last 20 messages to avoid token limits
        int startIndex = Math.max(0, messages.size() - 20);
        
        for (int i = startIndex; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (!message.isTypingIndicator()) {
                history.add(message);
            }
        }
        
        return history;
    }
    
    // ChatSessionAdapter.OnSessionClickListener implementation
    @Override
    public void onSessionClick(ChatSession session) {
        // Save current session
        if (currentSession != null) {
            saveCurrentSession();
        }
        
        // Switch to selected session
        sessionManager.setActiveSession(session.getSessionId());
        currentSession = session;
        
        // Load session messages
        loadSessionMessages();
        updateChatTitle();
        
        // Refresh sessions list to update active state
        loadChatSessions();
        
        // Close drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        
        Toast.makeText(requireContext(), "Switched to: " + session.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onSessionDelete(ChatSession session) {
        showDeleteSessionDialog(session);
    }
    
    @Override
    public void onSessionRename(ChatSession session) {
        showRenameSessionDialog(session);
    }
    
    /**
     * Show delete session confirmation dialog
     */
    private void showDeleteSessionDialog(ChatSession session) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hapus Chat")
                .setMessage("Apakah Anda yakin ingin menghapus chat \"" + session.getTitle() + "\"?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    sessionManager.deleteSession(session.getSessionId());
                    
                    // If deleted session was current, create new session
                    if (currentSession != null && currentSession.getSessionId().equals(session.getSessionId())) {
                        createNewChat();
                    }
                    
                    loadChatSessions();
                    Toast.makeText(requireContext(), "Chat telah dihapus", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
    
    /**
     * Show rename session dialog
     */
    private void showRenameSessionDialog(ChatSession session) {
        EditText editText = new EditText(requireContext());
        editText.setText(session.getTitle());
        editText.selectAll();
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Ubah Nama Chat")
                .setView(editText)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newTitle = editText.getText().toString().trim();
                    if (!newTitle.isEmpty()) {
                        sessionManager.renameSession(session.getSessionId(), newTitle);
                        
                        // Update current session title if it's the same session
                        if (currentSession != null && currentSession.getSessionId().equals(session.getSessionId())) {
                            currentSession.setTitle(newTitle);
                            updateChatTitle();
                        }
                        
                        loadChatSessions();
                        Toast.makeText(requireContext(), "Nama chat telah diubah", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }
    
    /**
     * Show clear all sessions dialog
     */
    private void showClearAllSessionsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hapus Semua Riwayat")
                .setMessage("Apakah Anda yakin ingin menghapus semua riwayat chat?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    sessionManager.clearAllSessions();
                    createNewChat();
                    loadChatSessions();
                    Toast.makeText(requireContext(), "Semua riwayat chat telah dihapus", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
    
    private void showTypingIndicator() {
        ChatMessage typingMessage = new ChatMessage("🤖 Sedang mengetik...", false, System.currentTimeMillis());
        typingMessage.setTypingIndicator(true);
        chatAdapter.addMessage(typingMessage);
        scrollToBottom();
    }
    
    private void hideTypingIndicator() {
        chatAdapter.removeTypingIndicator();
    }
    
    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }
    
    // Remove all menu-related methods since we don't need them anymore
    // onCreateOptionsMenu, onOptionsItemSelected are removed
    
    @Override
    public void onPause() {
        super.onPause();
        saveCurrentSession();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveCurrentSession();
    }
}
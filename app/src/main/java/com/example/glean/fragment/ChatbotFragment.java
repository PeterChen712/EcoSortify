package com.example.glean.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.glean.R;
import com.example.glean.adapter.ChatAdapter;
import com.example.glean.databinding.FragmentChatbotBinding;
import com.example.glean.helper.NetworkHelper;
import com.example.glean.helper.NetworkNotificationHelper;
import com.example.glean.helper.NetworkConnectionListener;
import com.example.glean.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment untuk EcoBot yang membantu menjawab pertanyaan seputar sampah, plogging, dan edukasi lingkungan
 * WAJIB ONLINE - Fitur ini hanya dapat diakses jika perangkat dalam keadaan online
 */
public class ChatbotFragment extends Fragment {    private FragmentChatbotBinding binding;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatHistory;
    private SharedPreferences chatPrefs;
    private NetworkNotificationHelper networkNotification;
    private NetworkConnectionListener networkListener;
    private static final String PREF_CHAT_HISTORY = "chat_history";
    private static final String KEY_CHAT_MESSAGES = "chat_messages";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatbotBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeChatHistory();
        initializeNetworkNotification();
        setupChatbot();
        checkNetworkAndShowUI();
        startNetworkMonitoring();
    }private void initializeChatHistory() {
        chatPrefs = requireContext().getSharedPreferences(PREF_CHAT_HISTORY, Context.MODE_PRIVATE);
        chatHistory = loadChatHistory();
          // Setup RecyclerView for chat
        chatAdapter = new ChatAdapter(requireContext());
        binding.recyclerViewChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewChat.setAdapter(chatAdapter);
    }
      private void checkNetworkAndShowUI() {
        NetworkHelper.NetworkStatus networkStatus = NetworkHelper.getNetworkStatus(requireContext());
        
        if (!networkStatus.isAvailable()) {
            networkNotification.showOfflineNotification();
            showOfflineMode();
        } else {
            networkNotification.hideNotification();
            showOnlineMode();
        }
    }
    
    private void showOfflineMode() {
        binding.chatInputLayout.setVisibility(View.GONE);
        
        // Show offline example in chat if no history
        if (chatHistory.isEmpty()) {
            ChatMessage offlineMsg = new ChatMessage(
                "🤖 EcoBot saat ini offline. EcoBot memerlukan koneksi internet untuk dapat menjawab pertanyaan Anda.",
                false,
                System.currentTimeMillis()
            );
            chatHistory.add(offlineMsg);
            chatAdapter.notifyItemInserted(chatHistory.size() - 1);
        }
    }
      private void showOnlineMode() {
        binding.chatInputLayout.setVisibility(View.VISIBLE);
        
        // Add welcome message if no history
        if (chatHistory.isEmpty()) {
            ChatMessage welcomeMsg = new ChatMessage(
                "🤖 Halo! Saya EcoBot, asisten pintar untuk pertanyaan seputar sampah, plogging, dan pelestarian lingkungan. Ada yang bisa saya bantu?",
                false,
                System.currentTimeMillis()
            );
            chatHistory.add(welcomeMsg);
            chatAdapter.notifyItemInserted(chatHistory.size() - 1);
            saveChatHistory();
        }
    }

    private void setupChatbot() {
        binding.tvChatbotDescription.setText("Tanya apa saja tentang sampah, plogging, daur ulang, dan pelestarian lingkungan!");
        
        // Setup example questions
        setupExampleQuestions();
        
        // Setup send button
        binding.btnSendMessage.setOnClickListener(v -> {
            String message = binding.etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            }
        });
    }
    
    private void sendMessage(String message) {
        // Check network before sending
        NetworkHelper.NetworkStatus networkStatus = NetworkHelper.getNetworkStatus(requireContext());
          if (!networkStatus.isAvailable()) {
            Toast.makeText(requireContext(), "🌐 Tidak ada koneksi internet. EcoBot memerlukan koneksi online.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Add user message to chat
        ChatMessage userMessage = new ChatMessage(message, true, System.currentTimeMillis());
        chatHistory.add(userMessage);
        chatAdapter.notifyItemInserted(chatHistory.size() - 1);
        binding.recyclerViewChat.scrollToPosition(chatHistory.size() - 1);
        
        // Clear input
        binding.etMessage.setText("");
          // Show typing indicator
        binding.btnSendMessage.setEnabled(false);
        binding.btnSendMessage.setText("💭");
        
        // Simulate EcoBot response (TODO: Connect to real backend/Gemini)
        simulateEcoBotResponse(message);
    }
      private void simulateEcoBotResponse(String userMessage) {
        // Simulate processing time
        binding.recyclerViewChat.postDelayed(() -> {
            String ecoBotResponse = generateMockResponse(userMessage);
            
            ChatMessage ecoBotMessage = new ChatMessage(ecoBotResponse, false, System.currentTimeMillis());
            chatHistory.add(ecoBotMessage);
            chatAdapter.notifyItemInserted(chatHistory.size() - 1);
            binding.recyclerViewChat.scrollToPosition(chatHistory.size() - 1);
            
            // Save updated chat history
            saveChatHistory();
            
            // Reset send button
            binding.btnSendMessage.setEnabled(true);
            binding.btnSendMessage.setText("📤");
        }, 1500);
    }
    
    private String generateMockResponse(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("sampah") || lowerMessage.contains("trash")) {
            return "♻️ Tentang sampah: Sampah dapat diklasifikasikan menjadi organik (mudah terurai) dan anorganik (sulit terurai). Pemilahan yang benar sangat penting untuk daur ulang yang efektif!";
        } else if (lowerMessage.contains("plogging")) {
            return "🏃‍♂️ Plogging adalah kombinasi jogging dan mengambil sampah! Aktivitas ini bermanfaat untuk kesehatan tubuh sekaligus membersihkan lingkungan. Mulai dengan area kecil di sekitar rumah Anda.";
        } else if (lowerMessage.contains("plastik")) {
            return "🥤 Sampah plastik membutuhkan 100-1000 tahun untuk terurai! Gunakan botol minum yang dapat digunakan ulang, hindari sedotan plastik, dan pilih produk dengan kemasan minimal.";
        } else if (lowerMessage.contains("daur ulang") || lowerMessage.contains("recycle")) {
            return "♻️ Daur ulang mengurangi limbah dan menghemat sumber daya! Pastikan sampah sudah bersih dan dipilah dengan benar: kertas, plastik, logam, dan kaca terpisah.";
        } else if (lowerMessage.contains("halo") || lowerMessage.contains("hai")) {
            return "👋 Halo! Saya siap membantu menjawab pertanyaan tentang sampah, plogging, daur ulang, dan pelestarian lingkungan. Ada yang ingin Anda tanyakan?";
        } else {
            return "🤖 Terima kasih atas pertanyaannya! Saya akan terus belajar untuk memberikan jawaban yang lebih baik. Coba tanyakan tentang sampah, plogging, atau daur ulang ya!";
        }
    }
    
    private List<ChatMessage> loadChatHistory() {
        String json = chatPrefs.getString(KEY_CHAT_MESSAGES, null);
        if (json != null) {
            Type type = new TypeToken<List<ChatMessage>>(){}.getType();
            return new Gson().fromJson(json, type);
        }
        return new ArrayList<>();
    }
    
    private void saveChatHistory() {
        String json = new Gson().toJson(chatHistory);
        chatPrefs.edit().putString(KEY_CHAT_MESSAGES, json).apply();
    }

    private void setupExampleQuestions() {
        binding.btnExample1.setOnClickListener(v -> {
            binding.etMessage.setText("Bagaimana cara memilah sampah yang benar?");
        });
        
        binding.btnExample2.setOnClickListener(v -> {
            binding.etMessage.setText("Apa manfaat plogging untuk lingkungan?");
        });
        
        binding.btnExample3.setOnClickListener(v -> {
            binding.etMessage.setText("Berapa lama sampah plastik terurai di alam?");
        });
    }

    private void initializeNetworkNotification() {
        networkNotification = NetworkNotificationHelper.create(requireContext(), binding.notificationContainer);
    }
    
    private void startNetworkMonitoring() {
        networkListener = new NetworkConnectionListener(requireContext());
        networkListener.setOnNetworkChangeListener(new NetworkConnectionListener.OnNetworkChangeListener() {
            @Override
            public void onNetworkAvailable() {
                // Run on UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        networkNotification.hideNotification();
                        showOnlineMode();
                    });
                }
            }
            
            @Override
            public void onNetworkLost() {
                // Run on UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        networkNotification.showOfflineNotification();
                        showOfflineMode();
                    });
                }
            }
        });
        networkListener.startListening();
    }    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up network monitoring
        if (networkListener != null) {
            networkListener.stopListening();
            networkListener = null;
        }
        
        // Clean up notification
        if (networkNotification != null) {
            networkNotification.destroy();
            networkNotification = null;
        }
        
        binding = null;
    }
}

package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glean.R;
import com.example.glean.databinding.FragmentChatbotBinding;

/**
 * Fragment untuk Chatbot AI yang membantu menjawab pertanyaan seputar sampah, plogging, dan edukasi lingkungan
 */
public class ChatbotFragment extends Fragment {

    private FragmentChatbotBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatbotBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupChatbot();
    }    private void setupChatbot() {
        binding.tvChatbotDescription.setText("Tanya apa saja tentang sampah, plogging, daur ulang, dan pelestarian lingkungan!");
        
        // Setup example questions
        setupExampleQuestions();
        
        // Setup send button
        binding.btnSendMessage.setOnClickListener(v -> {
            String message = binding.etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                // TODO: Implementasi chatbot AI
                Toast.makeText(requireContext(), "Chatbot AI segera hadir! 🤖", Toast.LENGTH_SHORT).show();
                binding.etMessage.setText("");
            }
        });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

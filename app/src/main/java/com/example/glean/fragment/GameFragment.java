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
import com.example.glean.databinding.FragmentGameBinding;

/**
 * Fragment untuk menampilkan game edukasi bertema sampah dan lingkungan
 */
public class GameFragment extends Fragment {

    private FragmentGameBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupGameMenu();
    }    private void setupGameMenu() {
        // Setup header title
        binding.tvGameTitle.setText("Game Edukasi");
        
        // Setup info button
        binding.btnGameInfo.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Info: Game edukasi untuk belajar tentang lingkungan! 🎮", Toast.LENGTH_LONG).show();
        });
        
        // Setup description
        binding.tvGameDescription.setText("Game edukasi interaktif untuk belajar tentang sampah, daur ulang, dan pelestarian lingkungan.\n\nSegera hadir!");
        
        // Setup button placeholder
        binding.btnComingSoon.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Game segera hadir! 🎮", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

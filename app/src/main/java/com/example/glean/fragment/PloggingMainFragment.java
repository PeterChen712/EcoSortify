package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glean.databinding.FragmentPloggingMainBinding;

/**
 * Fragment untuk konten utama plogging
 * Sementara ini akan menjadi wrapper untuk PloggingFragment yang sudah ada
 */
public class PloggingMainFragment extends Fragment {

    private FragmentPloggingMainBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPloggingMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Load existing plogging fragment content
        loadPloggingContent();
    }    private void loadPloggingContent() {
        // Untuk saat ini, fragment ini akan menampilkan pesan placeholder
        // Nantinya konten dari PloggingFragment yang asli akan dipindahkan ke sini
        binding.tvPloggingDescription.setText("Fitur plogging utama untuk merekam aktivitas dan mengumpulkan data sampah.");
        
        binding.btnStartPlogging.setOnClickListener(v -> {
            // TODO: Implementasi start plogging
            // Untuk sementara tampilkan pesan
            android.widget.Toast.makeText(requireContext(), "Plogging akan segera dimulai!", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

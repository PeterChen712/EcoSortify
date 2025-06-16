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
import com.example.glean.databinding.FragmentKlasifikasiBinding;

/**
 * Fragment untuk edukasi klasifikasi sampah
 */
public class KlasifikasiFragment extends Fragment {

    private FragmentKlasifikasiBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentKlasifikasiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupKlasifikasi();
    }    private void setupKlasifikasi() {
        binding.tvKlasifikasiDescription.setText("Pelajari cara mengklasifikasikan jenis-jenis sampah dengan benar untuk daur ulang yang optimal.");
        
        // Setup category buttons
        setupCategoryButtons();
        
        // Setup scan button
        binding.btnScanTrash.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Fitur scan sampah segera hadir! 📷", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupCategoryButtons() {
        binding.btnCategoryOrganic.setOnClickListener(v -> {
            showCategoryInfo("Organik", "Sisa makanan, daun, kulit buah, dan bahan alami yang mudah terurai.");
        });
        
        binding.btnCategoryAnorganic.setOnClickListener(v -> {
            showCategoryInfo("Anorganik", "Plastik, kertas, logam, kaca, dan bahan yang sulit terurai.");
        });
        
        binding.btnCategoryB3.setOnClickListener(v -> {
            showCategoryInfo("B3 (Berbahaya)", "Baterai, lampu neon, obat-obatan, dan bahan kimia berbahaya.");
        });
        
        binding.btnCategoryElektronik.setOnClickListener(v -> {
            showCategoryInfo("Elektronik", "HP, komputer, TV, dan perangkat elektronik lainnya.");
        });
    }

    private void showCategoryInfo(String category, String info) {
        binding.tvCategoryInfo.setText(category + ":\n" + info);
        binding.categoryInfoLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glean.R;

/**
 * Fragment untuk fitur Donasi
 * Sementara menampilkan "Coming Soon"
 */
public class DonationFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup UI for coming soon message
        TextView comingSoonText = view.findViewById(R.id.tv_coming_soon);
        if (comingSoonText != null) {
            comingSoonText.setText("🚀 Fitur Donasi\n\nComing Soon!\n\nFitur ini akan memungkinkan Anda untuk berdonasi kepada organisasi-organisasi lingkungan yang berkontribusi untuk kebersihan dan kelestarian lingkungan.");
        }
    }
}

package com.example.glean.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.glean.fragment.community.NewsFragment;
import com.example.glean.fragment.ChatbotFragment;
import com.example.glean.fragment.KlasifikasiFragment;

/**
 * Adapter untuk mengelola tabs di menu Eksplorasi
 * Berisi: News, Chatbot AI, dan Klasifikasi Sampah
 */
public class EksplorasiSectionAdapter extends FragmentStateAdapter {

    public EksplorasiSectionAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new NewsFragment();
            case 1:
                return new ChatbotFragment();
            case 2:
                return new KlasifikasiFragment();
            default:
                return new NewsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // News, Chatbot AI, Klasifikasi
    }
}

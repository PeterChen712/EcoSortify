package com.example.glean.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.glean.fragment.community.NewsFragment;
import com.example.glean.fragment.DonationFragment;
import com.example.glean.fragment.ClassifyFragment;

/**
 * Adapter untuk mengelola tabs di menu Eksplorasi
 * Berisi: News, Donasi, dan Klasifikasi Sampah
 */
public class EksplorasiSectionAdapter extends FragmentStateAdapter {

    public EksplorasiSectionAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {        switch (position) {
            case 0:
                return new NewsFragment();
            case 1:
                return new DonationFragment();
            case 2:
                return new ClassifyFragment();
            default:
                return new NewsFragment();
        }
    }    @Override
    public int getItemCount() {
        return 3; // News, Donasi, Klasifikasi
    }
}

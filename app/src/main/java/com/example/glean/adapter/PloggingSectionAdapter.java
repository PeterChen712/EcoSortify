package com.example.glean.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.glean.fragment.PloggingMainFragment;
import com.example.glean.fragment.StatsFragment;
import com.example.glean.fragment.community.RankingFragment;

/**
 * Adapter untuk mengelola tabs di dalam menu Plogging
 * Berisi: Plogging utama, Stats, dan Ranking
 */
public class PloggingSectionAdapter extends FragmentStateAdapter {

    public PloggingSectionAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PloggingMainFragment();
            case 1:
                return new StatsFragment();
            case 2:
                return new RankingFragment();
            default:
                return new PloggingMainFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Plogging, Stats, Ranking
    }
}

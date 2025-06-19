package com.example.glean.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.glean.fragment.RankingFragment;

/**
 * Community Section Adapter for main app sections
 * Contains: Ranking only
 * Removed news features per app requirements
 */
public class CommunitySectionAdapter extends FragmentStateAdapter {

    public CommunitySectionAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Only Ranking fragment now
        return new RankingFragment();
    }

    @Override
    public int getItemCount() {
        return 1; // Only Ranking
    }
}

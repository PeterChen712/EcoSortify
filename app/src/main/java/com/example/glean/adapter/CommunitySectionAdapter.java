package com.example.glean.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.glean.fragment.NewsFragment;
import com.example.glean.fragment.RankingFragment;

/**
 * Community Section Adapter for main app sections
 * Contains: News, Ranking
 * No social posting features - just informational content
 */
public class CommunitySectionAdapter extends FragmentStateAdapter {

    public CommunitySectionAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new NewsFragment();
            case 1:
                return new RankingFragment();
            default:
                return new NewsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // News, Ranking
    }
}

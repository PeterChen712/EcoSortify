package com.example.glean.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.glean.fragment.community.NewsFragment;
// import com.example.glean.fragment.community.RankingFragment; // Temporarily disabled

/**
 * Simplified Community Section Adapter without social posting features
 * Contains only: News, Ranking
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
                // return new RankingFragment(); // Temporarily disabled
                return new NewsFragment(); // Fallback to news for now
            default:
                return new NewsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // News, Ranking
    }
}

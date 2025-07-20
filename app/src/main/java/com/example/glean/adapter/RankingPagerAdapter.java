package com.example.glean.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.glean.fragment.RankingTabFragment;

public class RankingPagerAdapter extends FragmentStateAdapter {
    
    public RankingPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return RankingTabFragment.newInstance(true); // Points ranking
            case 1:
                return RankingTabFragment.newInstance(false); // Distance ranking
            default:
                return RankingTabFragment.newInstance(true);
        }
    }
    
    @Override
    public int getItemCount() {
        return 2; // Points and Distance tabs
    }
}

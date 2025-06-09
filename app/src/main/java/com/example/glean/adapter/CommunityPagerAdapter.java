package com.example.glean.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.glean.fragment.community.NewsFragment;
import com.example.glean.fragment.community.RankingFragment;
import com.example.glean.fragment.community.SosialFragment;

public class CommunityPagerAdapter extends FragmentStateAdapter {

    public CommunityPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new SosialFragment();
            case 1:
                return new RankingFragment();
            case 2:
                return new NewsFragment();
            default:
                return new SosialFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }    public String getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Sosial";
            case 1:
                return "Ranking";
            case 2:
                return "News";
            default:
                return "Sosial";
        }
    }
}

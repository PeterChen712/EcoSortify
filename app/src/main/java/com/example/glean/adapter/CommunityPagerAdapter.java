package com.example.glean.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.glean.R;
import com.example.glean.fragment.community.NewsFragment;
import com.example.glean.fragment.community.RankingFragment;
import com.example.glean.fragment.community.SosialFragment;

public class CommunityPagerAdapter extends FragmentStateAdapter {

    private Context context;

    public CommunityPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, Context context) {
        super(fragmentManager, lifecycle);
        this.context = context;
    }@NonNull
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
                return context.getString(R.string.community_social_tab);
            case 1:
                return context.getString(R.string.community_ranking_tab);
            case 2:
                return context.getString(R.string.community_news_tab);
            default:
                return context.getString(R.string.community_social_tab);
        }
    }
}

package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.glean.R;
import com.example.glean.databinding.FragmentCommunityBinding;
import com.example.glean.fragment.community.SosialFragment;
import com.example.glean.fragment.community.RankingFragment;
import com.example.glean.fragment.community.NewsFragment;
import com.example.glean.fragment.community.EventFragment;
import com.example.glean.fragment.community.DonasiFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment {
    
    private FragmentCommunityBinding binding;
    private CommunityPagerAdapter pagerAdapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupTabs();
    }
    
    private void setupTabs() {
        pagerAdapter = new CommunityPagerAdapter(requireActivity());
        binding.viewPager.setAdapter(pagerAdapter);
        
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Sosial");
                            tab.setIcon(R.drawable.ic_social);
                            break;
                        case 1:
                            tab.setText("Ranking");
                            tab.setIcon(R.drawable.ic_ranking);
                            break;
                        case 2:
                            tab.setText("News");
                            tab.setIcon(R.drawable.ic_news);
                            break;
                        case 3:
                            tab.setText("Event");
                            tab.setIcon(R.drawable.ic_event);
                            break;
                        case 4:
                            tab.setText("Donasi");
                            tab.setIcon(R.drawable.ic_donation);
                            break;
                    }
                }).attach();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    private static class CommunityPagerAdapter extends FragmentStateAdapter {
        
        public CommunityPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new SosialFragment();
                case 1:
                    return new RankingFragment();
                case 2:
                    return new NewsFragment();
                case 3:
                    return new EventFragment();
                case 4:
                    return new DonasiFragment();
                default:
                    return new SosialFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 5;
        }
    }
}

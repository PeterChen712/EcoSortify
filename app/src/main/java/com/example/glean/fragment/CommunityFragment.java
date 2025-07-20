package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.glean.R;
import com.example.glean.adapter.CommunitySectionAdapter;
import com.example.glean.databinding.FragmentCommunityBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Simplified Community Fragment without news features
 * Contains only: Ranking
 */
public class CommunityFragment extends Fragment {

    private FragmentCommunityBinding binding;
    private CommunitySectionAdapter sectionAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewPager();
    }

    private void setupViewPager() {
        sectionAdapter = new CommunitySectionAdapter(this);
        binding.viewPager.setAdapter(sectionAdapter);        // Setup tabs
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    // Only Ranking tab now
                    tab.setText("Ranking");
                    tab.setIcon(R.drawable.ic_ranking);
                }).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

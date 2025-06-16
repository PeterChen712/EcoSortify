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
import com.example.glean.adapter.PloggingSectionAdapter;
import com.example.glean.databinding.FragmentPloggingTabsBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Fragment wrapper untuk menu Plogging dengan tabs
 * Berisi: Plogging utama, Stats, dan Ranking
 */
public class PloggingTabsFragment extends Fragment {

    private FragmentPloggingTabsBinding binding;
    private PloggingSectionAdapter sectionAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPloggingTabsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewPager();
    }    private void setupViewPager() {
        sectionAdapter = new PloggingSectionAdapter(this);
        binding.viewPager.setAdapter(sectionAdapter);

        // Setup tabs
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Plogging");
                            tab.setIcon(R.drawable.ic_plogging);
                            break;
                        case 1:
                            tab.setText("Statistik");
                            tab.setIcon(R.drawable.ic_stats);
                            break;
                        case 2:
                            tab.setText("Ranking");
                            tab.setIcon(R.drawable.ic_ranking);
                            break;
                    }
                }).attach();

        // Update header title when tab changes
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateHeaderTitle(position);
            }
        });

        // Set initial header title
        updateHeaderTitle(0);
    }    private void updateHeaderTitle(int position) {
        switch (position) {
            case 0:
                binding.tvPloggingHeader.setText("Plogging");
                break;
            case 1:
                binding.tvPloggingHeader.setText("Statistik");
                break;
            case 2:
                binding.tvPloggingHeader.setText("Ranking");
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

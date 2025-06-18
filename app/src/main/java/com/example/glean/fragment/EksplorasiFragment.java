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
import com.example.glean.adapter.EksplorasiSectionAdapter;
import com.example.glean.databinding.FragmentEksplorasiBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Fragment Eksplorasi yang berisi: News, Donasi, dan Klasifikasi Sampah
 */
public class EksplorasiFragment extends Fragment {

    private FragmentEksplorasiBinding binding;
    private EksplorasiSectionAdapter sectionAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEksplorasiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewPager();
    }    private void setupViewPager() {
        sectionAdapter = new EksplorasiSectionAdapter(this);
        binding.viewPager.setAdapter(sectionAdapter);        // Setup tabs
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Berita");
                            tab.setIcon(R.drawable.ic_news);
                            break;                        case 1:
                            tab.setText("Donasi");
                            tab.setIcon(R.drawable.ic_donation);
                            break;
                        case 2:
                            tab.setText("Sampah");
                            tab.setIcon(R.drawable.ic_classification);
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
                binding.tvEksplorasiHeader.setText("Berita");
                break;            case 1:
                binding.tvEksplorasiHeader.setText("Donasi");
                break;
            case 2:
                binding.tvEksplorasiHeader.setText("Klasifikasi");
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

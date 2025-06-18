package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.adapter.RankingPagerAdapter;
import com.example.glean.databinding.FragmentRankingBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class RankingFragment extends Fragment {
    
    private FragmentRankingBinding binding;
    private RankingPagerAdapter pagerAdapter;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRankingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupBackButton();
        setupTabsAndViewPager();
    }
    
    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                Navigation.findNavController(v).popBackStack();
            }
        });
    }
    
    private void setupTabsAndViewPager() {
        // Setup ViewPager2 with adapter
        pagerAdapter = new RankingPagerAdapter(requireActivity());
        binding.viewPager.setAdapter(pagerAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("🏆 Points");
                            break;
                        case 1:
                            tab.setText("🏃 Distance");
                            break;
                    }
                }).attach();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

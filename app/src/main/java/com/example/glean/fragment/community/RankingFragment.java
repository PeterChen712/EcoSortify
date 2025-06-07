package com.example.glean.fragment.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.glean.R;
import com.example.glean.adapter.RankingAdapter;
import com.example.glean.databinding.FragmentRankingBinding;
import com.example.glean.model.RankingEntity;

import java.util.ArrayList;
import java.util.List;

public class RankingFragment extends Fragment {
    
    private FragmentRankingBinding binding;
    private RankingAdapter rankingAdapter;
    private List<RankingEntity> rankings = new ArrayList<>();
    private String currentFilter = "weekly";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRankingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupSpinner();
        setupRecyclerView();
        loadRankings();
    }
    
    private void setupSpinner() {
        String[] filterOptions = {"Minggu Ini", "Bulan Ini", "Semua Waktu"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_spinner_item, filterOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(adapter);
        
        binding.spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        currentFilter = "weekly";
                        break;
                    case 1:
                        currentFilter = "monthly";
                        break;
                    case 2:
                        currentFilter = "all_time";
                        break;
                }
                loadRankings();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    private void setupRecyclerView() {
        rankingAdapter = new RankingAdapter(rankings);
        binding.recyclerViewRanking.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewRanking.setAdapter(rankingAdapter);
    }
    
    private void loadRankings() {
        rankings.clear();
        
        // Sample ranking data - replace with actual API call
        RankingEntity rank1 = new RankingEntity();
        rank1.setPosition(1);
        rank1.setUsername("EcoChampion");
        rank1.setAvatar("avatar_1");
        rank1.setPoints(2150);
        rank1.setTrashCollected(45.2f);
        rank1.setDistance(28.5f);
        rank1.setBadges(12);
        rankings.add(rank1);
        
        RankingEntity rank2 = new RankingEntity();
        rank2.setPosition(2);
        rank2.setUsername("GreenWarrior");
        rank2.setAvatar("avatar_2");
        rank2.setPoints(1980);
        rank2.setTrashCollected(38.7f);
        rank2.setDistance(25.3f);
        rank2.setBadges(10);
        rankings.add(rank2);
        
        RankingEntity rank3 = new RankingEntity();
        rank3.setPosition(3);
        rank3.setUsername("CleanHero");
        rank3.setAvatar("avatar_3");
        rank3.setPoints(1750);
        rank3.setTrashCollected(32.1f);
        rank3.setDistance(22.8f);
        rank3.setBadges(8);
        rankings.add(rank3);
        
        RankingEntity rank4 = new RankingEntity();
        rank4.setPosition(4);
        rank4.setUsername("EcoFriend");
        rank4.setAvatar("avatar_4");
        rank4.setPoints(1420);
        rank4.setTrashCollected(28.9f);
        rank4.setDistance(19.2f);
        rank4.setBadges(6);
        rankings.add(rank4);
        
        RankingEntity rank5 = new RankingEntity();
        rank5.setPosition(5);
        rank5.setUsername("GreenTeam");
        rank5.setAvatar("avatar_5");
        rank5.setPoints(1280);
        rank5.setTrashCollected(25.6f);
        rank5.setDistance(17.4f);
        rank5.setBadges(5);
        rankings.add(rank5);
        
        // Add more sample data
        for (int i = 6; i <= 20; i++) {
            RankingEntity rank = new RankingEntity();
            rank.setPosition(i);
            rank.setUsername("User" + i);
            rank.setAvatar("avatar_default");
            rank.setPoints(1200 - (i * 50));
            rank.setTrashCollected(25.0f - (i * 1.2f));
            rank.setDistance(15.0f - (i * 0.8f));
            rank.setBadges(Math.max(1, 8 - i/3));
            rankings.add(rank);
        }
        
        rankingAdapter.notifyDataSetChanged();
        updateMyRanking();
    }
    
    private void updateMyRanking() {
        // Show current user's ranking
        binding.myRankingCard.setVisibility(View.VISIBLE);
        binding.tvMyPosition.setText("#15");
        binding.tvMyUsername.setText("You");
        binding.tvMyPoints.setText("850 poin");
        binding.tvMyStats.setText("18.5kg • 12.3km • 3 badge");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

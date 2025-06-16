package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glean.R;
import com.example.glean.databinding.FragmentRankingBinding;

import java.util.ArrayList;
import java.util.List;

public class RankingFragment extends Fragment {
    
    private FragmentRankingBinding binding;
    
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
        setupRankingList();
    }
    
    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                Navigation.findNavController(v).popBackStack();
            }
        });
    }
    
    private void setupRankingList() {
        // Setup RecyclerView for ranking list
        binding.recyclerViewRanking.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // TODO: Implement ranking adapter and load actual ranking data
        // For now, this is a placeholder structure
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

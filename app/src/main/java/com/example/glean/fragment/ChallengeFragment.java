package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.glean.R;
import com.example.glean.adapter.ChallengeAdapter;
import com.example.glean.databinding.FragmentChallengeBinding;
import com.example.glean.model.Challenge;

import java.util.ArrayList;
import java.util.List;

public class ChallengeFragment extends Fragment implements ChallengeAdapter.OnChallengeClickListener {

    private FragmentChallengeBinding binding;
    private List<Challenge> activeChallenges = new ArrayList<>();
    private List<Challenge> upcomingChallenges = new ArrayList<>();
    private ChallengeAdapter activeAdapter;
    private ChallengeAdapter upcomingAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChallengeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup RecyclerViews
        binding.rvActiveChallenges.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUpcomingChallenges.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Initialize adapters
        activeAdapter = new ChallengeAdapter(activeChallenges, this);
        upcomingAdapter = new ChallengeAdapter(upcomingChallenges, this);
        
        binding.rvActiveChallenges.setAdapter(activeAdapter);
        binding.rvUpcomingChallenges.setAdapter(upcomingAdapter);
        
        // Set click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        
        // Load challenges
        loadChallenges();
    }
    
    private void loadChallenges() {
        // In a real app, this would load from a server or database
        
        // Mock active challenges
        activeChallenges.add(new Challenge(
                1,
                "Weekend Warrior",
                "Collect at least 10 pieces of trash this weekend",
                "2023-05-26",
                "2023-05-28",
                10,
                50
        ));
        
        activeChallenges.add(new Challenge(
                2,
                "5K Plogging Run",
                "Complete a 5km plogging run",
                "2023-05-20",
                "2023-05-30",
                5,
                100
        ));
        
        // Mock upcoming challenges
        upcomingChallenges.add(new Challenge(
                3,
                "Community Cleanup",
                "Join the community cleanup event",
                "2023-06-05",
                "2023-06-05",
                0,
                200
        ));
        
        upcomingChallenges.add(new Challenge(
                4,
                "Plastic-Free Week",
                "Collect 20 plastic items in one week",
                "2023-06-10",
                "2023-06-17",
                0,
                150
        ));
        
        // Notify adapters
        activeAdapter.notifyDataSetChanged();
        upcomingAdapter.notifyDataSetChanged();
    }
    
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onChallengeClick(Challenge challenge) {
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putInt("CHALLENGE_ID", challenge.getId());
        navController.navigate(R.id.action_challengeFragment_to_challengeDetailFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}
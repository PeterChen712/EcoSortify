package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.databinding.FragmentChallengeDetailBinding;
import com.example.glean.model.Challenge;
import com.example.glean.helper.NotificationHelper; // Add this import

public class ChallengeDetailFragment extends Fragment {

    private FragmentChallengeDetailBinding binding;
    private int challengeId = -1;
    private Challenge challenge;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            challengeId = getArguments().getInt("CHALLENGE_ID", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChallengeDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnJoinChallenge.setOnClickListener(v -> joinChallenge());
        
        // Load challenge data
        loadChallengeData();
    }
    
    private void loadChallengeData() {
        // In a real app, this would load from a server or database
        if (challengeId == 1) {
            challenge = new Challenge(
                    1,
                    "Weekend Warrior",
                    "Collect at least 10 pieces of trash this weekend",
                    "2023-05-26",
                    "2023-05-28",
                    10,
                    50
            );
        } else if (challengeId == 2) {
            challenge = new Challenge(
                    2,
                    "5K Plogging Run",
                    "Complete a 5km plogging run",
                    "2023-05-20",
                    "2023-05-30",
                    5,
                    100
            );
        } else if (challengeId == 3) {
            challenge = new Challenge(
                    3,
                    "Community Cleanup",
                    "Join the community cleanup event",
                    "2023-06-05",
                    "2023-06-05",
                    0,
                    200
            );
        } else if (challengeId == 4) {
            challenge = new Challenge(
                    4,
                    "Plastic-Free Week",
                    "Collect 20 plastic items in one week",
                    "2023-06-10",
                    "2023-06-17",
                    0,
                    150
            );
        }
        
        if (challenge != null) {
            updateUI();
        }
    }
    
    private void updateUI() {
        binding.tvTitle.setText(challenge.getTitle());
        binding.tvDescription.setText(challenge.getDescription());
        binding.tvDateRange.setText(challenge.getStartDate() + " - " + challenge.getEndDate());
        binding.tvPoints.setText(challenge.getPoints() + " points");
        
        if (challenge.getProgress() > 0) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.progressBar.setMax(100);
            binding.progressBar.setProgress(challenge.getProgress());
            binding.tvProgress.setText(challenge.getProgress() + "%");
            binding.tvStatus.setText("In Progress");
            binding.btnJoinChallenge.setText("View Progress");
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvProgress.setVisibility(View.GONE);
            binding.tvStatus.setText("Not Started");
            binding.btnJoinChallenge.setText("Join Challenge");
        }
    }
    
    private void joinChallenge() {
        if (challenge != null) {
            if (challenge.getProgress() > 0) {
                // Already joined, show progress
                Toast.makeText(requireContext(), "You're already participating in this challenge!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Joined challenge: " + challenge.getTitle(), Toast.LENGTH_SHORT).show();
                // In a real app, you would update the database
                challenge.setProgress(1); // Start progress
                updateUI();
                
                // Show notification
                NotificationHelper.showChallengeNotification(
                    requireContext(),
                    "Challenge Joined",
                    "You've joined the " + challenge.getTitle() + " challenge!"
                );
            }
        }
    }
    
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}
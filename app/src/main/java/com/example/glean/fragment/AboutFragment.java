package com.example.glean.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnVisitWebsite.setOnClickListener(v -> openWebsite());
        binding.btnContactUs.setOnClickListener(v -> contactUs());
        binding.btnPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());
        binding.btnTermsOfService.setOnClickListener(v -> openTermsOfService());
        
        // Set app version
        String versionName = "1.0"; // In a real app, get this dynamically
        binding.tvAppVersion.setText("Version " + versionName);
    }
    
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }
    
    private void openWebsite() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/glean"));
        startActivity(intent);
    }
    
    private void contactUs() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:support@glean.example.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about GleanGo App");
        startActivity(intent);
    }
    
    private void openPrivacyPolicy() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/glean/privacy"));
        startActivity(intent);
    }
    
    private void openTermsOfService() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/glean/terms"));
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}
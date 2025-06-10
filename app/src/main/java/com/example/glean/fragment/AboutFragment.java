package com.example.glean.fragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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

import com.example.glean.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnVisitWebsite.setOnClickListener(v -> openWebsite());
        binding.btnContactUs.setOnClickListener(v -> contactUs());
        binding.btnPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());
        binding.btnTermsOfService.setOnClickListener(v -> openTermsOfService());
        binding.tvWikipediaLink.setOnClickListener(v -> openWikipediaLink());
        
        // Set app version dynamically
        setAppVersion();
    }
    
    private void setAppVersion() {
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            binding.tvAppVersion.setText("Version " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            binding.tvAppVersion.setText("Version 1.0"); // Fallback
        }
    }
      private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }
    
    private void openWikipediaLink() {
        String wikipediaUrl = "https://en.wikipedia.org/wiki/The_Gleaners";
        openUrl(wikipediaUrl, "Tidak dapat membuka link Wikipedia");
    }
    
    private void openWebsite() {
        String websiteUrl = "https://example.com/glean";
        openUrl(websiteUrl, "Tidak dapat membuka website");
    }
    
    private void openPrivacyPolicy() {
        String privacyUrl = "https://example.com/glean/privacy";
        openUrl(privacyUrl, "Tidak dapat membuka halaman kebijakan privasi");
    }
    
    private void openTermsOfService() {
        String termsUrl = "https://example.com/glean/terms";
        openUrl(termsUrl, "Tidak dapat membuka halaman syarat & ketentuan");
    }
    
    /**
     * Generic method to open URLs with proper error handling
     */
    private void openUrl(String url, String errorMessage) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            
            // List of common browser packages to try
            String[] browserPackages = {
                "com.android.chrome",
                "com.android.browser",
                "org.mozilla.firefox",
                "com.opera.browser",
                "com.sec.android.app.sbrowser",
                "com.microsoft.emmx"
            };
            
            boolean browserFound = false;
            
            // Try specific browsers first
            for (String packageName : browserPackages) {
                Intent browserIntent = new Intent(intent);
                browserIntent.setPackage(packageName);
                
                if (browserIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(browserIntent);
                    browserFound = true;
                    break;
                }
            }
            
            // If no specific browser found, try generic intent
            if (!browserFound) {
                intent.setPackage(null);
                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), errorMessage + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
      private void contactUs() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@gleango.app"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about GleanGo App");
            intent.putExtra(Intent.EXTRA_TEXT, "Halo Tim GleanGo,\n\nSaya ingin bertanya tentang...\n\nTerima kasih.");
            
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "Tidak ada aplikasi email yang tersedia", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Tidak dapat membuka aplikasi email", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}
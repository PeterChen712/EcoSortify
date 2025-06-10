package com.example.glean.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;


import com.example.glean.activity.MainActivity;
import com.example.glean.R;
import com.example.glean.databinding.FragmentLoginBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AppDatabase db;
    private ExecutorService executor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set click listeners
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvRegister.setOnClickListener(v -> navigateToRegister());
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();        // Basic validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.login_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        // Authenticate user
        executor.execute(() -> {
            UserEntity user = db.userDao().getUserByEmailSync(email);            requireActivity().runOnUiThread(() -> {
                if (user != null && user.getPassword().equals(password)) {
                    // Save user ID to ALL SharedPreferences locations used by different fragments
                    int userId = user.getId();
                    
                    // Save to default preferences (used by HomeFragment, StatsFragment, etc.)
                    SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                    defaultPrefs.edit().putInt("USER_ID", userId).apply();
                    
                    // Save to USER_PREFS (used by ProfileFragment, ProfileDecorFragment)
                    SharedPreferences userPrefs = requireActivity().getSharedPreferences("USER_PREFS", 0);
                    userPrefs.edit().putInt("USER_ID", userId).apply();
                    
                    // Save to user_prefs with current_user_id key (used by CommunityFeedFragment, CreatePostFragment, etc.)
                    SharedPreferences communityPrefs = requireContext().getSharedPreferences("user_prefs", requireContext().MODE_PRIVATE);
                    communityPrefs.edit().putInt("current_user_id", userId).apply();

                    // Navigate to main activity
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);                } else {
                    Toast.makeText(requireContext(), getString(R.string.login_invalid_credentials), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void navigateToRegister() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_loginFragment_to_registerFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
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
import com.example.glean.auth.FirebaseAuthManager;
import com.example.glean.databinding.FragmentLoginBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.UserEntity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginFragment extends Fragment {

    private static final int RC_SIGN_IN = 9001;
    private FragmentLoginBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private FirebaseAuthManager authManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        authManager = FirebaseAuthManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if already logged in
        if (authManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        // Hide Google Sign-In if not available
        if (!authManager.isGoogleSignInAvailable()) {
            binding.btnGoogleSignIn.setVisibility(View.GONE);
            binding.divider.setVisibility(View.GONE);
            binding.tvOr.setVisibility(View.GONE);
        }

        // Set click listeners
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.tvRegister.setOnClickListener(v -> navigateToRegister());
        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Show loading
        binding.btnLogin.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        // Firebase authentication
        authManager.loginWithEmail(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (isAdded() && getActivity() != null) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnLogin.setEnabled(true);
                    
                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded() && getActivity() != null) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnLogin.setEnabled(true);
                    
                    Toast.makeText(requireContext(), "Login failed: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    private void signInWithGoogle() {
        Intent signInIntent = authManager.getGoogleSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            authManager.handleGoogleSignInResult(task, new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    if (isAdded() && getActivity() != null) {
                        Toast.makeText(requireContext(), "Google Sign-In successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (isAdded() && getActivity() != null) {
                        Toast.makeText(requireContext(), "Google Sign-In failed: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
    
    private void showForgotPasswordDialog() {
        String email = binding.etEmail.getText().toString().trim();
        
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        authManager.sendPasswordResetEmail(email, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to send reset email: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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
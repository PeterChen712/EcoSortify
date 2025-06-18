package com.example.glean.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.activity.MainActivity;
import com.example.glean.auth.FirebaseAuthManager;
import com.example.glean.databinding.FragmentRegisterBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.UserEntity;
import com.example.glean.util.PasswordValidator;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
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
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set click listeners
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvLogin.setOnClickListener(v -> navigateBack());
        
        // Add real-time password validation
        setupPasswordValidation();
    }
    
    private void setupPasswordValidation() {
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                if (!password.isEmpty()) {
                    PasswordValidator.ValidationResult result = PasswordValidator.validatePassword(password);
                    if (!result.isValid()) {
                        binding.tilPassword.setError(result.getErrorMessage());
                    } else {
                        binding.tilPassword.setError(null);
                        binding.tilPassword.setHelperText("✓ Password valid");
                    }
                } else {
                    binding.tilPassword.setError(null);
                    binding.tilPassword.setHelperText("Minimal 8 karakter, huruf besar, huruf kecil, dan angka");
                }
            }
        });
    }    private void attemptRegister() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        String name = binding.etName.getText().toString().trim();

        // Basic validation
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || name.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password strength
        PasswordValidator.ValidationResult passwordValidation = PasswordValidator.validatePasswordWithDetailedMessage(password);
        if (!passwordValidation.isValid()) {
            Toast.makeText(requireContext(), passwordValidation.getErrorMessage(), Toast.LENGTH_LONG).show();
            return;
        }        // Show loading
        binding.btnRegister.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        // Firebase registration
        authManager.registerWithEmail(email, password, name, new FirebaseAuthManager.AuthCallback() {            @Override
            public void onSuccess(FirebaseUser user) {
                if (isAdded() && getActivity() != null) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRegister.setEnabled(true);
                    
                    // Firebase mode - save to Firestore
                    com.google.firebase.firestore.FirebaseFirestore mStore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                    String userID = user.getUid();
                    com.google.firebase.firestore.DocumentReference documentReference = mStore.collection("users").document(userID);
                    java.util.Map<String, Object> userMap = new java.util.HashMap<>();
                    userMap.put("nama", name);
                    userMap.put("email", email);
                    userMap.put("totalPoints", 0);
                    userMap.put("totalKm", 0.0);
                    userMap.put("photoURL", "https://via.placeholder.com/150");
                    documentReference.set(userMap)
                        .addOnSuccessListener(aVoid -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.btnRegister.setEnabled(true);
                            Toast.makeText(requireContext(), "Registration successful! Welcome to EcoSortify!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        })
                        .addOnFailureListener(e -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.btnRegister.setEnabled(true);
                            Toast.makeText(requireContext(), "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded() && getActivity() != null) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRegister.setEnabled(true);
                    Toast.makeText(requireContext(), "Registration failed: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
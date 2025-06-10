package com.example.glean.fragment;

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
import com.example.glean.databinding.FragmentRegisterBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.UserEntity;
import com.example.glean.util.PasswordValidator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
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
    }

    private void attemptRegister() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        String name = binding.etName.getText().toString().trim();        // Basic validation
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || name.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.register_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }        if (!password.equals(confirmPassword)) {
            Toast.makeText(requireContext(), getString(R.string.register_passwords_no_match), Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password strength
        PasswordValidator.ValidationResult passwordValidation = PasswordValidator.validatePasswordWithDetailedMessage(password);
        if (!passwordValidation.isValid()) {
            Toast.makeText(requireContext(), passwordValidation.getErrorMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // Check if email is already registered
        executor.execute(() -> {
            UserEntity existingUser = db.userDao().getUserByEmailSync(email);            requireActivity().runOnUiThread(() -> {
                if (existingUser != null) {
                    Toast.makeText(requireContext(), getString(R.string.register_email_already_exists), Toast.LENGTH_SHORT).show();
                } else {
                    registerUser(email, password, name);
                }
            });
        });
    }

    private void registerUser(String email, String password, String name) {
        // Create new user with available constructor
        UserEntity user = new UserEntity(email, password);
        
        // Set the name by splitting into first and last name
        String[] nameParts = name.split(" ", 2);
        user.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            user.setLastName(nameParts[1]);
        } else {
            user.setLastName("");
        }

        // Insert in background thread
        executor.execute(() -> {
            long userId = db.userDao().insert(user);            requireActivity().runOnUiThread(() -> {
                if (userId > 0) {
                    Toast.makeText(requireContext(), getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                    
                    // Navigate to login
                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigate(R.id.action_registerFragment_to_loginFragment);
                } else {
                    Toast.makeText(requireContext(), getString(R.string.register_failed), Toast.LENGTH_SHORT).show();
                }
            });
        });
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
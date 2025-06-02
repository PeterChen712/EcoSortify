package com.example.glean.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.databinding.FragmentTrashDetailBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.TrashEntity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrashDetailFragment extends Fragment {

    private FragmentTrashDetailBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private TrashEntity currentTrash;
    private long trashId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        if (getArguments() != null) {
            trashId = getArguments().getInt("TRASH_ID", -1); // Changed from getLong to getInt
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTrashDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnSave.setOnClickListener(v -> saveTrashDetails());
        binding.btnDelete.setOnClickListener(v -> deleteTrash());
        
        // Setup spinner
        setupTrashTypeSpinner();
        
        // Load trash data
        loadTrashDetails();
    }
    
    private void setupTrashTypeSpinner() {
        String[] trashTypes = {
            "Unknown", "Plastic Bottle", "Plastic Bag", "Paper", "Metal Can", 
            "Glass", "Food Waste", "Cigarette Butt", "Other"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(), 
            android.R.layout.simple_spinner_item, 
            trashTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spTrashType.setAdapter(adapter);
    }
    
    private void loadTrashDetails() {
        if (trashId != -1) {
            db.trashDao().getTrashById((int) trashId).observe(getViewLifecycleOwner(), trash -> {
                if (trash != null) {
                    currentTrash = trash;
                    updateUI();
                }
            });
        }
    }
    
    private void updateUI() {
        if (currentTrash == null) return;
        
        // Set trash type
        String trashType = currentTrash.getTrashType();
        if (trashType != null && !trashType.isEmpty()) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.spTrashType.getAdapter();
            int position = adapter.getPosition(trashType);
            if (position >= 0) {
                binding.spTrashType.setSelection(position);
            }
        }
        
        // Set description
        String description = currentTrash.getDescription();
        if (description != null) {
            binding.etDescription.setText(description);
        }
        
        // Set timestamp
        String timeStr = formatTimestamp(currentTrash.getTimestamp());
        binding.tvTimestamp.setText("Collected: " + timeStr);
        
        // Set location
        if (currentTrash.getLatitude() != 0 && currentTrash.getLongitude() != 0) {
            String location = String.format(Locale.getDefault(), 
                "Location: %.6f, %.6f", currentTrash.getLatitude(), currentTrash.getLongitude());
            binding.tvLocation.setText(location);
            binding.tvLocation.setVisibility(View.VISIBLE);
        } else {
            binding.tvLocation.setVisibility(View.GONE);
        }
        
        // Load image using Glide
        String imagePath = currentTrash.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Glide.with(this)
                        .load(imageFile)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error)
                        .centerCrop()
                        .into(binding.ivTrashPhoto);
            } else {
                // Set default placeholder
                Glide.with(this)
                        .load(R.drawable.ic_trash_placeholder)
                        .placeholder(R.drawable.ic_placeholder)
                        .into(binding.ivTrashPhoto);
            }
        } else {
            // Set default placeholder when no image
            Glide.with(this)
                    .load(R.drawable.ic_trash_placeholder)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(binding.ivTrashPhoto);
        }
        
        // Set ML prediction info
        if (currentTrash.getMlLabel() != null && !currentTrash.getMlLabel().isEmpty()) {
            String mlInfo = String.format(Locale.getDefault(),
                "ML Prediction: %s (%.1f%% confidence)", 
                currentTrash.getMlLabel(), currentTrash.getConfidence() * 100);
            binding.tvMlPrediction.setText(mlInfo);
            binding.tvMlPrediction.setVisibility(View.VISIBLE);
        } else {
            binding.tvMlPrediction.setVisibility(View.GONE);
        }
    }
    
    private void saveTrashDetails() {
        if (currentTrash == null) return;
        
        String selectedType = binding.spTrashType.getSelectedItem().toString();
        String description = binding.etDescription.getText().toString().trim();
        
        currentTrash.setTrashType(selectedType);
        currentTrash.setDescription(description);
        
        executor.execute(() -> {
            db.trashDao().update(currentTrash);
            
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Trash details updated", Toast.LENGTH_SHORT).show();
                navigateBack();
            });
        });
    }
    
    private void deleteTrash() {
        if (currentTrash == null) return;
        
        executor.execute(() -> {
            db.trashDao().delete(currentTrash);
            
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Trash item deleted", Toast.LENGTH_SHORT).show();
                navigateBack();
            });
        });
    }
    
    private String formatTimestamp(long timestamp) {
        try {
            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            return "Unknown time";
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
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
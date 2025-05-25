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

import com.example.glean.R;
import com.example.glean.databinding.FragmentAddTrashBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.TrashEntity;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTrashFragment extends Fragment {

    private FragmentAddTrashBinding binding;
    
    private final String[] trashTypes = {"Plastic", "Paper", "Glass", "Metal", "Organic", "Electronic", "Hazardous", "Other"};
    private long trashId = -1;
    private TrashEntity trashEntity;
    private AppDatabase db;
    private ExecutorService executor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        if (getArguments() != null) {
            trashId = getArguments().getLong("TRASH_ID", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddTrashBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                trashTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spTrashType.setAdapter(adapter);
        
        // Load trash data
        if (trashId != -1) {
            loadTrashData();
        }
        
        // Set click listeners
        binding.btnSave.setOnClickListener(v -> saveTrashType());
        binding.btnCancel.setOnClickListener(v -> navigateBack());
    }
    
    private void loadTrashData() {
        executor.execute(() -> {
            // Use synchronous method instead of LiveData
            trashEntity = db.trashDao().getTrashByIdSync((int) trashId);
            
            if (trashEntity != null) {
                requireActivity().runOnUiThread(() -> {
                    // Load image
                    if (trashEntity.getPhotoPath() != null && !trashEntity.getPhotoPath().isEmpty()) {
                        File imageFile = new File(trashEntity.getPhotoPath());
                        if (imageFile.exists()) {
                            Picasso.get()
                                    .load(imageFile)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .error(android.R.drawable.ic_menu_gallery)
                                    .into(binding.ivTrashPhoto);
                        } else {
                            binding.ivTrashPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
                        }
                    } else {
                        binding.ivTrashPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                    
                    // Set ML suggestion
                    String mlLabel = trashEntity.getMlLabel();
                    float confidence = trashEntity.getConfidence();
                    if (mlLabel != null && !mlLabel.isEmpty()) {
                        binding.tvMlSuggestion.setText(String.format("ML Suggestion: %s (%.1f%% confidence)", 
                                mlLabel, confidence * 100));
                        
                        // Set spinner selection based on ML suggestion
                        for (int i = 0; i < trashTypes.length; i++) {
                            if (trashTypes[i].equalsIgnoreCase(mlLabel)) {
                                binding.spTrashType.setSelection(i);
                                break;
                            }
                        }
                    } else {
                        binding.tvMlSuggestion.setText("No ML suggestion available");
                    }
                    
                    // Set current type if already classified
                    if (trashEntity.getType() != null && !trashEntity.getType().isEmpty()) {
                        for (int i = 0; i < trashTypes.length; i++) {
                            if (trashTypes[i].equalsIgnoreCase(trashEntity.getType())) {
                                binding.spTrashType.setSelection(i);
                                break;
                            }
                        }
                    }
                });
            }
        });
    }
    
    private void saveTrashType() {
        String selectedType = binding.spTrashType.getSelectedItem().toString();
        
        if (trashEntity != null) {
            trashEntity.setType(selectedType);
            
            executor.execute(() -> {
                db.trashDao().update(trashEntity);
                
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Trash classification updated", Toast.LENGTH_SHORT).show();
                    navigateBack();
                });
            });
        }
    }
    
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
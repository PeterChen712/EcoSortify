package com.example.glean.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.databinding.FragmentTrashMlBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.helper.MLHelper;
import com.example.glean.helper.PermissionHelper;
import com.example.glean.model.TrashEntity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrashMLFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_CAMERA_PERMISSION = 1002;
    
    private FragmentTrashMlBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private MLHelper mlHelper;
    private FusedLocationProviderClient fusedLocationClient;
    
    private String currentPhotoPath;
    private Bitmap capturedImage;
    private int recordId = -1;
    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private String predictedLabel = "";
    private float confidenceScore = 0f;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        
        if (getArguments() != null) {
            recordId = getArguments().getInt("RECORD_ID", -1);
        }
        
        // Initialize ML Helper
        try {
            mlHelper = new MLHelper(requireContext());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), 
                    "Error initializing ML model: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTrashMlBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set click listeners
        binding.btnTakePicture.setOnClickListener(v -> takePicture());
        binding.btnClassify.setOnClickListener(v -> classifyTrash());
        binding.btnSave.setOnClickListener(v -> saveTrash());
        binding.btnBack.setOnClickListener(v -> navigateBack());
        
        // Initially disable certain buttons
        binding.btnClassify.setEnabled(false);
        binding.btnSave.setEnabled(false);
        
        // Get current location
        getCurrentLocation();
    }
    
    private void takePicture() {
        if (!PermissionHelper.hasCameraPermissions(requireContext())) {
            PermissionHelper.requestCameraPermissions(this);
            return;
        }
        
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(requireContext(), 
                        "Error creating image file", Toast.LENGTH_SHORT).show();
            }
            
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.example.glean.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    private void getCurrentLocation() {
        if (!PermissionHelper.hasLocationPermissions(requireContext())) {
            PermissionHelper.requestLocationPermissions(this);
            return;
        }
        
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            currentLatitude = location.getLatitude();
                            currentLongitude = location.getLongitude();
                            
                            binding.tvLocation.setText(String.format(Locale.getDefault(),
                                    "Location: %.6f, %.6f", currentLatitude, currentLongitude));
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    
    private void classifyTrash() {
        if (capturedImage == null) {
            Toast.makeText(requireContext(), "No image captured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        executor.execute(() -> {
            if (mlHelper != null) {
                List<MLHelper.Recognition> results = mlHelper.classifyImage(capturedImage);
                
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    
                    if (results != null && !results.isEmpty()) {
                        MLHelper.Recognition topResult = results.get(0);
                        predictedLabel = topResult.getLabel();
                        confidenceScore = topResult.getConfidence();
                        
                        // Display results
                        binding.tvPredictedClass.setText("Predicted: " + predictedLabel);
                        binding.tvConfidence.setText(String.format(Locale.getDefault(),
                                "Confidence: %.1f%%", confidenceScore * 100));
                        
                        // Show top 3 results
                        StringBuilder resultText = new StringBuilder();
                        for (int i = 0; i < Math.min(results.size(), 3); i++) {
                            MLHelper.Recognition recognition = results.get(i);
                            resultText.append(recognition.getLabel())
                                    .append(": ")
                                    .append(String.format(Locale.getDefault(), "%.1f%%", 
                                            recognition.getConfidence() * 100))
                                    .append("\n");
                        }
                        binding.tvResults.setText(resultText.toString());
                        
                        // Enable save button
                        binding.btnSave.setEnabled(true);
                    } else {
                        binding.tvPredictedClass.setText("Could not classify trash");
                        binding.tvConfidence.setText("");
                        binding.tvResults.setText("");
                        binding.btnSave.setEnabled(false);
                    }
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), 
                            "ML model not initialized", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void saveTrash() {
        if (currentPhotoPath == null || predictedLabel.isEmpty()) {
            Toast.makeText(requireContext(), "No valid trash data to save", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (recordId == -1) {
            Toast.makeText(requireContext(), "No active plogging session", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        executor.execute(() -> {
            // Create new trash entity
            TrashEntity trash = new TrashEntity();
            trash.setRecordId(recordId);
            trash.setType(predictedLabel);
            trash.setConfidence(confidenceScore);
            trash.setPhotoPath(currentPhotoPath);
            trash.setLatitude(currentLatitude);
            trash.setLongitude(currentLongitude);
            trash.setTimestamp(System.currentTimeMillis());
            
            // Save to database
            long trashId = db.trashDao().insert(trash);
            
            requireActivity().runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                if (trashId > 0) {
                    Toast.makeText(requireContext(), 
                            "Trash data saved successfully", Toast.LENGTH_SHORT).show();
                    navigateBack();
                } else {
                    Toast.makeText(requireContext(), 
                            "Failed to save trash data", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            try {
                // Load thumbnail from the saved file
                capturedImage = MediaStore.Images.Media.getBitmap(
                        requireContext().getContentResolver(), 
                        Uri.fromFile(new File(currentPhotoPath)));
                
                // Display the captured image
                binding.ivTrashImage.setImageBitmap(capturedImage);
                
                // Enable classify button
                binding.btnClassify.setEnabled(true);
                
                // Reset prediction
                binding.tvPredictedClass.setText("");
                binding.tvConfidence.setText("");
                binding.tvResults.setText("");
                binding.btnSave.setEnabled(false);
                
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), 
                        "Error loading captured image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                Toast.makeText(requireContext(), 
                        "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
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
        if (mlHelper != null) {
            mlHelper.close();
        }
    }
}
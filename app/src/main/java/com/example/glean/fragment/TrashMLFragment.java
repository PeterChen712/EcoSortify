package com.example.glean.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.databinding.FragmentTrashMlBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.helper.GeminiHelper;
import com.example.glean.helper.PermissionHelper;
import com.example.glean.model.TrashEntity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrashMLFragment extends Fragment {

    private static final String TAG = "TrashMLFragment";
    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_PERMISSION = 1002;

    private FragmentTrashMlBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private FusedLocationProviderClient fusedLocationClient;
    
    // Gemini API for AI classification
    private GeminiHelper geminiHelper;
    
    // UI Elements - using findViewById as fallback
    private Button btnTakePhoto;
    private Button btnClassify;
    private Button btnSave;
    private Button btnBack;
    private ImageView imageView;
    private TextView textViewInstructions;
    private TextView textViewResult;
    private ProgressBar progressBar;
    
    private File photoFile;
    private Bitmap capturedImage;
    private String currentPhotoPath;
    private int recordId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize components
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        
        // Initialize Gemini Helper - ONLY AI Classification Method
        try {
            geminiHelper = new GeminiHelper(requireContext());
            if (!geminiHelper.isApiKeyConfigured()) {
                Log.w(TAG, "Gemini API key not configured properly");
                Toast.makeText(requireContext(), "Gemini API key not configured", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GeminiHelper", e);
            Toast.makeText(requireContext(), "Failed to initialize AI classification", Toast.LENGTH_SHORT).show();
        }
        
        // Get arguments
        if (getArguments() != null) {
            recordId = getArguments().getInt("RECORD_ID", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTrashMlBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI elements using findViewById as fallback
        initializeUIElements();
        
        // Set click listeners
        if (btnTakePhoto != null) {
            btnTakePhoto.setOnClickListener(v -> checkPermissionsAndTakePhoto());
        }
        
        if (btnClassify != null) {
            btnClassify.setOnClickListener(v -> classifyTrash());
            btnClassify.setEnabled(false); // Initially disabled
        }
        
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveTrashItem());
            btnSave.setEnabled(false); // Initially disabled
        }
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }
        
        // Set initial instructions
        updateInstructionText("Tap 'Take Photo' to capture a trash image for AI classification.");
    }
    
    private void initializeUIElements() {
        // Try to get UI elements from binding first, then use findViewById
        View rootView = binding.getRoot();
        
        // Buttons
        btnTakePhoto = getButtonSafely(rootView, "btnTakePhoto", "button_take_photo", "btn_take_photo");
        btnClassify = getButtonSafely(rootView, "btnClassify", "button_classify", "btn_classify");
        btnSave = getButtonSafely(rootView, "btnSave", "button_save", "btn_save");
        btnBack = getButtonSafely(rootView, "btnBack", "button_back", "btn_back");
        
        // ImageView
        imageView = getImageViewSafely(rootView, "ivImage", "imageView", "ivTrashImage", "ivPhoto", "image_view");
        
        // TextViews
        textViewInstructions = getTextViewSafely(rootView, "tvInstructions", "tvInstruction", "tvStatus", "tvInfo");
        textViewResult = getTextViewSafely(rootView, "tvClassificationResult", "tvResult", "text_result");
        
        // ProgressBar
        progressBar = getProgressBarSafely(rootView, "progressBar", "progress_bar", "loading");
    }
    
    private Button getButtonSafely(View rootView, String... possibleIds) {
        for (String id : possibleIds) {
            try {
                int resId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
                if (resId != 0) {
                    View view = rootView.findViewById(resId);
                    if (view instanceof Button) {
                        return (Button) view;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Button not found with id: " + id);
            }
        }
        return null;
    }
    
    private ImageView getImageViewSafely(View rootView, String... possibleIds) {
        for (String id : possibleIds) {
            try {
                int resId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
                if (resId != 0) {
                    View view = rootView.findViewById(resId);
                    if (view instanceof ImageView) {
                        return (ImageView) view;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "ImageView not found with id: " + id);
            }
        }
        return null;
    }
    
    private TextView getTextViewSafely(View rootView, String... possibleIds) {
        for (String id : possibleIds) {
            try {
                int resId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
                if (resId != 0) {
                    View view = rootView.findViewById(resId);
                    if (view instanceof TextView) {
                        return (TextView) view;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "TextView not found with id: " + id);
            }
        }
        return null;
    }
    
    private ProgressBar getProgressBarSafely(View rootView, String... possibleIds) {
        for (String id : possibleIds) {
            try {
                int resId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
                if (resId != 0) {
                    View view = rootView.findViewById(resId);
                    if (view instanceof ProgressBar) {
                        return (ProgressBar) view;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "ProgressBar not found with id: " + id);
            }
        }
        return null;
    }
    
    private void checkPermissionsAndTakePhoto() {
        if (PermissionHelper.hasCameraPermission(requireContext())) {
            takePhoto();
        } else {
            PermissionHelper.requestCameraPermission(this, REQUEST_PERMISSION);
        }
    }
    
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error occurred while creating the File", ex);
                Toast.makeText(requireContext(), "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.example.glean.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }
    
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir("Pictures");
        
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == requireActivity().RESULT_OK) {
            // Load the full-size image
            if (photoFile != null && photoFile.exists()) {
                capturedImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                
                if (capturedImage != null) {
                    // Display the image
                    displayCapturedImage();
                    
                    // Enable classify button
                    if (btnClassify != null) {
                        btnClassify.setEnabled(true);
                    }
                    
                    // Update instructions
                    updateInstructionText("Photo captured! Tap 'Classify Trash' to analyze with AI.");
                } else {
                    Toast.makeText(requireContext(), "Failed to load captured image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void displayCapturedImage() {
        if (imageView != null && capturedImage != null) {
            try {
                Glide.with(this)
                        .load(capturedImage)
                        .placeholder(android.R.drawable.ic_menu_camera)
                        .error(android.R.drawable.ic_menu_close_clear_cancel)
                        .centerCrop()
                        .into(imageView);
            } catch (Exception e) {
                Log.e(TAG, "Error displaying image with Glide", e);
                // Fallback to direct bitmap setting
                imageView.setImageBitmap(capturedImage);
            }
        } else {
            Log.w(TAG, "ImageView not found or capturedImage is null");
            Toast.makeText(requireContext(), "Image captured but cannot display", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateInstructionText(String instruction) {
        if (textViewInstructions != null) {
            textViewInstructions.setText(instruction);
        } else if (textViewResult != null) {
            textViewResult.setText(instruction);
            textViewResult.setVisibility(View.VISIBLE);
        } else {
            // Fallback to Toast if no TextView is available
            Toast.makeText(requireContext(), instruction, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void classifyTrash() {
        if (capturedImage == null) {
            Toast.makeText(requireContext(), "Please take a photo first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (geminiHelper == null || !geminiHelper.isApiKeyConfigured()) {
            Toast.makeText(requireContext(), "AI classification not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        if (btnClassify != null) {
            btnClassify.setEnabled(false);
        }
        
        // Update status
        updateInstructionText("🤖 Analyzing image with AI...");
        
        // Classify using Gemini AI
        geminiHelper.classifyTrash(capturedImage, new GeminiHelper.ClassificationCallback() {
            @Override
            public void onSuccess(String trashType, float confidence, String description) {
                requireActivity().runOnUiThread(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    if (btnClassify != null) {
                        btnClassify.setEnabled(true);
                    }
                    
                    // Display results
                    String resultText = String.format(Locale.getDefault(),
                            "🎯 Classification Result:\n\n" +
                            "Type: %s\n" +
                            "Confidence: %.1f%%\n" +
                            "Description: %s",
                            trashType, confidence * 100, description);
                    
                    // Update result text
                    if (textViewResult != null) {
                        textViewResult.setText(resultText);
                        textViewResult.setVisibility(View.VISIBLE);
                    } else {
                        updateInstructionText(resultText);
                    }
                    
                    if (btnSave != null) {
                        btnSave.setEnabled(true);
                    }
                    
                    Log.d(TAG, "Gemini classification successful: " + trashType + " (" + confidence + ")");
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    if (btnClassify != null) {
                        btnClassify.setEnabled(true);
                    }
                    
                    String errorText = "❌ Classification failed: " + error;
                    
                    if (textViewResult != null) {
                        textViewResult.setText(errorText);
                        textViewResult.setVisibility(View.VISIBLE);
                    } else {
                        updateInstructionText(errorText);
                    }
                    
                    Log.e(TAG, "Gemini classification failed: " + error);
                    Toast.makeText(requireContext(), "Classification failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void saveTrashItem() {
        if (capturedImage == null || currentPhotoPath == null) {
            Toast.makeText(requireContext(), "No image to save", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get classification result
        String resultText = "";
        if (textViewResult != null) {
            resultText = textViewResult.getText().toString();
        }
        
        String trashType = "Unknown";
        float confidence = 0.0f;
        String description = "";
        
        // Parse results
        if (resultText.contains("Type: ")) {
            try {
                String[] lines = resultText.split("\n");
                for (String line : lines) {
                    if (line.startsWith("Type: ")) {
                        trashType = line.substring(6).trim();
                    } else if (line.startsWith("Confidence: ")) {
                        String confStr = line.substring(12).replace("%", "").trim();
                        confidence = Float.parseFloat(confStr) / 100.0f;
                    } else if (line.startsWith("Description: ")) {
                        description = line.substring(13).trim();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing classification result", e);
            }
        }
        
        // Get current location and save
        getCurrentLocationAndSave(trashType, confidence, description);
    }
    
    private void getCurrentLocationAndSave(String trashType, float confidence, String description) {
        if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                double latitude = 0.0;
                double longitude = 0.0;
                
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
                
                saveToDatabase(trashType, confidence, description, latitude, longitude);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get location", e);
                saveToDatabase(trashType, confidence, description, 0.0, 0.0);
            });
        } else {
            saveToDatabase(trashType, confidence, description, 0.0, 0.0);
        }
    }
    
    private void saveToDatabase(String trashType, float confidence, String description, double latitude, double longitude) {
        executor.execute(() -> {
            try {
                TrashEntity trash = new TrashEntity();
                trash.setRecordId(recordId);
                trash.setTrashType(trashType);
                trash.setMlLabel(trashType);
                trash.setConfidence(confidence);
                trash.setDescription(description);
                trash.setImagePath(currentPhotoPath);
                trash.setLatitude(latitude);
                trash.setLongitude(longitude);
                trash.setTimestamp(System.currentTimeMillis());
                
                long trashId = db.trashDao().insert(trash);
                
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Trash item saved successfully!", Toast.LENGTH_SHORT).show();
                    updateInstructionText("✅ Trash item saved! Take another photo or go back.");
                    resetForNewCapture();
                    
                    // Navigate back after delay
                    binding.getRoot().postDelayed(this::navigateBack, 1500);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving trash item", e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to save trash item", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void resetForNewCapture() {
        capturedImage = null;
        currentPhotoPath = null;
        photoFile = null;
        
        if (btnClassify != null) {
            btnClassify.setEnabled(false);
        }
        
        if (btnSave != null) {
            btnSave.setEnabled(false);
        }
        
        if (imageView != null) {
            imageView.setImageResource(android.R.drawable.ic_menu_camera);
        }
    }
    
    private void navigateBack() {
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigateUp();
        } catch (Exception e) {
            Log.e(TAG, "Navigation error", e);
            requireActivity().onBackPressed();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(requireContext(), "Camera permission required to take photos", Toast.LENGTH_SHORT).show();
                updateInstructionText("❌ Camera permission required. Please grant permission and try again.");
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
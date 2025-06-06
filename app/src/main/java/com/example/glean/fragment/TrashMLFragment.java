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
            Log.d(TAG, "=== TRASHML FRAGMENT DEBUG ===");
            Log.d(TAG, "INIT: Received recordId from arguments: " + recordId);
            Log.d(TAG, "INIT: All bundle keys: " + getArguments().keySet().toString());
            Log.d(TAG, "=== TRASHML FRAGMENT DEBUG END ===");
        } else {
            Log.e(TAG, "INIT: No arguments received! recordId will be -1");
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
        
        // Initialize UI dengan safe binding
        setupUIElements();
        
        // Set initial instructions
        updateInstructionText("📸 Tap 'Take Photo' to capture a trash image for AI classification.");
    }
    
    private void setupUIElements() {
        try {
            // Setup buttons dengan safe access
            setupTakePhotoButton();
            setupClassifyButton();
            setupSaveButton();
            setupBackButton();
            
            Log.d(TAG, "UI elements setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI elements: " + e.getMessage());
            Toast.makeText(requireContext(), "UI setup error, using fallback mode", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupTakePhotoButton() {
        try {
            // Try multiple possible button IDs
            View takePhotoBtn = findViewSafely("btnTakePhoto", "button_take_photo", "btn_take_photo", "takePhotoButton");
            if (takePhotoBtn != null) {
                takePhotoBtn.setOnClickListener(v -> checkPermissionsAndTakePhoto());
                Log.d(TAG, "Take photo button setup successfully");
            } else {
                Log.w(TAG, "Take photo button not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up take photo button: " + e.getMessage());
        }
    }
    
    private void setupClassifyButton() {
        try {
            View classifyBtn = findViewSafely("btnClassify", "button_classify", "btn_classify", "classifyButton");
            if (classifyBtn != null) {
                classifyBtn.setOnClickListener(v -> classifyTrash());
                classifyBtn.setEnabled(false); // Initially disabled
                Log.d(TAG, "Classify button setup successfully");
            } else {
                Log.w(TAG, "Classify button not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up classify button: " + e.getMessage());
        }
    }
    
    private void setupSaveButton() {
        try {
            View saveBtn = findViewSafely("btnSave", "button_save", "btn_save", "saveButton");
            if (saveBtn != null) {
                saveBtn.setOnClickListener(v -> saveTrashItem());
                saveBtn.setEnabled(false); // Initially disabled
                Log.d(TAG, "Save button setup successfully");
            } else {
                Log.w(TAG, "Save button not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up save button: " + e.getMessage());
        }
    }
    
    private void setupBackButton() {
        try {
            View backBtn = findViewSafely("btnBack", "button_back", "btn_back", "backButton");
            if (backBtn != null) {
                backBtn.setOnClickListener(v -> navigateBack());
                Log.d(TAG, "Back button setup successfully");
            } else {
                // Try to use toolbar back button or activity back
                Log.d(TAG, "Back button not found, will use system back");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up back button: " + e.getMessage());
        }
    }
    
    private View findViewSafely(String... possibleIds) {
        if (binding == null || binding.getRoot() == null) {
            return null;
        }
        
        for (String id : possibleIds) {
            try {
                int resId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
                if (resId != 0) {
                    View view = binding.getRoot().findViewById(resId);
                    if (view != null) {
                        return view;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "View not found with id: " + id);
            }
        }
        return null;
    }
    
    private void updateInstructionText(String instruction) {
        try {
            // Try to find instruction TextView
            View instructionView = findViewSafely("tvInstructions", "tvInstruction", "tvStatus", "tvInfo", "textInstructions");
            if (instructionView instanceof android.widget.TextView) {
                ((android.widget.TextView) instructionView).setText(instruction);
                return;
            }
            
            // Try to find result TextView as fallback
            View resultView = findViewSafely("tvClassificationResult", "tvResult", "textResult", "tvDescription");
            if (resultView instanceof android.widget.TextView) {
                ((android.widget.TextView) resultView).setText(instruction);
                resultView.setVisibility(View.VISIBLE);
                return;
            }
            
            // Fallback to Toast
            Toast.makeText(requireContext(), instruction, Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating instruction text: " + e.getMessage());
            Toast.makeText(requireContext(), instruction, Toast.LENGTH_SHORT).show();
        }
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
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error occurred while creating the File", ex);
                updateInstructionText("❌ Error creating image file");
                return;
            }
            
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.example.glean.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            updateInstructionText("❌ No camera app found");
        }
    }
    
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "GLEAN_TRASH_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir("Pictures");
        
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == requireActivity().RESULT_OK) {
            if (photoFile != null && photoFile.exists()) {
                capturedImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                
                if (capturedImage != null) {
                    displayCapturedImage();
                    enableButton("btnClassify", "button_classify", "btn_classify");
                    updateInstructionText("✅ Photo captured! Tap 'Classify Trash' to analyze with AI.");
                } else {
                    updateInstructionText("❌ Failed to load captured image");
                }
            }
        }
    }
    
    private void displayCapturedImage() {
        try {
            // Find ImageView
            View imageView = findViewSafely("ivImage", "imageView", "ivTrashImage", "ivPhoto", "capturedImageView");
            
            if (imageView instanceof android.widget.ImageView && capturedImage != null) {
                try {
                    // Use Glide with enhanced error handling
                    Glide.with(this)
                            .load(capturedImage)
                            .placeholder(R.drawable.ic_placeholder)
                            .error(R.drawable.ic_error)
                            .centerCrop()
                            .into((android.widget.ImageView) imageView);
                    
                    Log.d(TAG, "Image displayed successfully with Glide");
                } catch (Exception e) {
                    Log.e(TAG, "Glide error, using direct bitmap: " + e.getMessage());
                    ((android.widget.ImageView) imageView).setImageBitmap(capturedImage);
                }
            } else {
                Log.w(TAG, "ImageView not found or image is null");
                updateInstructionText("📷 Image captured but cannot display");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying image: " + e.getMessage());
        }
    }
    
    private void enableButton(String... buttonIds) {
        View button = findViewSafely(buttonIds);
        if (button != null) {
            button.setEnabled(true);
        }
    }
    
    private void disableButton(String... buttonIds) {
        View button = findViewSafely(buttonIds);
        if (button != null) {
            button.setEnabled(false);
        }
    }
    
    private void classifyTrash() {
        if (capturedImage == null) {
            updateInstructionText("❌ Please take a photo first");
            return;
        }
        
        if (geminiHelper == null || !geminiHelper.isApiKeyConfigured()) {
            updateInstructionText("❌ AI classification not available");
            return;
        }
        
        // Show loading state
        showProgressBar(true);
        disableButton("btnClassify", "button_classify", "btn_classify");
        updateInstructionText("🤖 Analyzing image with AI...");
        
        // Classify using Gemini AI
        geminiHelper.classifyTrash(capturedImage, new GeminiHelper.ClassificationCallback() {
            @Override
            public void onSuccess(String trashType, float confidence, String description) {
                requireActivity().runOnUiThread(() -> {
                    showProgressBar(false);
                    enableButton("btnClassify", "button_classify", "btn_classify");
                    
                    // Display results
                    String resultText = String.format(Locale.getDefault(),
                            "🎯 AI Classification Result:\n\n" +
                            "📋 Type: %s\n" +
                            "📊 Confidence: %.1f%%\n" +
                            "📝 Description: %s\n\n" +
                            "Ready to save!",
                            trashType, confidence * 100, description);
                    
                    updateResultText(resultText);
                    enableButton("btnSave", "button_save", "btn_save");
                    
                    Log.d(TAG, "Gemini classification successful: " + trashType + " (" + confidence + ")");
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    showProgressBar(false);
                    enableButton("btnClassify", "button_classify", "btn_classify");
                    
                    String errorText = "❌ Classification failed: " + error + "\n\nTry again or check your internet connection.";
                    updateResultText(errorText);
                    
                    Log.e(TAG, "Gemini classification failed: " + error);
                });
            }
        });
    }
    
    private void updateResultText(String resultText) {
        try {
            View resultView = findViewSafely("tvClassificationResult", "tvResult", "textResult", "tvDescription");
            if (resultView instanceof android.widget.TextView) {
                ((android.widget.TextView) resultView).setText(resultText);
                resultView.setVisibility(View.VISIBLE);
            } else {
                updateInstructionText(resultText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating result text: " + e.getMessage());
            updateInstructionText(resultText);
        }
    }
    
    private void showProgressBar(boolean show) {
        try {
            View progressBar = findViewSafely("progressBar", "progress_bar", "loading", "progressIndicator");
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error controlling progress bar: " + e.getMessage());
        }
    }
    
    private void saveTrashItem() {
        if (capturedImage == null || currentPhotoPath == null) {
            updateInstructionText("❌ No image to save");
            return;
        }
        
        // Get classification result
        String resultText = getResultText();
        String trashType = "Unknown";
        float confidence = 0.0f;
        String description = "";
        
        // Parse results
        if (resultText.contains("Type: ")) {
            try {
                String[] lines = resultText.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("📋 Type: ") || line.startsWith("Type: ")) {
                        trashType = line.substring(line.indexOf(": ") + 2).trim();
                    } else if (line.startsWith("📊 Confidence: ") || line.startsWith("Confidence: ")) {
                        String confStr = line.substring(line.indexOf(": ") + 2).replace("%", "").trim();
                        confidence = Float.parseFloat(confStr) / 100.0f;
                    } else if (line.startsWith("📝 Description: ") || line.startsWith("Description: ")) {
                        description = line.substring(line.indexOf(": ") + 2).trim();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing classification result", e);
            }
        }
        
        // Get current location and save
        getCurrentLocationAndSave(trashType, confidence, description);
    }
    
    private String getResultText() {
        try {
            View resultView = findViewSafely("tvClassificationResult", "tvResult", "textResult", "tvDescription");
            if (resultView instanceof android.widget.TextView) {
                return ((android.widget.TextView) resultView).getText().toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting result text: " + e.getMessage());
        }
        return "";
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
                    Log.d(TAG, "Location obtained: " + latitude + ", " + longitude);
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
        Log.d(TAG, "=== TRASH SAVE DEBUG START ===");
        Log.d(TAG, "SAVE: recordId = " + recordId);
        Log.d(TAG, "SAVE: trashType = " + trashType);
        Log.d(TAG, "SAVE: confidence = " + confidence);
        Log.d(TAG, "SAVE: description = " + description);
        Log.d(TAG, "SAVE: latitude = " + latitude);
        Log.d(TAG, "SAVE: longitude = " + longitude);
        Log.d(TAG, "SAVE: currentPhotoPath = " + currentPhotoPath);
        
        executor.execute(() -> {
            try {
                Log.d(TAG, "SAVE: Creating TrashEntity...");
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
                
                Log.d(TAG, "SAVE: TrashEntity created, attempting to insert to database...");
                Log.d(TAG, "SAVE: Entity details - RecordId: " + trash.getRecordId() + 
                      ", Type: " + trash.getTrashType() + ", Timestamp: " + trash.getTimestamp());
                
                long trashId = db.trashDao().insert(trash);
                
                Log.d(TAG, "SAVE: Insert completed! Trash item saved with ID: " + trashId);
                
                // Verify the insertion by querying back
                TrashEntity savedTrash = db.trashDao().getTrashByIdSync((int)trashId);
                if (savedTrash != null) {
                    Log.d(TAG, "SAVE: Verification successful - Trash exists in database with RecordId: " + savedTrash.getRecordId());
                } else {
                    Log.e(TAG, "SAVE: Verification FAILED - Trash not found in database after insert!");
                }
                
                // Check total trash count for this record
                int totalTrashCount = db.trashDao().getTrashCountByRecordIdSync(recordId);
                Log.d(TAG, "SAVE: Total trash count for record " + recordId + ": " + totalTrashCount);
                
                requireActivity().runOnUiThread(() -> {
                    updateInstructionText("✅ Trash item saved successfully!\n\nContributing to cleaner environment! 🌱");
                    resetForNewCapture();
                    
                    // Navigate back after delay
                    binding.getRoot().postDelayed(this::navigateBack, 2000);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "SAVE: Error saving trash item", e);
                Log.e(TAG, "SAVE: Error details - Message: " + e.getMessage());
                Log.e(TAG, "SAVE: Error details - Cause: " + e.getCause());
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    updateInstructionText("❌ Failed to save trash item. Please try again.");
                });
            }
            Log.d(TAG, "=== TRASH SAVE DEBUG END ===");
        });
    }
    
    private void resetForNewCapture() {
        try {
            capturedImage = null;
            currentPhotoPath = null;
            photoFile = null;
            
            disableButton("btnClassify", "button_classify", "btn_classify");
            disableButton("btnSave", "button_save", "btn_save");
            
            // Clear image display
            View imageView = findViewSafely("ivImage", "imageView", "ivTrashImage", "ivPhoto");
            if (imageView instanceof android.widget.ImageView) {
                ((android.widget.ImageView) imageView).setImageResource(R.drawable.ic_placeholder);
            }
            
            Log.d(TAG, "UI reset for new capture");
        } catch (Exception e) {
            Log.e(TAG, "Error resetting for new capture: " + e.getMessage());
        }
    }
    
    private void navigateBack() {
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigateUp();
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage());
            try {
                requireActivity().onBackPressed();
            } catch (Exception ex) {
                Log.e(TAG, "Back press error: " + ex.getMessage());
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                updateInstructionText("❌ Camera permission required. Please grant permission in settings and try again.");
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
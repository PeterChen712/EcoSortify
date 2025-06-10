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
    
    // AI Confidence Configuration
    private static final float AI_CONFIDENCE_THRESHOLD = 0.6f; // 60% minimum confidence
    private static final float AI_VERY_LOW_CONFIDENCE = 0.3f;  // 30% - very uncertain
    
    // Non-trash detection keywords
    private static final String[] NON_TRASH_KEYWORDS = {
        "not trash", "bukan sampah", "tidak sampah", 
        "unknown", "tidak dikenal", "bukan barang"
    };

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
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI dengan safe binding
        setupUIElements();
        
        // Set initial instructions
        updateInstructionText("Ketuk box hijau untuk mengambil foto sampah dengan kamera.");
    }      private void setupUIElements() {
        try {
            // Setup camera box click listener (for taking photos)
            setupCameraBoxClickListener();
            
            // Setup start detection button (for AI processing)
            setupStartDetectionButton();
            
            // Setup retake photo button
            setupRetakePhotoButton();
            
            Log.d(TAG, "UI elements setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI elements: " + e.getMessage());
            Toast.makeText(requireContext(), "UI setup error, using fallback mode", Toast.LENGTH_SHORT).show();
        }
    }      private void setupCameraBoxClickListener() {
        try {
            // Setup camera box click untuk take photo
            View cameraBox = findViewSafely("card_camera_preview", "cardCameraPreview");
            if (cameraBox != null) {
                cameraBox.setOnClickListener(v -> checkPermissionsAndTakePhoto());
                Log.d(TAG, "Camera box click listener setup successfully");
            } else {
                Log.w(TAG, "Camera box not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up camera box click listener: " + e.getMessage());
        }
    }
    
      private void setupStartDetectionButton() {
        try {
            // Button untuk memulai AI processing (hanya aktif setelah foto diambil)
            View startDetectionBtn = findViewSafely("btnStartDetection", "btn_start_detection", "button_start_detection");
            if (startDetectionBtn != null) {
                startDetectionBtn.setOnClickListener(v -> classifyTrash());
                startDetectionBtn.setEnabled(false); // Initially disabled
                Log.d(TAG, "Start detection button setup successfully");
            } else {
                Log.w(TAG, "Start detection button not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up start detection button: " + e.getMessage());
        }
    }
    
    private void setupRetakePhotoButton() {
        try {
            // Setup retake photo button
            View retakeBtn = findViewSafely("fab_retake_photo", "fabRetakePhoto");
            if (retakeBtn != null) {
                retakeBtn.setOnClickListener(v -> retakePhoto());
                Log.d(TAG, "Retake photo button setup successfully");
            } else {
                Log.w(TAG, "Retake photo button not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up retake photo button: " + e.getMessage());
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
            // Update text in main camera card
            View mainCardText = findViewSafely("tvCameraInstruction", "tv_camera_instruction");
            if (mainCardText instanceof android.widget.TextView) {
                ((android.widget.TextView) mainCardText).setText(instruction);
                return;
            }
            
            // Fallback to status text
            View statusView = findViewSafely("tvStatus", "tv_status");
            if (statusView instanceof android.widget.TextView) {
                ((android.widget.TextView) statusView).setText(instruction);
                statusView.setVisibility(View.VISIBLE);
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
                updateInstructionText("❌ Gagal membuat file gambar");
                return;
            }
            
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.example.glean.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            updateInstructionText("❌ Aplikasi kamera tidak ditemukan");
        }
    }
    
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "GLEAN_TRASH_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir("Pictures");
        
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }      @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == requireActivity().RESULT_OK) {
            if (photoFile != null && photoFile.exists()) {
                capturedImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                  if (capturedImage != null) {
                    displayCapturedImage();                    enableStartDetectionButton();
                    updateInstructionText(getString(R.string.photo_taken_success));
                } else {
                    updateInstructionText(getString(R.string.failed_to_load_photo));
                }
            }
        }
    }      private void displayCapturedImage() {
        try {
            // Hide camera instruction view
            View cameraInstruction = findViewSafely("layout_camera_instruction");
            if (cameraInstruction != null) {
                cameraInstruction.setVisibility(View.GONE);
            }
            
            // Show captured image
            View imageView = findViewSafely("imageView", "image_view", "image_view_main", "ivTrashImage", "ivPhoto");
            
            if (imageView instanceof android.widget.ImageView && capturedImage != null) {
                imageView.setVisibility(View.VISIBLE);
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
                
                // Show retake photo overlay
                View photoOverlay = findViewSafely("layout_photo_overlay");
                if (photoOverlay != null) {
                    photoOverlay.setVisibility(View.VISIBLE);
                }
                
            } else {
                Log.w(TAG, "ImageView not found or image is null");
                updateInstructionText("Foto berhasil diambil tapi tidak bisa ditampilkan");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying image: " + e.getMessage());
        }
    }
      private void enableButton(String... buttonIds) {
        View button = findViewSafely(buttonIds);
        if (button != null) {
            button.setEnabled(true);
            button.setAlpha(1.0f);
        }
    }
    
    private void disableButton(String... buttonIds) {
        View button = findViewSafely(buttonIds);
        if (button != null) {
            button.setEnabled(false);
            button.setAlpha(0.5f);
        }
    }
    
    private void enableStartDetectionButton() {
        enableButton("btnStartDetection", "btn_start_detection", "button_start_detection");
    }
    
    private void disableStartDetectionButton() {
        disableButton("btnStartDetection", "btn_start_detection", "button_start_detection");
    }
    
    private void retakePhoto() {
        // Reset untuk foto baru
        resetForNewCapture();
        checkPermissionsAndTakePhoto();
    }
    
    private void showProgressOverlay() {
        try {
            View progressOverlay = findViewSafely("progress_overlay");
            if (progressOverlay != null) {
                progressOverlay.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing progress overlay: " + e.getMessage());
        }
    }
    
    private void hideProgressOverlay() {
        try {
            View progressOverlay = findViewSafely("progress_overlay");
            if (progressOverlay != null) {
                progressOverlay.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding progress overlay: " + e.getMessage());
        }
    }    private void classifyTrash() {
        if (capturedImage == null) {
            updateInstructionText(getString(R.string.take_photo_first));
            return;
        }
        
        if (geminiHelper == null || !geminiHelper.isApiKeyConfigured()) {
            updateInstructionText(getString(R.string.ai_detection_unavailable));
            return;
        }        // Show loading state with progress overlay
        showProgressOverlay();
        disableStartDetectionButton();
        updateInstructionText(getString(R.string.analyzing_with_ai));
        
        // Hide any previous results or warnings
        hideClassificationResult();
        hideConfidenceWarning();
          // Classify using Gemini AI
        geminiHelper.classifyTrash(capturedImage, new GeminiHelper.ClassificationCallback() {            @Override
            public void onSuccess(String trashType, float confidence, String description) {
                requireActivity().runOnUiThread(() -> {
                    hideProgressOverlay();
                    enableStartDetectionButton();
                    
                    // Check confidence threshold and non-trash detection
                    if (confidence < AI_CONFIDENCE_THRESHOLD || isNonTrashItem(trashType)) {
                        
                        // Show confidence warning
                        showConfidenceWarning(confidence, trashType);
                        hideClassificationResult();
                        
                        Log.d(TAG, "Low confidence or non-trash detected: " + trashType + " (" + confidence + ")");
                        return;
                    }
                    
                    // Hide confidence warning if previously shown
                    hideConfidenceWarning();
                    
                    // Display successful results with waste category
                    String wasteCategory = determineWasteCategory(trashType);
                    String resultText = String.format(Locale.getDefault(),
                            "📋 Jenis: %s\n" +
                            "🗂️ Kategori: %s\n" +
                            "📊 Tingkat Keyakinan: %.1f%%\n" +
                            "📝 Deskripsi: %s\n\n" +
                            "Siap untuk disimpan! 🌱",
                            trashType, wasteCategory, confidence * 100, description);
                    
                    showClassificationResult(resultText);
                    
                    Log.d(TAG, "Gemini classification successful: " + trashType + " (" + confidence + ")");
                });
            }              @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    hideProgressOverlay();
                    enableStartDetectionButton();
                    
                    String errorText = "❌ Deteksi gagal: " + error + "\n\nCoba lagi atau periksa koneksi internet kamu.";
                    updateInstructionText(errorText);
                    
                    Log.e(TAG, "Gemini classification failed: " + error);
                });
            }
        });
    }
      private void updateResultText(String resultText) {
        try {
            View resultView = findViewSafely("tvClassificationResult", "tv_classification_result");
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
    
    private void showClassificationResult(String resultText) {
        try {
            // Show result card
            View resultCard = findViewSafely("cardClassificationResult", "card_classification_result");
            if (resultCard != null) {
                resultCard.setVisibility(View.VISIBLE);
            }
            
            // Update result text
            updateResultText(resultText);
            
            // Setup save button
            View saveBtn = findViewSafely("btnSave", "btn_save");
            if (saveBtn != null) {
                saveBtn.setOnClickListener(v -> saveTrashItem());
                saveBtn.setEnabled(true);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing classification result: " + e.getMessage());
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
      private void showConfidenceWarning(float confidence, String trashType) {
        try {
            View warningCard = findViewSafely("cardConfidenceWarning", "card_confidence_warning");
            if (warningCard != null) {
                warningCard.setVisibility(View.VISIBLE);
                
                // Update warning text
                View warningText = findViewSafely("tvConfidenceWarning", "tv_confidence_warning");
                if (warningText instanceof android.widget.TextView) {
                    String message = generateConfidenceWarningMessage(confidence, trashType);
                    ((android.widget.TextView) warningText).setText(message);
                }
                
                // Setup retake photo button
                View retakeBtn = findViewSafely("btnRetakePhoto", "btn_retake_photo");
                if (retakeBtn != null) {
                    retakeBtn.setOnClickListener(v -> {
                        hideConfidenceWarning();
                        resetForNewCapture();                        updateInstructionText(getString(R.string.ready_for_new_photo_detailed));
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing confidence warning: " + e.getMessage());
            updateInstructionText(getString(R.string.ai_not_sure_detailed));
        }
    }
    
    private void hideConfidenceWarning() {
        try {
            View warningCard = findViewSafely("cardConfidenceWarning", "card_confidence_warning");
            if (warningCard != null) {
                warningCard.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding confidence warning: " + e.getMessage());
        }
    }
      private void hideClassificationResult() {
        try {
            View resultCard = findViewSafely("cardClassificationResult", "card_classification_result");
            if (resultCard != null) {
                resultCard.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding classification result: " + e.getMessage());
        }
    }    private void saveTrashItem() {
        if (capturedImage == null) {
            updateInstructionText("❌ Tidak ada data untuk disimpan");
            return;
        }
        
        // Get classification result
        String resultText = getResultText();
        String trashType = "Tidak Dikenal";
        float confidence = 0.0f;
        String description = "";
          // Parse results - support new format with category
        if (resultText.contains("Jenis: ") || resultText.contains("Type: ")) {
            try {
                String[] lines = resultText.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("📋 Jenis: ") || line.startsWith("📋 Type: ")) {
                        trashType = line.substring(line.indexOf(": ") + 2).trim();
                    } else if (line.startsWith("🗂️ Kategori: ") || line.startsWith("🗂️ Category: ")) {
                        // Category is already included in the result, we can extract it if needed
                        String category = line.substring(line.indexOf(": ") + 2).trim();
                        description = description + " [Kategori: " + category + "]";                    } else if (line.startsWith("📊 Tingkat Keyakinan: ") || line.startsWith("📊 Confidence: ")) {
                        String confStr = line.substring(line.indexOf(": ") + 2).replace("%", "").trim();
                        // Handle different locale decimal separators (comma vs dot)
                        confStr = confStr.replace(",", ".");
                        confidence = Float.parseFloat(confStr) / 100.0f;
                    } else if (line.startsWith("📝 Deskripsi: ") || line.startsWith("📝 Description: ")) {
                        description = line.substring(line.indexOf(": ") + 2).trim();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing classification result", e);
            }
        }
        
        // Clean up temporary photo immediately after AI processing
        cleanupTemporaryPhoto();
        
        // Get current location and save (without photo)
        getCurrentLocationAndSave(trashType, confidence, description);
    }
      private String getResultText() {
        try {
            View resultView = findViewSafely("tvClassificationResult", "tv_classification_result");
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
            try {                Log.d(TAG, "SAVE: Creating TrashEntity...");
                TrashEntity trash = new TrashEntity();
                trash.setRecordId(recordId);
                trash.setTrashType(trashType);
                trash.setMlLabel(trashType);
                trash.setConfidence(confidence);
                trash.setDescription(description);
                // NOTE: ImagePath is intentionally NOT set - photos are not saved per requirement
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
                    updateInstructionText(getString(R.string.trash_saved_success));
                    resetForNewCapture();
                    
                    // Navigate back after delay
                    binding.getRoot().postDelayed(this::navigateBack, 2000);
                });
                
            } catch (Exception e) {                Log.e(TAG, "SAVE: Error saving trash item", e);
                Log.e(TAG, "SAVE: Error details - Message: " + e.getMessage());
                Log.e(TAG, "SAVE: Error details - Cause: " + e.getCause());
                e.printStackTrace();
                
                requireActivity().runOnUiThread(() -> {
                    updateInstructionText("❌ Gagal menyimpan data sampah. Silakan coba lagi.");
                });
            }
            Log.d(TAG, "=== TRASH SAVE DEBUG END ===");
        });
    }    private void resetForNewCapture() {
        try {
            // Clean up temporary photo before resetting
            cleanupTemporaryPhoto();
            
            // Clear captured image from memory
            capturedImage = null;
            photoFile = null;
            
            // Hide all result cards
            hideClassificationResult();
            hideConfidenceWarning();
            
            // Hide captured image card
            View imageCard = findViewSafely("cardCapturedImage", "card_captured_image");
            if (imageCard != null) {
                imageCard.setVisibility(View.GONE);
            }
            
            Log.d(TAG, "UI reset for new capture with photo cleanup");
        } catch (Exception e) {
            Log.e(TAG, "Error resetting for new capture: " + e.getMessage());
        }
    }
    
    /**
     * Determine waste category based on trash type
     * @param trashType The detected trash type
     * @return Category: Organik, Anorganik, or B3
     */
    private String determineWasteCategory(String trashType) {
        if (trashType == null) return "Tidak Dikenal";
        
        String lowerType = trashType.toLowerCase();
        
        // B3 (Bahan Berbahaya dan Beracun)
        String[] b3Keywords = {
            "battery", "baterai", "toxic", "beracun", "chemical", "kimia",
            "pesticide", "pestisida", "paint", "cat", "medical", "medis",
            "syringe", "jarum", "electronic", "elektronik", "fluorescent", "mercury"
        };
        
        for (String keyword : b3Keywords) {
            if (lowerType.contains(keyword)) {
                return "B3 (Berbahaya & Beracun)";
            }
        }
        
        // Organik
        String[] organicKeywords = {
            "food", "makanan", "fruit", "buah", "vegetable", "sayur", "leaf", "daun",
            "paper", "kertas", "wood", "kayu", "organic", "organik", "compost", "kompos",
            "banana", "pisang", "apple", "apel", "rice", "nasi", "bread", "roti"
        };
        
        for (String keyword : organicKeywords) {
            if (lowerType.contains(keyword)) {
                return "Organik";
            }
        }
        
        // Anorganik (default for most other waste)
        String[] inorganicKeywords = {
            "plastic", "plastik", "bottle", "botol", "can", "kaleng", "metal", "logam",
            "glass", "kaca", "rubber", "karet", "fabric", "kain", "styrofoam"
        };
        
        for (String keyword : inorganicKeywords) {
            if (lowerType.contains(keyword)) {
                return "Anorganik";
            }
        }
        
        // Default fallback
        return "Anorganik";
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
                takePhoto();            } else {
                updateInstructionText("❌ Izin kamera diperlukan. Berikan izin di pengaturan dan coba lagi.");
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
    
    /**
     * Helper method to check if the detected item is not trash based on keywords
     * @param trashType The detected item type
     * @return true if the item should be considered as non-trash
     */
    private boolean isNonTrashItem(String trashType) {
        if (trashType == null) return true;
        
        String lowerType = trashType.toLowerCase();
        for (String keyword : NON_TRASH_KEYWORDS) {
            if (lowerType.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Helper method to generate appropriate confidence warning message
     * @param confidence AI confidence level
     * @param trashType Detected item type
     * @return Appropriate warning message in Indonesian
     */
    private String generateConfidenceWarningMessage(float confidence, String trashType) {
        if (confidence < AI_VERY_LOW_CONFIDENCE) {
            return String.format("AI sangat tidak yakin (%.1f%%) bahwa ini adalah sampah. " +
                    "Coba foto objek sampah yang lebih jelas dengan pencahayaan yang baik.", 
                    confidence * 100);
        } else if (isNonTrashItem(trashType)) {
            return "AI mendeteksi bahwa ini kemungkinan bukan sampah. " +
                    "Pastikan kamu memfoto sampah yang jelas dan benar.";
        } else {
            return String.format("Tingkat keyakinan AI rendah (%.1f%%). " +
                    "Coba ambil foto dengan sudut yang berbeda atau pencahayaan yang lebih baik.", 
                    confidence * 100);
        }
    }
    
    /**
     * Clean up temporary photo file and memory after AI processing
     * This ensures photos are not permanently stored on device
     */
    private void cleanupTemporaryPhoto() {
        try {
            // Delete temporary photo file from storage
            if (photoFile != null && photoFile.exists()) {
                boolean deleted = photoFile.delete();
                Log.d(TAG, "Temporary photo file deleted: " + deleted);
            }
            
            // Clear photo path
            currentPhotoPath = null;
            
            Log.d(TAG, "Temporary photo cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up temporary photo: " + e.getMessage());
        }
    }
}
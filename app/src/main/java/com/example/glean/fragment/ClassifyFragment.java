package com.example.glean.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.glean.R;
import com.example.glean.db.AppDatabase;
import com.example.glean.ml.ClassificationHelper;
import com.example.glean.model.ClassificationFeedback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClassifyFragment extends Fragment {

    private static final String TAG = "ClassifyFragment";
    
    // UI components
    private ImageView imagePreview;
    private MaterialCardView cardCamera;
    private MaterialCardView cardGallery;
    private LinearLayout emptyState;
    private LinearLayout resultContainer;
    private CircularProgressIndicator progressIndicator;
    private LinearLayout feedbackContainer;
    private MaterialButtonToggleGroup feedbackToggleGroup;
    private MaterialButton submitFeedbackButton;
    
    private TextView resultType;
    private TextView resultTitle;
    private TextView resultDescription;
    private TextView resultTips;
    private MaterialButton shareButton;
    
    // API and data
    private ClassificationHelper classificationHelper;
    private Bitmap selectedImage;
    private Uri photoUri;
    private String classificationResult;
    private String classificationDescription;
    private String selectedFeedback;
    
    // Permission launchers
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;
    
    // Activity result launchers
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> selectImageLauncher;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_classify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI components
        imagePreview = view.findViewById(R.id.image_preview);
        cardCamera = view.findViewById(R.id.card_camera);
        cardGallery = view.findViewById(R.id.card_gallery);
        emptyState = view.findViewById(R.id.empty_state);
        resultContainer = view.findViewById(R.id.result_container);
        progressIndicator = view.findViewById(R.id.progress_indicator);
        feedbackContainer = view.findViewById(R.id.feedback_container);
        feedbackToggleGroup = view.findViewById(R.id.feedback_toggle_group);
        submitFeedbackButton = view.findViewById(R.id.btn_submit_feedback);
        
        resultType = view.findViewById(R.id.result_type);
        resultTitle = view.findViewById(R.id.result_title);
        resultDescription = view.findViewById(R.id.result_description);
        resultTips = view.findViewById(R.id.result_tips);
        shareButton = view.findViewById(R.id.share_button);
        
        // Initialize API
        classificationHelper = new ClassificationHelper(requireContext());
        
        // Setup permission and activity result launchers
        setupPermissionLaunchers();
        setupActivityResultLaunchers();
        
        // Setup click listeners
        cardCamera.setOnClickListener(v -> checkCameraPermission());
        cardGallery.setOnClickListener(v -> checkStoragePermission());
        shareButton.setOnClickListener(v -> shareResults());
        
        // Setup feedback buttons
        feedbackToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_organik) {
                    selectedFeedback = "ORGANIK";
                } else if (checkedId == R.id.btn_anorganik) {
                    selectedFeedback = "ANORGANIK";
                } else if (checkedId == R.id.btn_b3) {
                    selectedFeedback = "B3";
                }
            }
        });
        
        submitFeedbackButton.setOnClickListener(v -> {
            if (selectedFeedback != null && classificationResult != null && selectedImage != null) {
                submitFeedback(classificationResult, selectedFeedback, selectedImage);
            } else {
                Toast.makeText(requireContext(), "Pilih kategori sampah yang benar", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Initial UI state
        showEmptyState();
    }
    
    private void setupPermissionLaunchers() {
        // Camera permission launcher
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(requireContext(),
                                "Izin kamera diperlukan untuk mengambil foto",
                                Toast.LENGTH_SHORT).show();
                    }
                });
        
        // Storage permission launcher
        requestStoragePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(requireContext(),
                                "Izin penyimpanan diperlukan untuk memilih gambar",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void setupActivityResultLaunchers() {
        // Camera result launcher
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK) {
                        try {
                            // Load the captured image
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    requireContext().getContentResolver(), photoUri);
                            handleImageSelected(bitmap);
                        } catch (IOException e) {
                            Log.e(TAG, "Error loading camera image", e);
                            Toast.makeText(requireContext(), 
                                    "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        
        // Gallery result launcher
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                        try {
                            Uri selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                InputStream inputStream = requireContext().getContentResolver()
                                        .openInputStream(selectedImageUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                if (bitmap != null) {
                                    handleImageSelected(bitmap);
                                }
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error loading gallery image", e);
                            Toast.makeText(requireContext(), 
                                    "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == 
                PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
    
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses more granular permissions
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == 
                    PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // For older versions
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == 
                    PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }
    
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            try {
                // Create temporary file for photo
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                File storageDir = requireContext().getExternalFilesDir(null);
                File photoFile = File.createTempFile(
                        "JPEG_" + timeStamp,  /* prefix */
                        ".jpg",               /* suffix */
                        storageDir            /* directory */
                );
                
                // Get URI for the file
                photoUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile);
                
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePictureLauncher.launch(takePictureIntent);
            } catch (IOException e) {
                Log.e(TAG, "Error creating image file", e);
                Toast.makeText(requireContext(), 
                        "Gagal membuat file gambar", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), 
                    "Tidak ada aplikasi kamera yang tersedia", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        selectImageLauncher.launch(intent);
    }
    
    // Update handleImageSelected method
    private void handleImageSelected(Bitmap bitmap) {
        selectedImage = bitmap;
        imagePreview.setImageBitmap(bitmap);
        showLoadingState();
        
        // Use ClassificationHelper for hybrid classification
        classificationHelper.classifyImage(bitmap, new ClassificationHelper.ClassificationCallback() {
            @Override
            public void onSuccess(String wasteType, String description, String tips, boolean isLocalModel) {
                requireActivity().runOnUiThread(() -> {
                    classificationResult = wasteType;
                    classificationDescription = description;
                    
                    // Show result with model source indicator
                    showResultState(wasteType, description);
                    
                    // Optionally show which model was used
                    String modelSource = isLocalModel ? "Model Lokal (Offline)" : "Gemini API (Online)";
                    Toast.makeText(requireContext(), 
                            "Klasifikasi menggunakan: " + modelSource, 
                            Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                    showImagePreviewState();
                });
            }
        });
    }
    
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        imagePreview.setVisibility(View.GONE);
        resultContainer.setVisibility(View.GONE);
        progressIndicator.setVisibility(View.GONE);
    }
    
    private void showImagePreviewState() {
        emptyState.setVisibility(View.GONE);
        imagePreview.setVisibility(View.VISIBLE);
        resultContainer.setVisibility(View.GONE);
        progressIndicator.setVisibility(View.GONE);
    }
    
    private void showLoadingState() {
        emptyState.setVisibility(View.GONE);
        imagePreview.setVisibility(View.VISIBLE);
        resultContainer.setVisibility(View.GONE);
        progressIndicator.setVisibility(View.VISIBLE);
    }
    
    private void showResultState(String wasteType, String description) {
        emptyState.setVisibility(View.GONE);
        imagePreview.setVisibility(View.VISIBLE);
        resultContainer.setVisibility(View.VISIBLE);
        progressIndicator.setVisibility(View.GONE);
        
        // Set waste type and related info
        resultType.setText(wasteType);
        
        // Set color based on waste type
        int color = getColorForWasteType(wasteType);
        resultType.setTextColor(color);
        
        // Set title based on waste type
        String title = getTitleForWasteType(wasteType);
        resultTitle.setText(title);
        
        // Set description
        resultDescription.setText(description);
        
        // Set tips based on waste type
        String tips = getTipsForWasteType(wasteType);
        resultTips.setText(tips);
        
        // Also show feedback container
        feedbackContainer.setVisibility(View.VISIBLE);
        
        // Reset feedback selection
        feedbackToggleGroup.clearChecked();
        selectedFeedback = null;
    }
    
    private int getColorForWasteType(String wasteType) {
        if (wasteType.equalsIgnoreCase("ORGANIK")) {
            return Color.parseColor("#4CAF50"); // Green
        } else if (wasteType.equalsIgnoreCase("ANORGANIK")) {
            return Color.parseColor("#2196F3"); // Blue
        } else if (wasteType.equalsIgnoreCase("B3")) {
            return Color.parseColor("#F44336"); // Red
        } else {
            return Color.parseColor("#9E9E9E"); // Gray for unknown
        }
    }
    
    private String getTitleForWasteType(String wasteType) {
        if (wasteType.equalsIgnoreCase("ORGANIK")) {
            return "Sampah Organik";
        } else if (wasteType.equalsIgnoreCase("ANORGANIK")) {
            return "Sampah Anorganik";
        } else if (wasteType.equalsIgnoreCase("B3")) {
            return "Sampah B3 (Bahan Berbahaya & Beracun)";
        } else {
            return "Jenis Sampah Tidak Diketahui";
        }
    }
    
    private String getTipsForWasteType(String wasteType) {
        if (wasteType.equalsIgnoreCase("ORGANIK")) {
            return "• Buat kompos di rumah dengan sampah dapur dan kebun\n" +
                   "• Gunakan sampah organik sebagai pupuk tanaman\n" +
                   "• Simpan dalam wadah tertutup untuk mencegah bau dan serangga";
        } else if (wasteType.equalsIgnoreCase("ANORGANIK")) {
            return "• Pisahkan berdasarkan jenis material (plastik, kertas, logam, kaca)\n" +
                   "• Bersihkan wadah sebelum didaur ulang\n" +
                   "• Manfaatkan kembali botol atau wadah untuk keperluan lain";
        } else if (wasteType.equalsIgnoreCase("B3")) {
            return "• Jangan campur dengan sampah lain\n" +
                   "• Simpan dalam wadah khusus dan tertutup\n" +
                   "• Serahkan ke pusat pengolahan limbah B3 terdekat\n" +
                   "• Jangan buang ke saluran air atau tanah";
        } else {
            return "Tidak ada tips tersedia untuk jenis sampah ini.";
        }
    }
    
    private void shareResults() {
        if (selectedImage != null && classificationResult != null) {
            try {
                // Create a temporary file to share
                File cachePath = new File(requireContext().getCacheDir(), "images");
                cachePath.mkdirs();
                File imageFile = new File(cachePath, "shared_image.jpg");
                
                FileOutputStream stream = new FileOutputStream(imageFile);
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                stream.close();
                
                Uri contentUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider", imageFile);
                
                // Create share intent
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                
                // Add classification result text
                String shareText = "Hasil klasifikasi sampah:\n" +
                        "Jenis: " + getTitleForWasteType(classificationResult) + "\n\n" +
                        "Deskripsi: " + classificationDescription + "\n\n" +
                        "Tips: " + getTipsForWasteType(classificationResult) + "\n\n" +
                        "Klasifikasi oleh aplikasi EcoSortify";
                
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                // Start sharing activity
                startActivity(Intent.createChooser(shareIntent, "Bagikan hasil klasifikasi"));
                
            } catch (IOException e) {
                Log.e(TAG, "Error sharing image", e);
                Toast.makeText(requireContext(), 
                        "Gagal membagikan hasil", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitFeedback(String originalClassification, String correctedClassification, Bitmap image) {
        // Save image to local storage
        String imagePath = saveFeedbackImage(image);
          if (imagePath != null) {
            // Create feedback object
            ClassificationFeedback feedback = new ClassificationFeedback(
                    originalClassification,
                    correctedClassification,
                    imagePath,
                    "user@example.com" // Using placeholder for userEmail
            );
              // Save to database in background thread
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(requireContext());
                    // For now, just save feedback locally or log it                    // TODO: Add feedbackDao() to AppDatabase when available
                    
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Terima kasih atas feedback Anda", Toast.LENGTH_SHORT).show();
                        feedbackContainer.setVisibility(View.GONE);
                    });
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Feedback disimpan secara lokal", Toast.LENGTH_SHORT).show();
                        feedbackContainer.setVisibility(View.GONE);
                    });
                }
            }).start();
        } else {
            Toast.makeText(requireContext(), "Gagal menyimpan feedback", Toast.LENGTH_SHORT).show();
        }
    }

    private String saveFeedbackImage(Bitmap image) {
        try {
            // Create directory for feedback images if it doesn't exist
            File dir = new File(requireContext().getFilesDir(), "feedback");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Create file for the image
            String fileName = "feedback_" + System.currentTimeMillis() + ".jpg";
            File file = new File(dir, fileName);
            
            // Save image to file
            FileOutputStream fos = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error saving feedback image", e);
            return null;
        }
    }

    /**
     * Clean up temporary image data and cache after classification
     */
    private void cleanupTemporaryImages() {
        try {
            // Clear selected image from memory
            if (selectedImage != null && !selectedImage.isRecycled()) {
                selectedImage.recycle();
                selectedImage = null;
                Log.d(TAG, "Selected image bitmap recycled");
            }
            
            // Clear photo URI
            photoUri = null;
            
            // Clear image preview to free memory
            if (imagePreview != null) {
                imagePreview.setImageDrawable(null);
            }
            
            // Clear any temporary files if they exist
            try {
                File cacheDir = requireContext().getCacheDir();
                File[] tempFiles = cacheDir.listFiles((dir, name) -> 
                    name.startsWith("JPEG_") || name.startsWith("cropped_") || name.startsWith("shared_image"));
                
                if (tempFiles != null) {
                    for (File tempFile : tempFiles) {
                        if (tempFile.exists() && tempFile.delete()) {
                            Log.d(TAG, "Temporary file deleted: " + tempFile.getName());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning temporary files: " + e.getMessage());
            }
            
            Log.d(TAG, "Temporary image cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up temporary images: " + e.getMessage());
        }
    }
      /**
     * Reset UI and cleanup for new classification
     */
    private void resetForNewClassification() {
        cleanupTemporaryImages();
        
        // Reset UI state
        showEmptyState();
        
        // Clear classification results
        classificationResult = null;
        classificationDescription = null;
    }@Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clean up temporary images and free memory
        cleanupTemporaryImages();
        
        if (classificationHelper != null) {
            classificationHelper.release();
        }
    }
}
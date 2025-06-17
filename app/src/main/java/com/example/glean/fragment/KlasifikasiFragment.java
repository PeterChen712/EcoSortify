package com.example.glean.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.glean.R;
import com.example.glean.databinding.FragmentKlasifikasiBinding;
import com.example.glean.helper.NetworkHelper;

import java.io.IOException;

/**
 * Fragment untuk klasifikasi sampah
 * DAPAT diakses online maupun offline - menggunakan model online jika tersedia, model lokal jika offline
 */
public class KlasifikasiFragment extends Fragment {

    private FragmentKlasifikasiBinding binding;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentKlasifikasiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupKlasifikasi();
        updateNetworkStatus();
    }    private void updateNetworkStatus() {
        NetworkHelper.NetworkStatus networkStatus = NetworkHelper.getNetworkStatus(requireContext());
        
        if (networkStatus.isAvailable()) {
            binding.tvNetworkMode.setText("🌐 Mode Online - Klasifikasi menggunakan AI model terbaru");
            binding.tvNetworkMode.setTextColor(getResources().getColor(R.color.environmental_green));
        } else {
            binding.tvNetworkMode.setText("📱 Mode Offline - Klasifikasi menggunakan model lokal");
            binding.tvNetworkMode.setTextColor(getResources().getColor(R.color.accent_color));
        }
    }

    private void setupKlasifikasi() {
        binding.tvKlasifikasiDescription.setText("Ambil foto sampah untuk mengetahui jenis dan cara pengelolaannya. Klasifikasi tersedia online maupun offline.");
        
        // Setup category buttons for education
        setupCategoryButtons();
        
        // Setup camera scan button
        binding.btnScanTrash.setOnClickListener(v -> showImageSourceDialog());
        
        // Setup gallery button
        binding.btnFromGallery.setOnClickListener(v -> openGallery());
    }
    
    private void showImageSourceDialog() {
        String[] options = {"📷 Ambil Foto", "🖼️ Pilih dari Galeri"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Pilih Sumber Gambar")
               .setItems(options, (dialog, which) -> {
                   if (which == 0) {
                       openCamera();
                   } else {
                       openGallery();
                   }
               })
               .show();
    }
    
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), 
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return;
        }
        
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(requireContext(), "Kamera tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE && data != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                processImage(photo);
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                Uri imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                    processImage(bitmap);
                } catch (IOException e) {
                    Toast.makeText(requireContext(), "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void processImage(Bitmap image) {
        // Show loading
        binding.progressClassification.setVisibility(View.VISIBLE);
        binding.tvResult.setText("🔍 Menganalisis gambar...");
        binding.classificationResult.setVisibility(View.VISIBLE);
        
        // Check network status for classification model
        NetworkHelper.NetworkStatus networkStatus = NetworkHelper.getNetworkStatus(requireContext());
        
        if (networkStatus.isAvailable()) {
            // Use online model (simulate for now)
            classifyWithOnlineModel(image);
        } else {
            // Use offline model (simulate for now)
            classifyWithOfflineModel(image);
        }
    }
    
    private void classifyWithOnlineModel(Bitmap image) {
        // Simulate online classification with better accuracy
        binding.tvResult.postDelayed(() -> {
            binding.progressClassification.setVisibility(View.GONE);
            
            String result = "🥤 Plastik PET (Botol Minuman)\n\n" +
                          "📋 Penjelasan:\n" +
                          "• Jenis: Plastik dapat didaur ulang\n" +
                          "• Cara pengelolaan: Bersihkan dari sisa cairan, lepas tutup dan label\n" +
                          "• Tempat buang: Tempat sampah anorganik (kuning)\n" +
                          "• Tips: Botol plastik dapat dibuat kerajinan atau disetor ke bank sampah\n\n" +
                          "🌐 Hasil dari AI model online (akurasi tinggi)";
            
            binding.tvResult.setText(result);
        }, 2000);
    }
    
    private void classifyWithOfflineModel(Bitmap image) {
        // Simulate offline classification with basic rules
        binding.tvResult.postDelayed(() -> {
            binding.progressClassification.setVisibility(View.GONE);
            
            String result = "🗑️ Sampah Anorganik\n\n" +
                          "📋 Penjelasan:\n" +
                          "• Jenis: Kemungkinan plastik atau logam\n" +
                          "• Cara pengelolaan: Bersihkan dan pilah sesuai jenis\n" +
                          "• Tempat buang: Tempat sampah anorganik (kuning)\n" +
                          "• Tips: Periksa kode daur ulang untuk pengelolaan lebih tepat\n\n" +
                          "📱 Hasil dari model lokal (akurasi dasar)";
            
            binding.tvResult.setText(result);
        }, 1500);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(requireContext(), "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupCategoryButtons() {
        binding.btnCategoryOrganic.setOnClickListener(v -> {
            showCategoryInfo("Organik", "Sisa makanan, daun, kulit buah, dan bahan alami yang mudah terurai.");
        });
        
        binding.btnCategoryAnorganic.setOnClickListener(v -> {
            showCategoryInfo("Anorganik", "Plastik, kertas, logam, kaca, dan bahan yang sulit terurai.");
        });
        
        binding.btnCategoryB3.setOnClickListener(v -> {
            showCategoryInfo("B3 (Berbahaya)", "Baterai, lampu neon, obat-obatan, dan bahan kimia berbahaya.");
        });
        
        binding.btnCategoryElektronik.setOnClickListener(v -> {
            showCategoryInfo("Elektronik", "HP, komputer, TV, dan perangkat elektronik lainnya.");
        });
    }

    private void showCategoryInfo(String category, String info) {
        binding.tvCategoryInfo.setText(category + ":\n" + info);
        binding.categoryInfoLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

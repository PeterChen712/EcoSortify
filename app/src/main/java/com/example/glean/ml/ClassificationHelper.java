package com.example.glean.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.example.glean.api.GeminiApi;
import com.example.glean.model.WasteClassification;

import java.io.IOException;

public class ClassificationHelper {
    private static final String TAG = "ClassificationHelper";
    
    private final Context context;
    private final WasteClassifierModel localModel;
    private final GeminiApi geminiApi;
    
    public interface ClassificationCallback {
        void onSuccess(String wasteType, String description, String tips, boolean isLocalModel);
        void onError(String error);
    }
    
    public ClassificationHelper(Context context) {
        this.context = context;
        
        // Initialize local model if possible
        WasteClassifierModel model = null;
        try {
            model = new WasteClassifierModel(context);
        } catch (IOException e) {
            Log.e(TAG, "Error loading ML model", e);
        }
        this.localModel = model;
        
        // Initialize Gemini API client
        this.geminiApi = new GeminiApi();
    }
    
    // Update the classifyImage method to handle the new ClassificationResult format
    public void classifyImage(Bitmap image, ClassificationCallback callback) {
        // Check if we have a local model available
        if (localModel != null) {
            try {
                // Try local model first
                WasteClassifierModel.ClassificationResult result = localModel.classify(image);
                String wasteType = result.wasteCategory; // Use the mapped category
                String originalLabel = result.wasteType; // Original waste type label
                
                // Check if we have high confidence
                if (result.isHighConfidence()) {
                    // Use local result
                    String description = getDescriptionForWasteType(wasteType, originalLabel);
                    String tips = getTipsForWasteType(wasteType);
                    callback.onSuccess(wasteType, description, tips, true);
                    return;
                }
                
                // If confidence is low and we have internet, fallback to Gemini
                if (isNetworkAvailable()) {
                    fallbackToGeminiApi(image, callback);
                } else {
                    // No internet, use local model result despite low confidence
                    String description = getDescriptionForWasteType(wasteType, originalLabel);
                    String tips = getTipsForWasteType(wasteType);
                    callback.onSuccess(wasteType, description, tips, true);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error with local model classification", e);
                if (isNetworkAvailable()) {
                    fallbackToGeminiApi(image, callback);
                } else {
                    callback.onError("Klasifikasi gagal: Model lokal mengalami kesalahan dan tidak ada koneksi internet");
                }
            }
        } else if (isNetworkAvailable()) {
            // No local model, use Gemini if we have internet
            fallbackToGeminiApi(image, callback);
        } else {
            // No local model and no internet
            callback.onError("Klasifikasi gagal: Tidak ada koneksi internet dan model lokal tidak tersedia");
        }
    }
    
    private void fallbackToGeminiApi(Bitmap image, ClassificationCallback callback) {
        geminiApi.classifyWasteImage(image, new GeminiApi.ClassificationCallback() {
            @Override
            public void onSuccess(String wasteType, String description) {
                String tips = getTipsForWasteType(wasteType);
                callback.onSuccess(wasteType, description, tips, false);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return false;
    }
    
    // Add a new method that includes the original label in the description
    private String getDescriptionForWasteType(String wasteType, String originalLabel) {
        String baseDescription = "";
        
        if (wasteType.equalsIgnoreCase("ORGANIK")) {
            baseDescription = "Sampah organik adalah sampah yang berasal dari makhluk hidup dan dapat terurai " +
                  "secara alami. ";
        } else if (wasteType.equalsIgnoreCase("ANORGANIK")) {
            baseDescription = "Sampah anorganik adalah sampah yang sulit atau tidak bisa terurai secara alami. ";
        } else if (wasteType.equalsIgnoreCase("B3")) {
            baseDescription = "Sampah B3 (Bahan Berbahaya dan Beracun) adalah sampah yang mengandung zat berbahaya " +
                  "atau beracun yang dapat membahayakan lingkungan dan kesehatan manusia. ";
        }
        
        return baseDescription + "Sampah ini terdeteksi sebagai \"" + originalLabel + "\".";
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
    
    public void release() {
        if (localModel != null) {
            localModel.close();
        }
    }
}
package com.example.glean.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    
    /**
     * Load ML model file
     */
    public static MappedByteBuffer loadMappedFile(Context context, String modelPath) throws IOException {
        try (FileInputStream fileInputStream = context.openFileInput(modelPath);
             FileChannel fileChannel = fileInputStream.getChannel()) {
            return fileChannel.map(
                    FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        }
    }
    
    /**
     * Load labels file
     */
    public static List<String> loadLabels(Context context, String labelsPath) throws IOException {
        List<String> labels = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelsPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        return labels;
    }
    
    /**
     * Copy ML model from assets to internal storage
     */
    public static void copyAssetToInternalStorage(Context context, String assetName, String fileName) throws IOException {
        try (java.io.InputStream is = context.getAssets().open(assetName);
             java.io.FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
    }
}
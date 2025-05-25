package com.example.glean.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MLHelper {
    private static final String TAG = "MLHelper";
    private static final String MODEL_PATH = "model.tflite";
    private static final String LABELS_PATH = "labels.txt";
    
    private final Interpreter tflite;
    private final List<String> labels;
    private final int imageSizeX;
    private final int imageSizeY;
    
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;
    
    private TensorImage inputImageBuffer;
    private final TensorBuffer outputProbabilityBuffer;
    private final TensorProcessor probabilityProcessor;
    
    public MLHelper(Context context) throws IOException {
        // Load model
        MappedByteBuffer tfliteModel = loadModelFile(context);
        tflite = new Interpreter(tfliteModel);
        
        // Get model input and output info
        imageSizeX = tflite.getInputTensor(0).shape()[1];
        imageSizeY = tflite.getInputTensor(0).shape()[2];
        
        // Load labels
        labels = loadLabels(context);
        
        // Create output buffer and processor
        outputProbabilityBuffer = TensorBuffer.createFixedSize(
                new int[]{1, labels.size()}, 
                org.tensorflow.lite.DataType.FLOAT32);
        
        // Create tensor processor for normalization
        TensorOperator normalizationOp = new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
        probabilityProcessor = new TensorProcessor.Builder().add(normalizationOp).build();
    }
    
    public List<Recognition> classifyImage(Bitmap bitmap) {
        // Verify the model is initialized
        if (tflite == null) {
            Log.e(TAG, "Interpreter not initialized");
            return Collections.emptyList();
        }
        
        // Preprocess the image
        inputImageBuffer = loadImage(bitmap);
        
        // Run inference
        long startTime = SystemClock.uptimeMillis();
        tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Inference time: " + (endTime - startTime) + "ms");
        
        // Post-process the result
        Map<String, Float> labeledProbability = new HashMap<>();
        float[] probabilities = outputProbabilityBuffer.getFloatArray();
        for (int i = 0; i < labels.size(); i++) {
            labeledProbability.put(labels.get(i), probabilities[i]);
        }
        
        // Get top results
        return getTopResults(labeledProbability);
    }
    
    private TensorImage loadImage(Bitmap bitmap) {
        // Prepare input
        TensorImage inputImage = new TensorImage(org.tensorflow.lite.DataType.FLOAT32);
        inputImage.load(bitmap);
        
        // Create image processor for resizing
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build();
        
        // Process the image
        return imageProcessor.process(inputImage);
    }
    
    private List<Recognition> getTopResults(Map<String, Float> labelProb) {
        // Get all results
        List<Recognition> recognitions = new ArrayList<>();
        for (Map.Entry<String, Float> entry : labelProb.entrySet()) {
            recognitions.add(new Recognition(entry.getKey(), entry.getValue()));
        }
        
        // Sort by confidence (descending)
        Collections.sort(recognitions, (o1, o2) -> Float.compare(o2.getConfidence(), o1.getConfidence()));
        
        // Return top 3 results (or less if there are fewer)
        return recognitions.subList(0, Math.min(recognitions.size(), 3));
    }
    
    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        try {
            // Try to load directly from assets
            return FileUtil.loadMappedFile(context, MODEL_PATH);
        } catch (IOException e) {
            Log.e(TAG, "Error loading model file from assets", e);
            throw e;
        }
    }
    
    private List<String> loadLabels(Context context) throws IOException {
        try {
            // Load labels from file
            return FileUtil.loadLabels(context, LABELS_PATH);
        } catch (IOException e) {
            Log.e(TAG, "Error loading labels file", e);
            throw e;
        }
    }
    
    // Class to store recognition results
    public static class Recognition implements Comparable<Recognition> {
        private final String label;
        private final Float confidence;
        
        public Recognition(String label, Float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
        
        public String getLabel() {
            return label;
        }
        
        public Float getConfidence() {
            return confidence;
        }
        
        @Override
        public int compareTo(Recognition other) {
            // Sort in descending order of confidence
            return other.confidence.compareTo(this.confidence);
        }
        
        @Override
        public String toString() {
            return "Recognition{" +
                    "label='" + label + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }
    
    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
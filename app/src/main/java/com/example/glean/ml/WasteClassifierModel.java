package com.example.glean.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WasteClassifierModel {
    private static final String TAG = "WasteClassifierModel";
    
    private static final String MODEL_PATH = "model/waste_classifier_model.tflite";
    private static final String LABEL_PATH = "model/waste_labels.txt";
    
    private static final int IMAGE_SIZE = 224; // Common input size for models
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;
    private static final float CONFIDENCE_THRESHOLD = 0.8f; // Threshold for high confidence
    
    private Interpreter tflite;
    private List<String> labels;
    private TensorImage inputImageBuffer;
    private TensorBuffer outputBuffer;
    private TensorProcessor probabilityProcessor;
    private Map<String, String> wasteGroupMap;
    
    public static class ClassificationResult {
        public final String wasteType;        // Original label like "plastic", "paper"
        public final String wasteCategory;    // Mapped category: "ORGANIK", "ANORGANIK", "B3"
        public final float confidence;
        
        public ClassificationResult(String wasteType, String wasteCategory, float confidence) {
            this.wasteType = wasteType;
            this.wasteCategory = wasteCategory;
            this.confidence = confidence;
        }
        
        public boolean isHighConfidence() {
            return confidence >= CONFIDENCE_THRESHOLD;
        }
    }
    
    public WasteClassifierModel(Context context) throws IOException {
        // Load model
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, MODEL_PATH);
        Interpreter.Options options = new Interpreter.Options();
        tflite = new Interpreter(modelFile, options);
        
        // Load labels
        labels = FileUtil.loadLabels(context, LABEL_PATH);
        
        // Setup input and output buffers
        int[] inputShape = tflite.getInputTensor(0).shape();
        DataType inputDataType = tflite.getInputTensor(0).dataType();
        inputImageBuffer = new TensorImage(inputDataType);
        
        int[] outputShape = tflite.getOutputTensor(0).shape();
        DataType outputDataType = tflite.getOutputTensor(0).dataType();
        outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType);
        
        // Validate model output matches labels
        int expectedClasses = outputShape[outputShape.length - 1]; // Last dimension is number of classes
        if (labels.size() != expectedClasses) {
            Log.w(TAG, "Label count (" + labels.size() + ") doesn't match model output (" + expectedClasses + ")");
            // Adjust labels list to match model output
            if (labels.size() > expectedClasses) {
                labels = labels.subList(0, expectedClasses);
            } else {
                // Add placeholder labels if needed
                while (labels.size() < expectedClasses) {
                    labels.add("unknown_" + labels.size());
                }
            }
        }
        
        // Setup normalization
        TensorOperator normalizationOp = new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
        probabilityProcessor = new TensorProcessor.Builder().add(normalizationOp).build();
        
        // Initialize waste type to category mapping
        initializeWasteGroupMap();
    }

    private void initializeWasteGroupMap() {
        wasteGroupMap = new HashMap<>();
        
        // Anorganik items
        wasteGroupMap.put("paper", "ANORGANIK");
        wasteGroupMap.put("cardboard", "ANORGANIK");
        wasteGroupMap.put("plastic", "ANORGANIK");
        wasteGroupMap.put("metal", "ANORGANIK");
        wasteGroupMap.put("trash", "ANORGANIK");
        wasteGroupMap.put("shoes", "ANORGANIK");
        wasteGroupMap.put("clothes", "ANORGANIK");
        wasteGroupMap.put("green-glass", "ANORGANIK");
        wasteGroupMap.put("brown-glass", "ANORGANIK");
        wasteGroupMap.put("white-glass", "ANORGANIK");
        wasteGroupMap.put("glass", "ANORGANIK");
        
        // Organik items
        wasteGroupMap.put("biological", "ORGANIK");
        wasteGroupMap.put("food", "ORGANIK");
        wasteGroupMap.put("fruit", "ORGANIK");
        wasteGroupMap.put("vegetable", "ORGANIK");
        wasteGroupMap.put("leaves", "ORGANIK");
        wasteGroupMap.put("wood", "ORGANIK");
        wasteGroupMap.put("organic", "ORGANIK");
        
        // B3 items
        wasteGroupMap.put("battery", "B3");
        wasteGroupMap.put("electronic", "B3");
        wasteGroupMap.put("hazardous", "B3");
        wasteGroupMap.put("toxic", "B3");
        wasteGroupMap.put("medical", "B3");
    }

    public ClassificationResult classify(Bitmap bitmap) {
        try {
            // Preprocess image
            inputImageBuffer = loadImage(bitmap);
            
            // Run inference
            tflite.run(inputImageBuffer.getBuffer(), outputBuffer.getBuffer());
            
            // Post-process results manually to avoid TensorLabel issues
            float[] probabilities = outputBuffer.getFloatArray();
            
            // Find the class with highest probability
            int maxIndex = 0;
            float maxProbability = probabilities[0];
            
            for (int i = 1; i < probabilities.length; i++) {
                if (probabilities[i] > maxProbability) {
                    maxProbability = probabilities[i];
                    maxIndex = i;
                }
            }
            
            // Get predicted label safely
            String predictedLabel = "unknown";
            if (maxIndex < labels.size()) {
                predictedLabel = labels.get(maxIndex);
            }
            
            // Map to waste category
            String wasteCategory = mapToWasteCategory(predictedLabel);
            
            return new ClassificationResult(predictedLabel, wasteCategory, maxProbability);
        } catch (Exception e) {
            Log.e(TAG, "Error during classification", e);
            // Return default result
            return new ClassificationResult("unknown", "ANORGANIK", 0.0f);
        }
    }
    
    // Map specific waste label to broader category
    public String mapToWasteCategory(String wasteLabel) {
        // Case insensitive lookup in the map
        for (Map.Entry<String, String> entry : wasteGroupMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(wasteLabel)) {
                return entry.getValue();
            }
        }
        
        // Default to ANORGANIK for unknown types (most common)
        Log.d(TAG, "Unknown waste type: " + wasteLabel + ", defaulting to ANORGANIK");
        return "ANORGANIK";
    }
    
    private TensorImage loadImage(Bitmap bitmap) {
        // Load bitmap into TensorImage
        inputImageBuffer.load(bitmap);
        
        // Create image processor for resizing
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build();
        
        return imageProcessor.process(inputImageBuffer);
    }
    
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}
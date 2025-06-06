package com.example.glean.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.glean.config.ApiConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class GeminiHelper {
    
    private static final String TAG = "GeminiHelper";
    private static final String MODEL_NAME = "gemini-1.5-flash";
    
    private final GenerativeModelFutures model;
    private final Context context;
    
    public interface ClassificationCallback {
        void onSuccess(String trashType, float confidence, String description);
        void onError(String error);
    }
    
    public GeminiHelper(Context context) {
        this.context = context;
        
        // Check if API key is configured
        if (!ApiConfig.isGeminiApiConfigured()) {
            Log.w(TAG, "Gemini API key not configured in gradle.properties");
            throw new IllegalStateException("Gemini API key not configured. Please add GEMINI_API_KEY to gradle.properties");
        }
        
        // Initialize Gemini model
        GenerativeModel gm = new GenerativeModel(
            MODEL_NAME,
            ApiConfig.getGeminiApiKey()
        );
        
        this.model = GenerativeModelFutures.from(gm);
        Log.d(TAG, "GeminiHelper initialized with model: " + MODEL_NAME);
    }
    
    public void classifyTrash(Bitmap bitmap, ClassificationCallback callback) {
        if (bitmap == null) {
            callback.onError("Invalid image provided");
            return;
        }
        
        try {
            // Create prompt for trash classification
            String prompt = createTrashClassificationPrompt();
            
            // Convert bitmap to content
            Content content = new Content.Builder()
                .addText(prompt)
                .addImage(bitmap)
                .build();
            
            // Generate content
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    try {
                        String responseText = result.getText();
                        Log.d(TAG, "Gemini response: " + responseText);
                        
                        // Parse response
                        parseClassificationResult(responseText, callback);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Gemini response", e);
                        callback.onError("Failed to process classification result");
                    }
                }
                
                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Gemini API call failed", t);
                    callback.onError("Classification failed: " + t.getMessage());
                }
            }, context.getMainExecutor());
            
        } catch (Exception e) {
            Log.e(TAG, "Error calling Gemini API", e);
            callback.onError("Failed to classify image: " + e.getMessage());
        }
    }
    
    private String createTrashClassificationPrompt() {
        return "Analyze this image and classify the type of trash/waste visible. " +
               "Respond ONLY with a JSON object in this exact format:\n" +
               "{\n" +
               "  \"trash_type\": \"one of: plastic, paper, glass, metal, organic, electronic, hazardous, cigarette_butt, other\",\n" +
               "  \"confidence\": 0.95,\n" +
               "  \"description\": \"Brief description of what you see\"\n" +
               "}\n\n" +
               "Rules:\n" +
               "- Use only the specified trash_type categories\n" +
               "- Confidence should be between 0.0 and 1.0\n" +
               "- Description should be 1-2 sentences\n" +
               "- If no trash is visible, use \"other\" with low confidence\n" +
               "- Only return the JSON, no other text";
    }
    
    private void parseClassificationResult(String responseText, ClassificationCallback callback) {
        try {
            // Clean the response text
            String cleanJson = extractJsonFromResponse(responseText);
            
            // Parse JSON
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(cleanJson, JsonObject.class);
            
            String trashType = jsonObject.get("trash_type").getAsString();
            float confidence = jsonObject.get("confidence").getAsFloat();
            String description = jsonObject.get("description").getAsString();
            
            // Validate trash type
            trashType = validateTrashType(trashType);
            
            // Ensure confidence is in valid range
            confidence = Math.max(0.0f, Math.min(1.0f, confidence));
            
            Log.d(TAG, String.format("Parsed result - Type: %s, Confidence: %.2f, Description: %s", 
                  trashType, confidence, description));
            
            callback.onSuccess(trashType, confidence, description);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing classification result: " + responseText, e);
            
            // Fallback parsing
            String trashType = extractTrashTypeFromText(responseText);
            float confidence = 0.5f; // Default confidence
            String description = "Classification result processed with fallback method";
            
            callback.onSuccess(trashType, confidence, description);
        }
    }
    
    private String extractJsonFromResponse(String responseText) {
        // Find JSON object in response
        int startIndex = responseText.indexOf("{");
        int endIndex = responseText.lastIndexOf("}");
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return responseText.substring(startIndex, endIndex + 1);
        }
        
        // If no JSON found, create a default one
        return createDefaultJson(responseText);
    }
    
    private String createDefaultJson(String responseText) {
        String trashType = extractTrashTypeFromText(responseText);
        return String.format(
            "{\"trash_type\":\"%s\",\"confidence\":0.7,\"description\":\"Classified from text analysis\"}", 
            trashType
        );
    }
    
    private String extractTrashTypeFromText(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("plastic") || lowerText.contains("bottle") || lowerText.contains("bag")) {
            return "plastic";
        } else if (lowerText.contains("paper") || lowerText.contains("cardboard")) {
            return "paper";
        } else if (lowerText.contains("glass") || lowerText.contains("jar")) {
            return "glass";
        } else if (lowerText.contains("metal") || lowerText.contains("can") || lowerText.contains("aluminum")) {
            return "metal";
        } else if (lowerText.contains("organic") || lowerText.contains("food") || lowerText.contains("fruit")) {
            return "organic";
        } else if (lowerText.contains("electronic") || lowerText.contains("battery") || lowerText.contains("phone")) {
            return "electronic";
        } else if (lowerText.contains("cigarette") || lowerText.contains("butt") || lowerText.contains("tobacco")) {
            return "cigarette_butt";
        } else if (lowerText.contains("hazardous") || lowerText.contains("chemical") || lowerText.contains("toxic")) {
            return "hazardous";
        } else {
            return "other";
        }
    }
    
    private String validateTrashType(String trashType) {
        String[] validTypes = {"plastic", "paper", "glass", "metal", "organic", "electronic", "hazardous", "cigarette_butt", "other"};
        
        for (String validType : validTypes) {
            if (validType.equals(trashType.toLowerCase())) {
                return validType;
            }
        }
        
        // If not found, try to map similar terms
        String lowerType = trashType.toLowerCase();
        if (lowerType.contains("plastic") || lowerType.contains("bottle")) return "plastic";
        if (lowerType.contains("paper") || lowerType.contains("cardboard")) return "paper";
        if (lowerType.contains("glass")) return "glass";
        if (lowerType.contains("metal") || lowerType.contains("aluminum")) return "metal";
        if (lowerType.contains("organic") || lowerType.contains("food")) return "organic";
        if (lowerType.contains("electronic") || lowerType.contains("battery")) return "electronic";
        if (lowerType.contains("cigarette")) return "cigarette_butt";
        if (lowerType.contains("hazardous") || lowerType.contains("chemical")) return "hazardous";
        
        return "other"; // Default fallback
    }
    
    public boolean isApiKeyConfigured() {
        return ApiConfig.isGeminiApiConfigured();
    }
}
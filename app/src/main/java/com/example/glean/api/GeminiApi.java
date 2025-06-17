package com.example.glean.api;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.example.glean.config.ApiConfig;
import com.example.glean.model.ChatRequest;
import com.example.glean.model.ChatResponse;
import com.example.glean.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiApi {
    private static final String TAG = "GeminiApi";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private OkHttpClient client;
    private Gson gson;
    
    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public interface ClassificationCallback {
        void onSuccess(String wasteType, String description);
        void onError(String error);
    }
    
    public GeminiApi() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }
    
    /**
     * Send message with conversation history to maintain context
     */
    public void sendMessageWithHistory(String message, List<ChatMessage> conversationHistory, GeminiCallback callback) {
        try {
            // Create request with conversation history
            ChatRequest chatRequest = createChatRequestWithHistory(message, conversationHistory);
            
            // Convert to JSON
            String requestJson = gson.toJson(chatRequest);
            
            RequestBody body = RequestBody.create(requestJson, JSON);
            
            Request request = new Request.Builder()
                    .url(ApiConfig.getGeminiProEndpoint())
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            Log.d(TAG, "Sending request with history to: " + ApiConfig.getGeminiProEndpoint());
            Log.d(TAG, "Request body: " + requestJson);
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Request failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response code: " + response.code());
                    Log.d(TAG, "Response body: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        try {
                            String extractedText = parseResponse(responseBody);
                            callback.onSuccess(extractedText);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing response", e);
                            callback.onError("Error parsing response: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Unexpected response code: " + response);
                        
                        try {
                            com.google.gson.JsonObject errorJson = gson.fromJson(responseBody, com.google.gson.JsonObject.class);
                            String errorMessage = "API Error";
                            
                            if (errorJson.has("error")) {
                                com.google.gson.JsonObject error = errorJson.getAsJsonObject("error");
                                if (error.has("message")) {
                                    errorMessage = error.get("message").getAsString();
                                }
                            }
                            
                            callback.onError("HTTP " + response.code() + ": " + errorMessage);
                        } catch (Exception e) {
                            callback.onError("HTTP " + response.code() + ": " + responseBody);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating request", e);
            callback.onError("Error creating request: " + e.getMessage());
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public void sendMessage(String message, GeminiCallback callback) {
        sendMessageWithHistory(message, new ArrayList<>(), callback);
    }
    
    /**
     * Create ChatRequest with conversation history
     */
    private ChatRequest createChatRequestWithHistory(String currentMessage, List<ChatMessage> conversationHistory) {
        List<ChatRequest.Content> contents = new ArrayList<>();
        
        // Add system prompt first
        String systemPrompt = createEnvironmentalContext("");
        ChatRequest.Part systemPart = new ChatRequest.Part(systemPrompt);
        ChatRequest.Content systemContent = new ChatRequest.Content("user", systemPart);
        contents.add(systemContent);
        
        // Add system response to acknowledge the role
        ChatRequest.Part systemResponsePart = new ChatRequest.Part("Baik, saya siap membantu sebagai EcoBot untuk pertanyaan lingkungan dan pengelolaan sampah.");
        ChatRequest.Content systemResponseContent = new ChatRequest.Content("model", systemResponsePart);
        contents.add(systemResponseContent);
        
        // Add conversation history (exclude typing indicators and system messages)
        for (ChatMessage historyMessage : conversationHistory) {
            if (!historyMessage.isTypingIndicator() && !isSystemMessage(historyMessage.getMessage())) {
                String role = historyMessage.isUser() ? "user" : "model";
                ChatRequest.Part part = new ChatRequest.Part(historyMessage.getMessage());
                ChatRequest.Content content = new ChatRequest.Content(role, part);
                contents.add(content);
            }
        }
        
        // Add current message
        ChatRequest.Part currentPart = new ChatRequest.Part(currentMessage);
        ChatRequest.Content currentContent = new ChatRequest.Content("user", currentPart);
        contents.add(currentContent);
        
        return new ChatRequest(contents);
    }
    
    /**
     * Check if message is a system message (welcome message, etc.)
     */
    private boolean isSystemMessage(String message) {
        return message.contains("🌱 Halo! Saya EcoBot") || 
               message.contains("😕 Maaf, terjadi kesalahan") ||
               message.contains("🤖 Sedang mengetik");
    }
    
    /**
     * Classify waste image using Gemini Vision API
     */
    public void classifyWasteImage(Bitmap image, ClassificationCallback callback) {
        try {
            String base64Image = bitmapToBase64(image);
            
            String prompt = "Analisis gambar sampah ini dan berikan klasifikasi yang tepat. " +
                    "Tentukan apakah sampah ini termasuk kategori:\n" +
                    "1. ORGANIK (sampah yang dapat terurai secara alami)\n" +
                    "2. ANORGANIK (sampah yang sulit terurai seperti plastik, logam, kaca)\n" +
                    "3. B3 (Bahan Berbahaya dan Beracun seperti baterai, elektronik)\n\n" +
                    "Format jawaban:\n" +
                    "Kategori: [ORGANIK/ANORGANIK/B3]\n" +
                    "Deskripsi: [penjelasan singkat tentang jenis sampah dan mengapa masuk kategori tersebut]";
            
            ChatRequest visionRequest = createVisionRequest(base64Image, prompt);
            String requestJson = gson.toJson(visionRequest);
            
            RequestBody body = RequestBody.create(requestJson, JSON);
            
            Request request = new Request.Builder()
                    .url(ApiConfig.getGeminiProVisionEndpoint())
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            Log.d(TAG, "Sending vision request to: " + ApiConfig.getGeminiProVisionEndpoint());
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Vision request failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Vision response code: " + response.code());
                    Log.d(TAG, "Vision response body: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        try {
                            String extractedText = parseResponse(responseBody);
                            parseClassificationResponse(extractedText, callback);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing vision response", e);
                            callback.onError("Error parsing response: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Vision request failed: " + response);
                        callback.onError("HTTP " + response.code() + ": " + responseBody);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating vision request", e);
            callback.onError("Error creating request: " + e.getMessage());
        }
    }
    
    /**
     * Convert bitmap to base64 string
     */
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
    
    /**
     * Create vision request with image and text
     */
    private ChatRequest createVisionRequest(String base64Image, String text) {
        List<ChatRequest.Part> parts = new ArrayList<>();
        
        // Add text part
        parts.add(new ChatRequest.Part(text));
        
        // Add image part
        ChatRequest.Part imagePart = new ChatRequest.Part();
        ChatRequest.InlineData inlineData = new ChatRequest.InlineData("image/jpeg", base64Image);
        imagePart.setInlineData(inlineData);
        parts.add(imagePart);
        
        ChatRequest.Content content = new ChatRequest.Content("user", parts);
        
        List<ChatRequest.Content> contents = new ArrayList<>();
        contents.add(content);
        
        return new ChatRequest(contents);
    }
    
    /**
     * Parse classification response and extract waste type and description
     */
    private void parseClassificationResponse(String response, ClassificationCallback callback) {
        try {
            String wasteType = "ANORGANIK"; // Default
            String description = response;
            
            if (response.toLowerCase().contains("organik") && !response.toLowerCase().contains("anorganik")) {
                wasteType = "ORGANIK";
            } else if (response.toLowerCase().contains("b3") || response.toLowerCase().contains("berbahaya")) {
                wasteType = "B3";
            } else if (response.toLowerCase().contains("anorganik")) {
                wasteType = "ANORGANIK";
            }
            
            if (response.contains("Deskripsi:")) {
                String[] parts = response.split("Deskripsi:", 2);
                if (parts.length > 1) {
                    description = parts[1].trim();
                }
            }
            
            callback.onSuccess(wasteType, description);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing classification response", e);
            callback.onError("Error parsing classification result");
        }
    }
    
    /**
     * Parse response using model classes
     */
    private String parseResponse(String responseBody) {
        try {
            ChatResponse chatResponse = gson.fromJson(responseBody, ChatResponse.class);
            
            if (chatResponse.getCandidates() != null && !chatResponse.getCandidates().isEmpty()) {
                ChatResponse.Candidate candidate = chatResponse.getCandidates().get(0);
                
                if (candidate.getContent() != null && 
                    candidate.getContent().getParts() != null && 
                    !candidate.getContent().getParts().isEmpty()) {
                    
                    ChatResponse.Part part = candidate.getContent().getParts().get(0);
                    
                    if (part.getText() != null && !part.getText().trim().isEmpty()) {
                        return part.getText().trim();
                    }
                }
            }
            
            return "Maaf, tidak ada respons yang valid dari AI.";
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing with Gson", e);
            return "Error parsing response: " + e.getMessage();
        }
    }
    
    /**
     * Create environmental context for the message
     */
    private String createEnvironmentalContext(String userMessage) {
        return "Anda adalah EcoBot, asisten AI yang ahli dalam bidang lingkungan, pengelolaan sampah, dan keberlanjutan. " +
                "Berikan jawaban yang informatif, praktis, dan ramah lingkungan dalam bahasa Indonesia. " +
                "Fokus pada solusi yang dapat diterapkan dalam kehidupan sehari-hari. " +
                "Jawab dengan bahasa yang mudah dipahami dan berikan contoh konkret jika memungkinkan. " +
                "Ingat informasi penting yang diberikan user dalam percakapan sebelumnya seperti nama, lokasi, atau preferensi mereka.";
    }
}
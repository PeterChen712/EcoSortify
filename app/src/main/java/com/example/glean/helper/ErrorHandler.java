package com.example.glean.helper;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.StringRes;

/**
 * Error handling helper for consistent error management
 * Based on EcoSortify error handling patterns
 */
public class ErrorHandler {
    
    /**
     * Handle API errors with user-friendly messages
     */
    public static String getApiErrorMessage(String apiError) {
        if (apiError == null || apiError.isEmpty()) {
            return "Terjadi kesalahan yang tidak diketahui";
        }
        
        // Common API error patterns
        if (apiError.contains("401") || apiError.contains("Unauthorized")) {
            return "🔑 API key tidak valid. Silakan periksa konfigurasi.";
        } else if (apiError.contains("403") || apiError.contains("Forbidden")) {
            return "🚫 Akses ditolak. Periksa izin API key.";
        } else if (apiError.contains("429") || apiError.contains("Too Many Requests")) {
            return "⏰ Terlalu banyak permintaan. Coba lagi dalam beberapa menit.";
        } else if (apiError.contains("500") || apiError.contains("Internal Server Error")) {
            return "🛠️ Server sedang bermasalah. Coba lagi nanti.";
        } else if (apiError.contains("timeout") || apiError.contains("Timeout")) {
            return "⏱️ Koneksi timeout. Periksa koneksi internet Anda.";
        } else if (apiError.contains("Network") || apiError.contains("network")) {
            return "📶 Masalah jaringan. Periksa koneksi internet.";
        } else if (apiError.contains("JSON") || apiError.contains("parse")) {
            return "📄 Format data tidak valid dari server.";
        } else if (apiError.contains("SSL") || apiError.contains("certificate")) {
            return "🔒 Masalah keamanan koneksi.";
        }
        
        // Return cleaned error message
        return "❌ " + cleanErrorMessage(apiError);
    }
    
    /**
     * Handle network errors
     */
    public static String getNetworkErrorMessage(Exception e) {
        if (e == null) {
            return "Masalah jaringan tidak diketahui";
        }
        
        String message = e.getMessage();
        if (message == null) {
            return "Masalah koneksi jaringan";
        }
        
        if (message.contains("timeout")) {
            return "⏱️ Koneksi timeout. Coba lagi.";
        } else if (message.contains("resolve")) {
            return "🌐 Tidak dapat menghubungi server.";
        } else if (message.contains("refused")) {
            return "🚫 Koneksi ditolak server.";
        } else if (message.contains("unreachable")) {
            return "📡 Server tidak dapat dijangkau.";
        }
        
        return "📶 Masalah jaringan: " + cleanErrorMessage(message);
    }
    
    /**
     * Handle database errors
     */
    public static String getDatabaseErrorMessage(Exception e) {
        if (e == null) {
            return "Masalah database tidak diketahui";
        }
        
        String message = e.getMessage();
        if (message == null) {
            return "Kesalahan database";
        }
        
        if (message.contains("UNIQUE constraint")) {
            return "💾 Data sudah ada di database.";
        } else if (message.contains("NOT NULL constraint")) {
            return "💾 Data tidak lengkap untuk disimpan.";
        } else if (message.contains("disk") || message.contains("space")) {
            return "💾 Ruang penyimpanan tidak cukup.";
        } else if (message.contains("lock") || message.contains("busy")) {
            return "💾 Database sedang sibuk. Coba lagi.";
        }
        
        return "💾 Masalah database: " + cleanErrorMessage(message);
    }
    
    /**
     * Handle cache errors
     */
    public static String getCacheErrorMessage(Exception e) {
        if (e == null) {
            return "Masalah cache tidak diketahui";
        }
        
        String message = e.getMessage();
        if (message == null) {
            return "Kesalahan cache";
        }
        
        if (message.contains("space") || message.contains("storage")) {
            return "📚 Ruang cache tidak cukup.";
        } else if (message.contains("permission")) {
            return "📚 Tidak ada izin akses cache.";
        } else if (message.contains("corrupt")) {
            return "📚 Cache rusak. Akan dibersihkan otomatis.";
        }
        
        return "📚 Masalah cache: " + cleanErrorMessage(message);
    }
    
    /**
     * Clean error message for display
     */
    private static String cleanErrorMessage(String message) {
        if (message == null) return "";
        
        // Remove technical details
        message = message.replaceAll("java\\..*?:", "");
        message = message.replaceAll("android\\..*?:", "");
        message = message.replaceAll("com\\..*?:", "");
        message = message.replaceAll("org\\..*?:", "");
        
        // Remove stack trace indicators
        message = message.replaceAll("\\s*at .*", "");
        message = message.replaceAll("Caused by:.*", "");
        
        // Limit length
        if (message.length() > 100) {
            message = message.substring(0, 97) + "...";
        }
        
        return message.trim();
    }
    
    /**
     * Show error toast with appropriate icon
     */
    public static void showErrorToast(Context context, String message) {
        if (context != null && message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Show error toast for exceptions
     */
    public static void showErrorToast(Context context, Exception e) {
        if (context != null && e != null) {
            String message = getNetworkErrorMessage(e);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Log error with consistent format
     */
    public static void logError(String tag, String operation, Exception e) {
        if (e != null) {
            android.util.Log.e(tag, operation + " failed: " + e.getMessage(), e);
        } else {
            android.util.Log.e(tag, operation + " failed: Unknown error");
        }
    }
    
    /**
     * Get fallback action message
     */
    public static String getFallbackMessage(String primaryAction, String fallbackAction) {
        return String.format("❌ %s gagal. 🔄 %s sebagai alternatif.", primaryAction, fallbackAction);
    }
    
    /**
     * Error categories for different handling
     */
    public enum ErrorCategory {
        NETWORK,
        API,
        DATABASE,
        CACHE,
        VALIDATION,
        PERMISSION,
        UNKNOWN
    }
    
    /**
     * Categorize error for appropriate handling
     */
    public static ErrorCategory categorizeError(Exception e) {
        if (e == null) return ErrorCategory.UNKNOWN;
        
        String message = e.getMessage();
        String className = e.getClass().getSimpleName();
        
        if (className.contains("Network") || className.contains("Socket") || 
            className.contains("Connect") || className.contains("Timeout")) {
            return ErrorCategory.NETWORK;
        } else if (className.contains("HTTP") || className.contains("JSON") ||
                  (message != null && (message.contains("401") || message.contains("403") || message.contains("500")))) {
            return ErrorCategory.API;
        } else if (className.contains("SQL") || className.contains("Database") ||
                  className.contains("Room")) {
            return ErrorCategory.DATABASE;
        } else if (message != null && (message.contains("cache") || message.contains("storage"))) {
            return ErrorCategory.CACHE;
        } else if (className.contains("Security") || className.contains("Permission")) {
            return ErrorCategory.PERMISSION;
        }
        
        return ErrorCategory.UNKNOWN;
    }
    
    /**
     * Get error message by category
     */
    public static String getErrorMessageByCategory(ErrorCategory category, Exception e) {
        switch (category) {
            case NETWORK:
                return getNetworkErrorMessage(e);
            case API:
                return getApiErrorMessage(e != null ? e.getMessage() : null);
            case DATABASE:
                return getDatabaseErrorMessage(e);
            case CACHE:
                return getCacheErrorMessage(e);
            case VALIDATION:
                return "✅ Data tidak valid: " + (e != null ? cleanErrorMessage(e.getMessage()) : "");
            case PERMISSION:
                return "🔒 Izin diperlukan: " + (e != null ? cleanErrorMessage(e.getMessage()) : "");
            default:
                return "❓ Kesalahan tidak diketahui: " + (e != null ? cleanErrorMessage(e.getMessage()) : "");
        }
    }
}

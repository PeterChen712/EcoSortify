package com.example.glean.helper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * Network Helper for detecting internet connectivity
 * Used for offline/online functionality in News feature
 */
public class NetworkHelper {
    private static final String TAG = "NetworkHelper";
    
    /**
     * Check if device has internet connectivity
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For API 23+
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            // For API < 23
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    /**
     * Check if device has WiFi connectivity
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && 
                   networkInfo.isConnected() && 
                   networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
    }
    
    /**
     * Check if device has cellular connectivity
     */
    public static boolean isCellularConnected(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && 
                   networkInfo.isConnected() && 
                   networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        }
    }
    
    /**
     * Get network type as string for debugging
     */
    public static String getNetworkType(Context context) {
        if (!isNetworkAvailable(context)) {
            return "No Connection";
        }
        
        if (isWifiConnected(context)) {
            return "WiFi";
        } else if (isCellularConnected(context)) {
            return "Cellular";
        } else {
            return "Other";
        }
    }
    
    /**
     * Check if network is metered (cellular data with potential charges)
     */
    public static boolean isNetworkMetered(Context context) {
        if (context == null) {
            return true; // Assume metered if we can't check
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return true;
        }
        
        return connectivityManager.isActiveNetworkMetered();
    }
    
    /**
     * Should we fetch news based on network conditions?
     * Only fetch on WiFi if user is on cellular to save data
     */
    public static boolean shouldFetchNews(Context context) {
        if (!isNetworkAvailable(context)) {
            Log.d(TAG, "No network available - should not fetch news");
            return false;
        }
        
        if (isWifiConnected(context)) {
            Log.d(TAG, "WiFi connected - safe to fetch news");
            return true;
        }
        
        if (isCellularConnected(context) && !isNetworkMetered(context)) {
            Log.d(TAG, "Cellular connected and not metered - safe to fetch news");
            return true;
        }
        
        // On metered cellular, let user decide
        Log.d(TAG, "Metered cellular connection - let user decide");
        return true; // In this implementation, we allow it but could add user preference
    }
    
    /**
     * Get network status for display
     */
    public static NetworkStatus getNetworkStatus(Context context) {
        boolean isAvailable = isNetworkAvailable(context);
        String type = getNetworkType(context);
        boolean isMetered = isNetworkMetered(context);
        boolean shouldFetch = shouldFetchNews(context);
        
        return new NetworkStatus(isAvailable, type, isMetered, shouldFetch);
    }
    
    /**
     * Network status class for bundling network information
     */
    public static class NetworkStatus {
        private final boolean isAvailable;
        private final String type;
        private final boolean isMetered;
        private final boolean shouldFetch;
        
        public NetworkStatus(boolean isAvailable, String type, boolean isMetered, boolean shouldFetch) {
            this.isAvailable = isAvailable;
            this.type = type;
            this.isMetered = isMetered;
            this.shouldFetch = shouldFetch;
        }
        
        public boolean isAvailable() { return isAvailable; }
        public String getType() { return type; }
        public boolean isMetered() { return isMetered; }
        public boolean shouldFetch() { return shouldFetch; }
        
        public String getStatusMessage() {
            if (!isAvailable) {
                return "❌ No internet connection";
            } else if (type.equals("WiFi")) {
                return "📶 Connected via WiFi";
            } else if (type.equals("Cellular")) {
                return isMetered ? "📱 Cellular (metered)" : "📱 Cellular (unlimited)";
            } else {
                return "🌐 Connected via " + type;
            }
        }
    }
}

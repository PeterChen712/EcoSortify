package com.example.glean.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

/**
 * Listener untuk memantau perubahan status koneksi internet secara real-time
 */
public class NetworkConnectionListener {
    
    public interface OnNetworkChangeListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }
    
    private Context context;
    private OnNetworkChangeListener listener;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private BroadcastReceiver connectivityReceiver;
    private boolean isRegistered = false;
    private boolean wasConnected = true; // Assume connected initially
    
    public NetworkConnectionListener(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    public void setOnNetworkChangeListener(OnNetworkChangeListener listener) {
        this.listener = listener;
    }
    
    public void startListening() {
        if (isRegistered) return;
        
        // Check initial state
        boolean isCurrentlyConnected = NetworkHelper.isNetworkAvailable(context);
        wasConnected = isCurrentlyConnected;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Use NetworkCallback for API 24+
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    if (!wasConnected && listener != null) {
                        wasConnected = true;
                        listener.onNetworkAvailable();
                    }
                }
                
                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    if (wasConnected && listener != null) {
                        wasConnected = false;
                        listener.onNetworkLost();
                    }
                }
            };
            
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        } else {
            // Use BroadcastReceiver for older versions
            connectivityReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean isConnected = NetworkHelper.isNetworkAvailable(context);
                    
                    if (isConnected && !wasConnected && listener != null) {
                        wasConnected = true;
                        listener.onNetworkAvailable();
                    } else if (!isConnected && wasConnected && listener != null) {
                        wasConnected = false;
                        listener.onNetworkLost();
                    }
                }
            };
            
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(connectivityReceiver, filter);
        }
        
        isRegistered = true;
    }
    
    public void stopListening() {
        if (!isRegistered) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            } else if (connectivityReceiver != null) {
                context.unregisterReceiver(connectivityReceiver);
                connectivityReceiver = null;
            }
        } catch (Exception e) {
            // Ignore unregister errors
        }
        
        isRegistered = false;
    }
}

package com.example.glean.helper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class PermissionHelper {

    // Common permission request codes
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 1003;

    /**
     * Check if location permissions are granted
     */
    public static boolean hasLocationPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request location permissions
     */
    public static void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    /**
     * Request location permissions in a fragment
     */
    public static void requestLocationPermissions(Fragment fragment) {
        fragment.requestPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    /**
     * Check if camera permissions are granted
     */
    public static boolean hasCameraPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request camera permissions
     */
    public static void requestCameraPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE);
    }

    /**
     * Request camera permissions in a fragment
     */
    public static void requestCameraPermissions(Fragment fragment) {
        fragment.requestPermissions(
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE);
    }

    /**
     * Check if storage permissions are granted
     */
    public static boolean hasStoragePermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true; // On Android 10+, we use scoped storage
        }
        
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request storage permissions 
     */
    public static void requestStoragePermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return; // On Android 10+, we use scoped storage
        }
        
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_REQUEST_CODE);
    }

    /**
     * Request storage permissions in a fragment
     */
    public static void requestStoragePermissions(Fragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return; // On Android 10+, we use scoped storage
        }
        
        fragment.requestPermissions(
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_REQUEST_CODE);
    }

    /**
     * Check if notifications permission is granted (Android 13+)
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 33) { // Android 13+
            return ContextCompat.checkSelfPermission(context,
                    "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Always granted before Android 13
    }

    /**
     * Request notification permission (Android 13+)
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 33) { // Android 13+
            ActivityCompat.requestPermissions(activity,
                    new String[]{"android.permission.POST_NOTIFICATIONS"},
                    1004);
        }
    }
}
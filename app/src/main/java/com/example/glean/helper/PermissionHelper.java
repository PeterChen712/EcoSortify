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
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1004;
    public static final int AUDIO_PERMISSION_REQUEST_CODE = 1005;
    public static final int ALL_PERMISSIONS_REQUEST_CODE = 1999;

    // ===============================
    // LOCATION PERMISSIONS
    // ===============================

    /**
     * Check if location permissions are granted
     */
    public static boolean hasLocationPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if coarse location permission is granted
     */
    public static boolean hasCoarseLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if any location permission is granted
     */
    public static boolean hasAnyLocationPermission(Context context) {
        return hasLocationPermissions(context) || hasCoarseLocationPermission(context);
    }

    /**
     * Request location permissions
     */
    public static void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    /**
     * Request location permissions in a fragment
     */
    public static void requestLocationPermissions(Fragment fragment) {
        fragment.requestPermissions(
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    /**
     * Request location permissions in a fragment with custom request code
     */
    public static void requestLocationPermissions(Fragment fragment, int requestCode) {
        fragment.requestPermissions(
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                requestCode);
    }

    // ===============================
    // CAMERA PERMISSIONS
    // ===============================

    /**
     * Check if camera permissions are granted
     */
    public static boolean hasCameraPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Alias method for consistency with TrashMLFragment
     */
    public static boolean hasCameraPermission(Context context) {
        return hasCameraPermissions(context);
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
     * Request camera permissions in a fragment with custom request code
     */
    public static void requestCameraPermission(Fragment fragment, int requestCode) {
        fragment.requestPermissions(
                new String[]{Manifest.permission.CAMERA},
                requestCode);
    }

    // ===============================
    // STORAGE PERMISSIONS
    // ===============================

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
     * Check if read external storage permission is granted
     */
    public static boolean hasReadStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true; // On Android 10+, we use scoped storage
        }
        
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request storage permissions 
     */
    public static void requestStoragePermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return; // On Android 10+, we use scoped storage
        }
        
        ActivityCompat.requestPermissions(activity,
                new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                },
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
                new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                },
                STORAGE_PERMISSION_REQUEST_CODE);
    }

    // ===============================
    // NOTIFICATION PERMISSIONS
    // ===============================

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
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Request notification permission in a fragment (Android 13+)
     */
    public static void requestNotificationPermission(Fragment fragment) {
        if (Build.VERSION.SDK_INT >= 33) { // Android 13+
            fragment.requestPermissions(
                    new String[]{"android.permission.POST_NOTIFICATIONS"},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    // ===============================
    // AUDIO PERMISSIONS
    // ===============================

    /**
     * Check if microphone permission is granted
     */
    public static boolean hasAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request microphone permission
     */
    public static void requestAudioPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_PERMISSION_REQUEST_CODE);
    }

    /**
     * Request microphone permission in a fragment
     */
    public static void requestAudioPermission(Fragment fragment) {
        fragment.requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_PERMISSION_REQUEST_CODE);
    }

    // ===============================
    // MULTIPLE PERMISSIONS
    // ===============================

    /**
     * Check if all basic app permissions are granted
     */
    public static boolean hasAllBasicPermissions(Context context) {
        return hasCameraPermissions(context) && 
               hasAnyLocationPermission(context) && 
               hasStoragePermissions(context);
    }

    /**
     * Request all basic app permissions
     */
    public static void requestAllBasicPermissions(Activity activity) {
        String[] permissions;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - no storage permissions needed
            permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else {
            // Below Android 10 - include storage permissions
            permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
        
        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            String[] newPermissions = new String[permissions.length + 1];
            System.arraycopy(permissions, 0, newPermissions, 0, permissions.length);
            newPermissions[permissions.length] = "android.permission.POST_NOTIFICATIONS";
            permissions = newPermissions;
        }
        
        ActivityCompat.requestPermissions(activity, permissions, ALL_PERMISSIONS_REQUEST_CODE);
    }

    /**
     * Request all basic app permissions in a fragment
     */
    public static void requestAllBasicPermissions(Fragment fragment) {
        String[] permissions;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - no storage permissions needed
            permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else {
            // Below Android 10 - include storage permissions
            permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
        
        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            String[] newPermissions = new String[permissions.length + 1];
            System.arraycopy(permissions, 0, newPermissions, 0, permissions.length);
            newPermissions[permissions.length] = "android.permission.POST_NOTIFICATIONS";
            permissions = newPermissions;
        }
        
        fragment.requestPermissions(permissions, ALL_PERMISSIONS_REQUEST_CODE);
    }

    // ===============================
    // UTILITY METHODS
    // ===============================

    /**
     * Check if permission request result was granted
     */
    public static boolean isPermissionGranted(int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if all permissions in request result were granted
     */
    public static boolean areAllPermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return grantResults.length > 0;
    }

    /**
     * Check if we should show rationale for a permission
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * Check if we should show rationale for a permission in fragment
     */
    public static boolean shouldShowRequestPermissionRationale(Fragment fragment, String permission) {
        return fragment.shouldShowRequestPermissionRationale(permission);
    }

    /**
     * Get permission status description for debugging
     */
    public static String getPermissionStatusReport(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("Permission Status Report:\n");
        report.append("Camera: ").append(hasCameraPermissions(context) ? "GRANTED" : "DENIED").append("\n");
        report.append("Location (Fine): ").append(hasLocationPermissions(context) ? "GRANTED" : "DENIED").append("\n");
        report.append("Location (Coarse): ").append(hasCoarseLocationPermission(context) ? "GRANTED" : "DENIED").append("\n");
        report.append("Storage: ").append(hasStoragePermissions(context) ? "GRANTED" : "DENIED").append("\n");
        report.append("Notifications: ").append(hasNotificationPermission(context) ? "GRANTED" : "DENIED").append("\n");
        report.append("Audio: ").append(hasAudioPermission(context) ? "GRANTED" : "DENIED").append("\n");
        return report.toString();
    }
}
package com.example.glean.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.databinding.FragmentPloggingBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.db.DaoTrash;
import com.example.glean.model.LocationPointEntity;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.UserEntity;
import com.example.glean.service.LocationService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PloggingFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "PloggingFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 1003;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1004;    // Auto-finish timeout constants
    private static final long AUTO_FINISH_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
    private static final long WARNING_TIMEOUT_MS = 4 * 60 * 1000; // 4 minutes (1 minute warning)
    
    // Location movement detection constants
    private static final long MOVEMENT_CHECK_INTERVAL_MS = 10 * 60 * 1000; // 10 minutes
    private static final long MOVEMENT_WARNING_TIMEOUT_MS = 10 * 1000; // 10 seconds auto-finish timeout
    private static final float MINIMAL_MOVEMENT_THRESHOLD_METERS = 50f; // 50 meters movement threshold

    private FragmentPloggingBinding binding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private List<LatLng> routePoints = new ArrayList<>();
    private boolean isTracking = false;
    private float totalDistance = 0;
    private Location lastLocation;

    private AppDatabase db;
    private int userId;
    private ExecutorService executor;
    private int currentRecordId = -1;
    private Marker currentLocationMarker;
    private LocationCallback trackingCallback;    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private BroadcastReceiver networkReceiver;
    private IntentFilter filter;
    private boolean isNetworkAvailable = false;
    private boolean isPloggingEnabled = false;
    private Snackbar networkSnackbar;
    private AlertDialog networkDialog;

    // GPS/Location services monitoring variables
    private LocationManager locationManager;
    private BroadcastReceiver gpsReceiver;
    private boolean isGpsEnabled = false;
    private boolean wasTrackingBeforeGpsLoss = false;
    private AlertDialog gpsDialog;

    private View noInternetLayout;
    private Button btnRetryConnection;
    private Button btnOpenSettings;
    private Button btnContinueOffline;
    private int currentTrashCount = 0;
    private int currentPoints = 0;

    private boolean wasTrackingBeforeNetworkLoss = false;    // Auto-finish timer variables
    private Handler autoFinishHandler;
    private Runnable autoFinishRunnable;
    private Runnable warningRunnable;
    private long networkLossStartTime = 0;
    private boolean hasShownWarning = false;
    
    // Location movement detection variables
    private Location startLocationForMovement;
    private long movementCheckStartTime = 0;
    private Handler movementCheckHandler;
    private Runnable movementCheckRunnable;    private Runnable movementWarningTimeoutRunnable;
    private AlertDialog movementWarningDialog;
    private boolean hasShownMovementWarning = false;

    // Photo capture variables
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri photoUri;
    private String currentPhotoPath;
    private int completedRecordId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        userId = prefs.getInt("USER_ID", -1);
        executor = Executors.newSingleThreadExecutor();        // Initialize auto-finish handler
        autoFinishHandler = new Handler(Looper.getMainLooper());
        
        // Initialize movement check handler
        movementCheckHandler = new Handler(Looper.getMainLooper());

        // Verify user exists, if not create a default user
        if (userId == -1) {
            createDefaultUser();
        } else {
            verifyUserExists();
        }        initializeNetworkMonitoring();
        initializeGpsMonitoring();
        initializeGoogleServices();
        initializeCameraLaunchers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPloggingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeNoInternetViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }        restoreTrackingSession();        updateUIForTrackingState(isTracking);
        updateUIForNetworkState(isNetworkAvailable);
          // Set up button click listeners for new UI structure
        binding.btnStartStop.setOnClickListener(v -> toggleTracking());
        binding.btnCollectTrash.setOnClickListener(v -> navigateToTrashCollection());
        binding.btnFinish.setOnClickListener(v -> finishPlogging());

        checkNetworkStatus();
        checkGpsStatus();

        // Restore auto-finish timer if needed
        checkAndRestoreAutoFinishTimer();
    }    private void initializeNoInternetViews() {
        noInternetLayout = binding.layoutNoInternetOverlay;
        btnRetryConnection = noInternetLayout.findViewById(R.id.btn_retry_connection);
        
        if (btnRetryConnection != null) {
            btnRetryConnection.setOnClickListener(v -> {
                btnRetryConnection.setText("🔄 Checking...");
                btnRetryConnection.setEnabled(false);

                binding.getRoot().postDelayed(() -> {
                    checkNetworkStatus();
                    btnRetryConnection.setText("🔄 Retry Connection");
                    btnRetryConnection.setEnabled(true);
                }, 1500);
            });
        }
        
        // Note: btn_open_settings and btn_continue_offline buttons may need to be added to XML
        // if offline functionality is required
    }    @Override
    public void onResume() {
        super.onResume();
        registerNetworkCallbacks();
        registerGpsCallbacks();

        restoreTrackingSession();

        updateTrashDataFromDatabase();

        if (isTracking && fusedLocationClient != null && trackingCallback == null) {
            startContinuousLocationTracking();
        }
    }

    private void updateTrashDataFromDatabase() {
        if (currentRecordId != -1) {
            executor.execute(() -> {
                try {
                    DaoTrash trashDao = db.trashDao();
                    int trashCount = trashDao.getTrashCountByRecordIdSync(currentRecordId);
                    int totalPoints = trashDao.getTotalPointsByRecordIdSync(currentRecordId);

                    requireActivity().runOnUiThread(() -> {
                        currentTrashCount = trashCount;
                        currentPoints = totalPoints;
                        updateTrashUIAlternative();
                    });
                } catch (Exception e) {

                }
            });
        }
    }    private void updateTrashUIAlternative() {
        if (binding == null) return;

        // Update individual stat displays
        if (binding.tvDistance != null) {
            binding.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", totalDistance / 1000f));
        }

        if (binding.tvTrashCount != null) {
            binding.tvTrashCount.setText(String.valueOf(currentTrashCount));
        }

        // Update chronometer is handled separately, no need to update duration value here
        // as it's automatically updated by the Chronometer widget

        // Update trash button text
        if (binding.btnCollectTrash != null) {
            if (isTracking) {
                String buttonText;
                if (currentTrashCount > 0) {
                    buttonText = String.format("Trash (+%d)", currentPoints);
                } else {
                    buttonText = "Trash";
                }
                binding.btnCollectTrash.setText(buttonText);
            } else {
                binding.btnCollectTrash.setText("Trash");
            }
        }
    }    private void toggleTracking() {
        // If network is not available, show network options
        if (!isNetworkAvailable) {
            showNetworkOptionsDialog();
            return;
        }
        
        // If GPS is not enabled, directly show GPS settings dialog
        if (!isGpsEnabled) {
            showGpsDisabledDialog();
            return;
        }
        
        // If both conditions are met, proceed with normal tracking
        if (!isPloggingEnabled) {
            showNetworkStatusMessage("⚠️ Please check your internet and location settings", true);
            return;
        }

        if (isTracking) {
            showStopConfirmationDialog();
        } else {
            if (hasActiveSession()) {
                resumeTracking();
            } else {
                startTracking();
            }
        }
    }

    private void showStopConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🛑 Stop Plogging?")
                .setMessage("Do you want to stop the current plogging session?\n\n" +
                        "⚠️ Note: You can finish the session to save your progress, or continue tracking.")
                .setPositiveButton("Stop", (dialog, which) -> {
                    internalPauseTracking();
                    showNetworkStatusMessage("🛑 Plogging stopped. Tap 'Start Plogging' to resume or 'Finish' to complete.", false);
                })                .setNegativeButton("Cancel", null)
                .setNeutralButton("Finish Session", (dialog, which) -> {
                    finishPlogging();
                })
                .show();
    }

    private void startTracking() {
        // If network is not available, show network options
        if (!isNetworkAvailable) {
            showNetworkOptionsDialog();
            return;
        }
        
        // If GPS is not enabled, directly show GPS settings dialog
        if (!isGpsEnabled) {
            showGpsDisabledDialog();
            return;
        }
        
        // If both conditions are met but plogging is still disabled, show general message
        if (!isPloggingEnabled) {
            showNetworkStatusMessage("⚠️ Please check your internet and location settings", true);
            return;
        }
        
        executor.execute(() -> {
            try {
                // First, verify that the user exists in the database
                UserEntity user = db.userDao().getUserByIdSync(userId);
                if (user == null) {
                    // User doesn't exist, show error and return
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "User not found. Please login again.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                // Create new record with verified userId
                RecordEntity record = new RecordEntity();
                record.setUserId(userId);
                record.setCreatedAt(System.currentTimeMillis());
                record.setUpdatedAt(System.currentTimeMillis());
                record.setDistance(0);
                record.setDuration(0);
                record.setPoints(0);
                record.setDescription("Plogging Session");

                long recordId = db.recordDao().insert(record);
                currentRecordId = (int) recordId;

                requireActivity().runOnUiThread(() -> {
                    isTracking = true;

                    currentTrashCount = 0;
                    currentPoints = 0;
                    totalDistance = 0f;

                    saveTrackingSession(true, currentRecordId, System.currentTimeMillis(), 0f);

                    updateUIForTrackingState(true);
                    updateTrashUIAlternative();                    binding.chronometer.setBase(SystemClock.elapsedRealtime());
                    binding.chronometer.start();
                    
                    // Reset movement detection state to ensure clean start for new session
                    stopMovementDetection();
                    
                    startContinuousLocationTracking();

                    String statusMessage = isNetworkAvailable ?
                            "🏃‍♂️ Plogging started with full GPS accuracy!" :
                            "🏃‍♂️ Plogging started in offline mode!";

                    showNetworkStatusMessage(statusMessage, false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error starting tracking", e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to start plogging session. Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Intent serviceIntent = new Intent(requireContext(), LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_START_TRACKING);
            serviceIntent.putExtra("RECORD_ID", currentRecordId);
            requireActivity().startService(serviceIntent);
        }
    }    private void internalPauseTracking() {
        isTracking = false;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        long sessionStartTime = prefs.getLong("SESSION_START_TIME", System.currentTimeMillis());
        saveTrackingSession(false, currentRecordId, sessionStartTime, totalDistance);

        updateUIForTrackingState(false);

        binding.chronometer.stop();

        if (fusedLocationClient != null && trackingCallback != null) {
            fusedLocationClient.removeLocationUpdates(trackingCallback);
            trackingCallback = null;
        }

        // Stop movement detection when pausing
        stopMovementDetection();

        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_STOP_TRACKING);
        requireActivity().startService(serviceIntent);
    }    private void stopTracking() {
        isTracking = false;

        saveTrackingSession(false, -1, 0, 0f);

        currentRecordId = -1;
        totalDistance = 0f;
        currentTrashCount = 0;
        currentPoints = 0;
        wasTrackingBeforeNetworkLoss = false;

        updateUIForTrackingState(false);
        updateTrashUIAlternative();

        binding.chronometer.stop();
        binding.chronometer.setBase(SystemClock.elapsedRealtime());

        if (fusedLocationClient != null && trackingCallback != null) {
            fusedLocationClient.removeLocationUpdates(trackingCallback);
            trackingCallback = null;
        }

        // Stop movement detection when stopping session
        stopMovementDetection();

        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_STOP_TRACKING);
        requireActivity().startService(serviceIntent);

        showNetworkStatusMessage("🛑 Plogging session ended.", false);
    }    private void resumeTracking() {
        if (currentRecordId == -1) {
            showNetworkStatusMessage("❌ No active session found", true);
            return;
        }
        
        if (!isPloggingEnabled) {
            showNetworkStatusMessage("⚠️ Internet connection required to resume tracking", true);
            return;
        }
        
        if (!isGpsEnabled) {
            showGpsStatusMessage("⚠️ Location services required to resume tracking", true);
            showGpsDisabledDialog();
            return;
        }

        requireActivity().runOnUiThread(() -> {
            isTracking = true;
            updateUIForTrackingState(true);
            updateTrashUIAlternative();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            long sessionStartTime = prefs.getLong("SESSION_START_TIME", System.currentTimeMillis());
            if (sessionStartTime > 0) {
                long elapsedTime = System.currentTimeMillis() - sessionStartTime;
                binding.chronometer.setBase(SystemClock.elapsedRealtime() - elapsedTime);
                binding.chronometer.start();
            }            startContinuousLocationTracking();

            String statusMessage = String.format(Locale.getDefault(),
                    "▶️ Plogging resumed! Distance: %.2f km, Trash: %d items, Points: %d",
                    totalDistance / 1000f, currentTrashCount, currentPoints);
            showNetworkStatusMessage(statusMessage, false);
        });

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Intent serviceIntent = new Intent(requireContext(), LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_START_TRACKING);
            serviceIntent.putExtra("RECORD_ID", currentRecordId);
            requireActivity().startService(serviceIntent);
        }
    }

    private void finishPlogging() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🏁 Finish Plogging?")
                .setMessage("Are you sure you want to finish this plogging session? This action cannot be undone.")
                .setPositiveButton("Yes, Finish", (dialog, which) -> {
                    finishPloggingConfirmed();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void finishPloggingConfirmed() {
        if (isTracking) {
            internalPauseTracking();
        }

        if (currentRecordId != -1) {
            executor.execute(() -> {
                try {
                    RecordEntity record = db.recordDao().getRecordByIdSync(currentRecordId);
                    if (record != null) {
                        long elapsedTimeMillis = SystemClock.elapsedRealtime() - binding.chronometer.getBase();

                        int finalTrashCount = db.trashDao().getTrashCountByRecordIdSync(currentRecordId);
                        int finalPoints = db.trashDao().getTotalPointsByRecordIdSync(currentRecordId);                        record.setDuration(elapsedTimeMillis);
                        record.setUpdatedAt(System.currentTimeMillis());
                        record.setDistance(totalDistance);
                        record.setPoints(finalPoints);

                        db.recordDao().update(record);

                        updateUserPoints(finalPoints);                        requireActivity().runOnUiThread(() -> {
                            stopTracking();
                            completedRecordId = record.getId(); // Store for photo capture

                            String completionMessage = String.format(Locale.getDefault(),
                                    "🎉 Plogging Complete!\n" +
                                            "📍 Distance: %.2f km\n" +
                                            "🗑️ Trash: %d items\n" +
                                            "⭐ Points: %d",
                                    totalDistance / 1000f, finalTrashCount, finalPoints);

                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Plogging Selesai")
                                    .setMessage(completionMessage + "\n\nAmbil foto dokumentasi plogging Anda?")
                                    .setPositiveButton("Ambil Foto", (dialog, which) -> {
                                        requestCameraPermissionAndCapture();
                                    })
                                    .setNegativeButton("Lihat Hasil", (dialog, which) -> {
                                        navigateToSummary(record.getId());
                                    })
                                    .setNeutralButton("Simpan ke Galeri", (dialog, which) -> {
                                        savePloggingResultToGallery(completionMessage);
                                    })
                                    .show();
                        });
                    }
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error saving plogging data", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void updateUserPoints(int pointsEarned) {
        if (pointsEarned > 0 && userId != -1) {
            executor.execute(() -> {                try {
                    UserEntity user = db.userDao().getUserByIdSync(userId);
                    if (user != null) {
                        int currentPoints = user.getPoints();
                        user.setPoints(currentPoints + pointsEarned);
                        db.userDao().update(user);
                    }
                } catch (Exception e) {

                }
            });
        }    }

    /**
     * Navigate to the plogging summary fragment
     */
    private void navigateToSummary(int recordId) {
        try {
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putInt("RECORD_ID", recordId);
            navController.navigate(R.id.action_ploggingFragment_to_ploggingSummaryFragment, args);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to plogging summary", e);
            Toast.makeText(requireContext(), "Gagal membuka hasil plogging", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Request camera permission and launch camera if granted
     */
    private void requestCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Launch camera intent to capture photo
     */
    private void launchCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            
            // Create a file to save the photo
            java.io.File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "com.example.glean.fileprovider",
                    photoFile
                );
                
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                cameraLauncher.launch(cameraIntent);
            } else {
                Toast.makeText(requireContext(), "Error creating photo file", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching camera", e);
            Toast.makeText(requireContext(), "Error launching camera", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create a unique image file for photo capture
     */
    private java.io.File createImageFile() throws java.io.IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "PLOGGING_" + timeStamp;
        java.io.File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        java.io.File image = java.io.File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
        );

        // Save the file path for later use
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Save the captured photo to database and show completion dialog
     */
    private void savePloggingPhotoToDatabase() {
        if (completedRecordId == -1 || photoUri == null) {
            Log.w(TAG, "Cannot save photo: invalid record ID or photo URI");
            navigateToSummary(completedRecordId);
            return;
        }

        executor.execute(() -> {
            try {
                // Update the record with photo path
                RecordEntity record = db.recordDao().getRecordByIdSync(completedRecordId);
                if (record != null) {
                    record.setPhotoPath(currentPhotoPath);
                    db.recordDao().update(record);
                    
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Foto dokumentasi tersimpan!", Toast.LENGTH_SHORT).show();
                        
                        // Show completion dialog with navigation options
                        new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Foto Tersimpan")
                            .setMessage("Foto dokumentasi plogging berhasil disimpan! Lihat hasil plogging Anda sekarang?")
                            .setPositiveButton("Lihat Hasil", (dialog, which) -> {
                                navigateToSummary(completedRecordId);
                            })
                            .setNegativeButton("Nanti", null)
                            .show();
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error: Record not found", Toast.LENGTH_SHORT).show();
                        navigateToSummary(completedRecordId);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving photo to database", e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error saving photo", Toast.LENGTH_SHORT).show();
                    navigateToSummary(completedRecordId);
                });
            }
        });
    }

    private void restoreTrackingSession() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        boolean wasTracking = prefs.getBoolean("IS_TRACKING", false);
        int activeRecordId = prefs.getInt("ACTIVE_RECORD_ID", -1);
        long sessionStartTime = prefs.getLong("SESSION_START_TIME", 0);
        float sessionDistance = prefs.getFloat("SESSION_DISTANCE", 0f);

        if (activeRecordId != -1) {
            currentRecordId = activeRecordId;
            totalDistance = sessionDistance;

            if (wasTracking) {
                isTracking = true;
                wasTrackingBeforeNetworkLoss = true;
            } else {
                isTracking = false;
                wasTrackingBeforeNetworkLoss = false;
            }

            executor.execute(() -> {
                try {
                    int trashCount = db.trashDao().getTrashCountByRecordIdSync(currentRecordId);
                    int points = db.trashDao().getTotalPointsByRecordIdSync(currentRecordId);

                    requireActivity().runOnUiThread(() -> {
                        currentTrashCount = trashCount;
                        currentPoints = points;
                        updateTrashUIAlternative();
                    });
                } catch (Exception e) {

                }
            });

            if (sessionStartTime > 0 && binding != null) {
                long elapsedTime = System.currentTimeMillis() - sessionStartTime;
                binding.chronometer.setBase(SystemClock.elapsedRealtime() - elapsedTime);
                if (isTracking) {
                    binding.chronometer.start();
                }
            }

            updateUIForTrackingState(isTracking);

            String statusMessage = String.format(Locale.getDefault(),
                    "🏃‍♂️ Session %s! Distance: %.2f km, Trash: %d items, Points: %d",
                    isTracking ? "active" : "available",
                    totalDistance / 1000f, currentTrashCount, currentPoints);
            showNetworkStatusMessage(statusMessage, false);
        } else {
            isTracking = false;
            currentRecordId = -1;
            totalDistance = 0f;
            currentTrashCount = 0;
            currentPoints = 0;
            wasTrackingBeforeNetworkLoss = false;
            updateUIForTrackingState(false);
            updateTrashUIAlternative();

            if (binding != null && binding.chronometer != null) {
                binding.chronometer.stop();
                binding.chronometer.setBase(SystemClock.elapsedRealtime());
            }
        }
    }    private void navigateToTrashCollection() {
        if (isTracking && currentRecordId != -1) {
            try {
                Log.d("PloggingFragment", "=== NAVIGATION TO TRASH COLLECTION DEBUG ===");
                Log.d("PloggingFragment", "NAV: currentRecordId = " + currentRecordId);
                Log.d("PloggingFragment", "NAV: currentTrashCount = " + currentTrashCount);
                Log.d("PloggingFragment", "NAV: currentPoints = " + currentPoints);
                Log.d("PloggingFragment", "NAV: isTracking = " + isTracking);
                
                showNetworkStatusMessage("📸 Opening trash collection...", false);

                NavController navController = Navigation.findNavController(requireView());
                Bundle args = new Bundle();
                args.putInt("RECORD_ID", currentRecordId);
                args.putInt("CURRENT_TRASH_COUNT", currentTrashCount);
                args.putInt("CURRENT_POINTS", currentPoints);
                
                Log.d("PloggingFragment", "NAV: Bundle created with keys: " + args.keySet().toString());
                Log.d("PloggingFragment", "NAV: Bundle RECORD_ID value: " + args.getInt("RECORD_ID"));
                Log.d("PloggingFragment", "=== NAVIGATION DEBUG END ===");
                
                navController.navigate(R.id.action_ploggingFragment_to_trashMLFragment, args);
            } catch (Exception e) {
                Log.e("PloggingFragment", "NAV: Error navigating to trash collection", e);
                Toast.makeText(requireContext(), "Trash collection feature temporarily unavailable", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w("PloggingFragment", "NAV: Cannot navigate - isTracking: " + isTracking + ", currentRecordId: " + currentRecordId);
            Toast.makeText(requireContext(), "Start plogging first", Toast.LENGTH_SHORT).show();
        }
    }private void updateUIForTrackingState(boolean tracking) {
        if (binding == null) return;

        if (tracking) {
            // Show active state layout with Trash and Finish buttons
            binding.layoutInitialState.setVisibility(View.GONE);
            binding.layoutActiveState.setVisibility(View.VISIBLE);
            
            // Enable buttons in active state
            binding.btnCollectTrash.setEnabled(true);
            binding.btnFinish.setEnabled(true);
            
            // Update button colors and alpha
            binding.btnCollectTrash.setAlpha(1.0f);
            binding.btnFinish.setAlpha(1.0f);
        } else {
            if (hasActiveSession()) {
                // Show active state but with different button states for paused session
                binding.layoutInitialState.setVisibility(View.GONE);
                binding.layoutActiveState.setVisibility(View.VISIBLE);
                
                // Update finish button to be enabled for paused sessions
                binding.btnFinish.setEnabled(true);
                binding.btnFinish.setAlpha(1.0f);
                
                // Disable trash button when not actively tracking
                binding.btnCollectTrash.setEnabled(false);
                binding.btnCollectTrash.setAlpha(0.6f);
                  // Show start button in overlay for resume functionality
                binding.layoutInitialState.setVisibility(View.VISIBLE);
                String resumeButtonText;
                if (!isNetworkAvailable) {
                    resumeButtonText = "Internet Required";
                } else if (!isGpsEnabled) {
                    resumeButtonText = "Location Required";
                } else {
                    resumeButtonText = "▶️ Resume";
                }
                binding.btnStartStop.setText(resumeButtonText);
                binding.btnStartStop.setEnabled(isPloggingEnabled);
                binding.btnStartStop.setAlpha(isPloggingEnabled ? 1.0f : 0.6f);
            } else {
                // Show initial state layout with Start button only
                binding.layoutInitialState.setVisibility(View.VISIBLE);
                binding.layoutActiveState.setVisibility(View.GONE);
                  // Update start button
                String buttonText;
                if (!isNetworkAvailable) {
                    buttonText = "Internet Required";
                } else if (!isGpsEnabled) {
                    buttonText = "Location Required";
                } else {
                    buttonText = "🚀 Start Plogging";
                }
                binding.btnStartStop.setText(buttonText);
                binding.btnStartStop.setEnabled(isPloggingEnabled);
                binding.btnStartStop.setAlpha(isPloggingEnabled ? 1.0f : 0.6f);
            }
        }
    }

    private boolean hasActiveSession() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int activeRecordId = prefs.getInt("ACTIVE_RECORD_ID", -1);
        boolean hasSession = activeRecordId != -1;

        return hasSession;
    }

    private void showRouteOnMap(int recordId) {
        try {
            if (mMap == null) {
                Toast.makeText(requireContext(), "Map not ready", Toast.LENGTH_SHORT).show();
                return;
            }

            if (routePoints != null && !routePoints.isEmpty()) {
                mMap.clear();

                if (routePoints.size() > 0) {
                    LatLng startPoint = routePoints.get(0);
                    mMap.addMarker(new MarkerOptions()
                            .position(startPoint)
                            .title("🏁 Start Point")
                            .snippet("Plogging session started here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                }

                if (routePoints.size() > 1) {
                    LatLng endPoint = routePoints.get(routePoints.size() - 1);
                    mMap.addMarker(new MarkerOptions()
                            .position(endPoint)
                            .title("🏁 End Point")
                            .snippet("Plogging session finished here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                }

                if (routePoints.size() > 0) {
                    double minLat = routePoints.get(0).latitude;
                    double maxLat = routePoints.get(0).latitude;
                    double minLng = routePoints.get(0).longitude;
                    double maxLng = routePoints.get(0).longitude;

                    for (LatLng point : routePoints) {
                        minLat = Math.min(minLat, point.latitude);
                        maxLat = Math.max(maxLat, point.latitude);
                        minLng = Math.min(minLng, point.longitude);
                        maxLng = Math.max(maxLng, point.longitude);
                    }

                    LatLng center = new LatLng((minLat + maxLat) / 2, (minLng + maxLng) / 2);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 15f));
                }

                Toast.makeText(requireContext(), "🗺️ Route displayed on map", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "No route data available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error displaying route", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePloggingResultToGallery(String completionMessage) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                savePloggingImageAndroid10Plus(completionMessage);
            } else {
                if (ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    savePloggingImageLegacy(completionMessage);
                } else {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            STORAGE_PERMISSION_REQUEST_CODE);
                }
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error saving to gallery", Toast.LENGTH_SHORT).show();
        }
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private void savePloggingImageAndroid10Plus(String completionMessage) {
        try {
            android.graphics.Bitmap bitmap = createPloggingSummaryBitmap(completionMessage);

            if (bitmap != null) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                        "Plogging_Result_" + System.currentTimeMillis() + ".jpg");
                values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/Glean/");

                android.net.Uri uri = requireContext().getContentResolver()
                        .insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (java.io.OutputStream outputStream = requireContext().getContentResolver()
                            .openOutputStream(uri)) {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream);

                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "📸 Plogging result saved to gallery!",
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }

                bitmap.recycle();
            }
        } catch (Exception e) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Error saving to gallery", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void savePloggingImageLegacy(String completionMessage) {
        try {
            android.graphics.Bitmap bitmap = createPloggingSummaryBitmap(completionMessage);

            if (bitmap != null) {
                java.io.File picturesDir = android.os.Environment
                        .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
                java.io.File gleanDir = new java.io.File(picturesDir, "Glean");

                if (!gleanDir.exists()) {
                    gleanDir.mkdirs();
                }

                java.io.File imageFile = new java.io.File(gleanDir,
                        "Plogging_Result_" + System.currentTimeMillis() + ".jpg");

                try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(imageFile)) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream);

                    android.media.MediaScannerConnection.scanFile(requireContext(),
                            new String[]{imageFile.getAbsolutePath()}, null, null);

                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "📸 Plogging result saved to gallery!",
                                Toast.LENGTH_LONG).show();
                    });
                }

                bitmap.recycle();
            }
        } catch (Exception e) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Error saving to gallery", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private android.graphics.Bitmap createPloggingSummaryBitmap(String completionMessage) {
        try {
            int width = 800;
            int height = 600;

            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height,
                    android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

            canvas.drawColor(android.graphics.Color.WHITE);

            android.graphics.Paint textPaint = new android.graphics.Paint();
            textPaint.setColor(android.graphics.Color.BLACK);
            textPaint.setTextSize(24f);
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            android.graphics.Paint titlePaint = new android.graphics.Paint();
            titlePaint.setColor(android.graphics.Color.parseColor("#4CAF50"));
            titlePaint.setTextSize(32f);
            titlePaint.setAntiAlias(true);
            titlePaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            canvas.drawText("🌱 Glean Plogging Result", 50, 80, titlePaint);

            String[] lines = completionMessage.split("\n");
            int yPosition = 150;

            for (String line : lines) {
                canvas.drawText(line, 50, yPosition, textPaint);
                yPosition += 40;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String timestamp = "Generated on: " + sdf.format(new Date());

            android.graphics.Paint timestampPaint = new android.graphics.Paint();
            timestampPaint.setColor(android.graphics.Color.GRAY);
            timestampPaint.setTextSize(18f);
            timestampPaint.setAntiAlias(true);

            canvas.drawText(timestamp, 50, height - 50, timestampPaint);

            canvas.drawText("Generated by Glean App", 50, height - 20, timestampPaint);

            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private void requestUltraHighPrecisionLocation() {
        if (fusedLocationClient == null) return;

        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(1000)
                    .setFastestInterval(500)
                    .setNumUpdates(10)
                    .setSmallestDisplacement(0.1f);

            LocationCallback ultraPrecisionCallback = new LocationCallback() {
                private int updateCount = 0;
                private Location bestLocation = null;
                private float bestAccuracy = Float.MAX_VALUE;

                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null || locationResult.getLocations().isEmpty()) {
                        return;
                    }

                    for (Location location : locationResult.getLocations()) {
                        updateCount++;

                        if (location.getAccuracy() < bestAccuracy) {
                            bestLocation = location;
                            bestAccuracy = location.getAccuracy();

                            if (mMap != null) {
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 22f), 2000, null);

                                addCurrentLocationMarker(currentLatLng, location.getAccuracy());

                                lastLocation = location;
                            }
                        }

                        if (location.getAccuracy() < 5f && updateCount >= 3) {
                            fusedLocationClient.removeLocationUpdates(this);
                            break;
                        }

                        if (updateCount >= 10) {
                            fusedLocationClient.removeLocationUpdates(this);
                            break;
                        }
                    }
                }
            };

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null && location.getAccuracy() < 20f) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 20f));

                            addCurrentLocationMarker(currentLatLng, location.getAccuracy());

                            lastLocation = location;
                        }

                        if (ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.requestLocationUpdates(locationRequest,
                                    ultraPrecisionCallback,
                                    Looper.getMainLooper());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.requestLocationUpdates(locationRequest,
                                    ultraPrecisionCallback,
                                    Looper.getMainLooper());
                        }
                    });

        } catch (SecurityException e) {

        }
    }

    private void addCurrentLocationMarker(LatLng location, float accuracy) {
        if (mMap == null) return;

        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        String title = "🚶 Posisi Saya";
        String snippet = String.format(Locale.getDefault(),
                "Akurasi GPS: ±%.1fm\nSiap untuk plogging!", accuracy);

        currentLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .anchor(0.5f, 0.5f));
    }

    private void startContinuousLocationTracking() {
        if (fusedLocationClient == null) return;

        try {
            LocationRequest continuousRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(2000)
                    .setFastestInterval(1000)
                    .setSmallestDisplacement(1f);            trackingCallback = new LocationCallback() {                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null || !isTracking) return;

                    Location location = locationResult.getLastLocation();
                    if (location != null && mMap != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        routePoints.add(currentLatLng);

                        addCurrentLocationMarker(currentLatLng, location.getAccuracy());

                        // Check for location movement and start movement detection if needed
                        checkLocationMovement(location);

                        // Calculate distance from last location
                        float distance = 0;
                        if (lastLocation != null) {
                            distance = lastLocation.distanceTo(location);
                            totalDistance += distance;
                        }                        // Save location point to database if we have an active record
                        if (currentRecordId != -1) {
                            final float finalDistance = distance;
                            Log.d(TAG, "📍 Preparing to save location point for record " + currentRecordId);
                            Log.d(TAG, "   Coordinates: (" + location.getLatitude() + ", " + location.getLongitude() + ")");
                            Log.d(TAG, "   Distance from last: " + finalDistance + "m");
                            Log.d(TAG, "   Total distance so far: " + totalDistance + "m");
                            
                            LocationPointEntity locationPoint = new LocationPointEntity(
                                    currentRecordId,
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    location.getAltitude(),
                                    System.currentTimeMillis(),
                                    finalDistance
                            );
                            
                            // Save to database in background thread with comprehensive error handling
                            executor.execute(() -> {
                                try {
                                    Log.d(TAG, "🔄 Starting database save operation...");
                                    
                                    // First verify the record exists
                                    RecordEntity record = db.recordDao().getRecordByIdSync(currentRecordId);
                                    if (record == null) {
                                        Log.e(TAG, "❌ CRITICAL ERROR: Record ID " + currentRecordId + " does not exist in database!");
                                        Log.e(TAG, "   This will cause foreign key constraint violation");
                                        Log.e(TAG, "   Location point cannot be saved");
                                        return;
                                    }
                                    
                                    // Get current count for verification
                                    int countBefore = db.locationPointDao().getLocationPointCountByRecordId(currentRecordId);
                                    Log.d(TAG, "   Location points before insert: " + countBefore);
                                    
                                    // Insert the location point
                                    long insertedId = db.locationPointDao().insert(locationPoint);
                                    Log.d(TAG, "✅ Location point inserted with ID: " + insertedId);
                                    
                                    // Verify insertion
                                    int countAfter = db.locationPointDao().getLocationPointCountByRecordId(currentRecordId);
                                    Log.d(TAG, "   Location points after insert: " + countAfter);
                                    
                                    if (countAfter == countBefore + 1) {
                                        Log.d(TAG, "✅ Location point insertion verified successfully");
                                    } else {
                                        Log.e(TAG, "⚠️  Warning: Location point count didn't increase as expected");
                                    }
                                    
                                    // Update total distance in record if there's movement
                                    if (finalDistance > 0) {
                                        Log.d(TAG, "🔄 Updating record distance...");
                                        db.recordDao().updateDistance(currentRecordId, finalDistance);
                                        
                                        // Verify distance update
                                        RecordEntity updatedRecord = db.recordDao().getRecordByIdSync(currentRecordId);
                                        if (updatedRecord != null) {
                                            Log.d(TAG, "✅ Record distance updated to: " + updatedRecord.getDistance() + "m");
                                        }
                                    }
                                    
                                    Log.d(TAG, "✅ Location point save operation completed successfully");
                                    
                                } catch (Exception e) {
                                    Log.e(TAG, "❌ CRITICAL ERROR saving location point to database: " + e.getMessage(), e);
                                    Log.e(TAG, "   Location: (" + location.getLatitude() + ", " + location.getLongitude() + ")");
                                    Log.e(TAG, "   Record ID: " + currentRecordId);
                                    Log.e(TAG, "   Distance: " + finalDistance + "m");
                                    
                                    // Additional debugging information
                                    try {
                                        RecordEntity record = db.recordDao().getRecordByIdSync(currentRecordId);
                                        if (record == null) {
                                            Log.e(TAG, "   Record verification: RECORD DOES NOT EXIST");
                                        } else {
                                            Log.e(TAG, "   Record verification: Record exists, User ID: " + record.getUserId());
                                        }
                                    } catch (Exception debugE) {
                                        Log.e(TAG, "   Could not verify record existence: " + debugE.getMessage());
                                    }
                                }
                            });
                        } else {
                            Log.w(TAG, "⚠️  Cannot save location point: no active record (currentRecordId = " + currentRecordId + ")");
                        }

                        // Handle session saving and UI updates
                        if (lastLocation != null) {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                            long lastSaveTime = prefs.getLong("LAST_SAVE_TIME", 0);
                            boolean shouldSave = System.currentTimeMillis() - lastSaveTime > 10000 || totalDistance - prefs.getFloat("LAST_SAVED_DISTANCE", 0) > 10;

                            if (shouldSave) {
                                saveTrackingSession(true, currentRecordId,
                                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                                                .getLong("SESSION_START_TIME", System.currentTimeMillis()),
                                        totalDistance);

                                prefs.edit().putLong("LAST_SAVE_TIME", System.currentTimeMillis())
                                        .putFloat("LAST_SAVED_DISTANCE", totalDistance)
                                        .apply();
                            }

                            requireActivity().runOnUiThread(() -> {
                                updateTrashUIAlternative();
                            });
                        }

                        lastLocation = location;
                    }
                }
            };

            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(continuousRequest,
                        trackingCallback,
                        Looper.getMainLooper());
            }
        } catch (SecurityException e) {

        }
    }

    private void saveTrackingSession(boolean isTracking, int recordId, long startTime, float distance) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("IS_TRACKING", isTracking);
        editor.putInt("ACTIVE_RECORD_ID", recordId);
        editor.putLong("SESSION_START_TIME", startTime);
        editor.putFloat("SESSION_DISTANCE", distance);
        editor.apply();
    }    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideNetworkWarning();
        unregisterNetworkCallbacks();
        unregisterGpsCallbacks();
        dismissGpsDialog();

        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }        // Cancel auto-finish timers when fragment is destroyed
        cancelAutoFinishTimer();

        if (autoFinishHandler != null) {
            autoFinishHandler.removeCallbacksAndMessages(null);
        }
        
        // Cancel movement detection timers when fragment is destroyed
        stopMovementDetection();
        
        if (movementCheckHandler != null) {
            movementCheckHandler.removeCallbacksAndMessages(null);
        }
    }    private void updateUIForNetworkState(boolean networkAvailable) {
        if (binding == null) return;

        // Update network status indicator
        if (binding.networkStatusIndicator != null) {
            if (networkAvailable) {
                binding.networkStatusIndicator.setVisibility(View.GONE);
            } else {
                binding.networkStatusIndicator.setVisibility(View.VISIBLE);
                binding.tvNetworkStatus.setText("❌ No Internet");
                binding.networkStatusDot.setBackgroundResource(R.drawable.network_dot_disconnected);
            }
        }

        // Legacy support for old network status view
        if (binding.tvNetworkStatus != null) {
            if (networkAvailable) {
                binding.tvNetworkStatus.setText("🌐 Connected");
                binding.tvNetworkStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                binding.tvNetworkStatus.setVisibility(View.VISIBLE);

                binding.tvNetworkStatus.postDelayed(() -> {
                    if (binding != null && binding.tvNetworkStatus != null) {
                        binding.tvNetworkStatus.setVisibility(View.GONE);
                    }
                }, 3000);
            } else {
                binding.tvNetworkStatus.setText("❌ No Internet");
                binding.tvNetworkStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                binding.tvNetworkStatus.setVisibility(View.VISIBLE);
            }
        }
    }    private void enablePloggingFeatures() {
        // Only enable plogging if both network and GPS are available
        isPloggingEnabled = isNetworkAvailable && isGpsEnabled;

        if (binding != null) {
            // Enable start button only if both conditions are met
            binding.btnStartStop.setEnabled(isPloggingEnabled);
            binding.btnStartStop.setAlpha(isPloggingEnabled ? 1.0f : 0.6f);
            
            // Enable other buttons based on tracking state and conditions
            if (isTracking && isPloggingEnabled) {
                binding.btnCollectTrash.setEnabled(true);
                binding.btnCollectTrash.setAlpha(1.0f);
            } else {
                binding.btnCollectTrash.setEnabled(false);
                binding.btnCollectTrash.setAlpha(0.6f);
            }
            
            // Enable finish button only if there's an active session
            if (hasActiveSession()) {
                binding.btnFinish.setEnabled(true);
                binding.btnFinish.setAlpha(1.0f);
            } else {
                binding.btnFinish.setEnabled(false);
                binding.btnFinish.setAlpha(0.6f);
            }

            updateUIForTrackingState(isTracking);
        }
    }

    private void showNetworkStatusMessage(String message, boolean isError) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(
                            isError ? android.R.color.holo_red_light : android.R.color.holo_green_light))
                    .setTextColor(getResources().getColor(android.R.color.white))
                    .show();
        }
    }

    private void showOfflineModeDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("📱 Offline Mode")
                .setMessage("You can use basic plogging features offline, but some functionality will be limited:\n\n" +
                        "✅ GPS tracking\n" +
                        "✅ Route recording\n" +
                        "✅ Distance calculation\n" +
                        "❌ Map tiles update\n" +
                        "❌ Enhanced GPS accuracy\n\n" +
                        "Enable internet for full experience.")
                .setPositiveButton("Continue Offline", (dialog, which) -> {
                    enableLimitedPloggingFeatures();
                    dialog.dismiss();
                })
                .setNegativeButton("Wait for Internet", null)
                .show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

            requestUltraHighPrecisionLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void initializeNetworkMonitoring() {
        connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    requireActivity().runOnUiThread(() -> onNetworkAvailable());
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    requireActivity().runOnUiThread(() -> onNetworkLost());
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities);
                    boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

                    requireActivity().runOnUiThread(() -> {
                        if (hasInternet && !isNetworkAvailable) {
                            onNetworkAvailable();
                        } else if (!hasInternet && isNetworkAvailable) {
                            onNetworkLost();
                        }
                    });
                }
            };
        } else {
            networkReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    checkNetworkStatus();
                }
            };
        }
    }

    private void initializeGpsMonitoring() {
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        
        // Create GPS receiver for monitoring GPS enable/disable events
        gpsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
                    checkGpsStatus();
                }
            }
        };
        
        // Check initial GPS status
        checkGpsStatus();
    }
    
    private void registerGpsCallbacks() {
        try {
            if (gpsReceiver != null) {
                IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
                requireContext().registerReceiver(gpsReceiver, filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering GPS callbacks", e);
        }
    }
    
    private void unregisterGpsCallbacks() {
        try {
            if (gpsReceiver != null) {
                try {
                    requireContext().unregisterReceiver(gpsReceiver);
                } catch (IllegalArgumentException e) {
                    // Receiver was not registered
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering GPS callbacks", e);
        }
    }
    
    private void checkGpsStatus() {
        if (locationManager == null) return;
        
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean locationServicesEnabled = gpsEnabled || networkLocationEnabled;
        
        if (locationServicesEnabled != isGpsEnabled) {
            isGpsEnabled = locationServicesEnabled;
            
            if (isGpsEnabled) {
                onGpsAvailable();
            } else {
                onGpsLost();
            }
        }
    }
      private void onGpsAvailable() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "GPS/Location services available");
            
            dismissGpsDialog();
            enablePloggingFeatures();
            
            if (wasTrackingBeforeGpsLoss && hasActiveSession() && isNetworkAvailable) {
                showGpsStatusMessage("📍 Location services restored! You can resume tracking.", false);
            } else {
                showGpsStatusMessage("📍 Location Services Enabled!", false);
            }
        });
    }
    
    private void onGpsLost() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "GPS/Location services lost");
            
            wasTrackingBeforeGpsLoss = isTracking;
            
            if (isTracking) {
                internalPauseTracking();
                showGpsStatusMessage("📍 Location services disabled! Tracking paused.", true);
            }
            
            // Disable plogging features when GPS is lost
            isPloggingEnabled = false;
            updateUIForTrackingState(isTracking);
            
            showGpsDisabledDialog();
        });
    }
    
    private void showGpsDisabledDialog() {
        if (gpsDialog != null && gpsDialog.isShowing()) {
            return; // Dialog already showing
        }
        
        if (getContext() == null || !isAdded()) return;
        
        gpsDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("📍 Enable Location Services")
                .setMessage("Location services are required for plogging tracking.\n\n" +
                           "Please enable GPS or Network location in your device settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Cannot open location settings", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }
    
    private void dismissGpsDialog() {
        if (gpsDialog != null && gpsDialog.isShowing()) {
            gpsDialog.dismiss();
            gpsDialog = null;
        }
    }
    
    private void showGpsStatusMessage(String message, boolean isError) {
        if (binding != null) {
            Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
            
            if (isError) {
                snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            } else {
                snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            }
            
            snackbar.setTextColor(Color.WHITE);
            snackbar.show();
        }
    }
    
    private void showNetworkOptionsDialog() {
        if (getContext() == null || !isAdded()) return;
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🌐 Internet Connection Required")
                .setMessage("Internet connection is required for plogging tracking.\n\n" +
                           "Please check your WiFi or mobile data connection.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        try {
                            // Fallback to wireless settings
                            Intent fallbackIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                            startActivity(fallbackIntent);
                        } catch (Exception ex) {
                            Toast.makeText(requireContext(), "Cannot open network settings", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNeutralButton("Mobile Data", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        try {
                            // Fallback to main settings
                            Intent fallbackIntent = new Intent(Settings.ACTION_SETTINGS);
                            startActivity(fallbackIntent);
                        } catch (Exception ex) {
                            Toast.makeText(requireContext(), "Cannot open mobile data settings", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }
    
    // Location movement detection methods
    private void checkLocationMovement(Location currentLocation) {
        if (!isTracking || currentLocation == null) return;

        // Start movement detection on first location
        if (startLocationForMovement == null) {
            startLocationForMovement = currentLocation;
            movementCheckStartTime = System.currentTimeMillis();
            hasShownMovementWarning = false;
            startMovementDetectionTimer();
            Log.d(TAG, "Movement detection started - monitoring user location");
            return;
        }

        // Check if user has moved significantly
        float distanceFromStart = startLocationForMovement.distanceTo(currentLocation);
        if (distanceFromStart >= MINIMAL_MOVEMENT_THRESHOLD_METERS) {
            // User has moved significantly, reset the timer
            resetMovementDetection(currentLocation);
        }
    }

    private void startMovementDetectionTimer() {
        cancelMovementDetectionTimer();

        movementCheckRunnable = this::checkIfUserNeedsToMove;
        movementCheckHandler.postDelayed(movementCheckRunnable, MOVEMENT_CHECK_INTERVAL_MS);
    }

    private void checkIfUserNeedsToMove() {
        if (!isTracking || getContext() == null || !isAdded() || hasShownMovementWarning) {
            return;
        }

        // Check if 10 minutes have passed without significant movement
        long elapsedTime = System.currentTimeMillis() - movementCheckStartTime;
        if (elapsedTime >= MOVEMENT_CHECK_INTERVAL_MS) {
            showMovementWarning();
        }
    }

    private void showMovementWarning() {
        if (getContext() == null || !isAdded() || hasShownMovementWarning) return;

        hasShownMovementWarning = true;
        Log.d(TAG, "Showing movement warning - user hasn't moved significantly in 10 minutes");

        requireActivity().runOnUiThread(() -> {
            if (movementWarningDialog != null && movementWarningDialog.isShowing()) {
                movementWarningDialog.dismiss();
            }

            movementWarningDialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("🚶‍♂️ Pindah Lokasi!")
                    .setMessage("Anda sudah berada di lokasi yang sama selama 10 menit. " +
                            "Untuk plogging yang efektif, silakan bergerak ke area yang berbeda.\n\n" +
                            "Session akan otomatis berakhir dalam 10 detik jika tidak ada pergerakan.")
                    .setPositiveButton("Lanjutkan Plogging", (dialog, which) -> {
                        acknowledgeMovementWarning();
                    })
                    .setNegativeButton("Akhiri Session", (dialog, which) -> {
                        finishPloggingConfirmed();
                    })
                    .setCancelable(false)
                    .show();

            // Start auto-finish timer for movement warning
            startMovementWarningTimeout();
        });
    }

    private void startMovementWarningTimeout() {
        cancelMovementWarningTimeout();

        movementWarningTimeoutRunnable = this::autoFinishDueToNoMovement;
        movementCheckHandler.postDelayed(movementWarningTimeoutRunnable, MOVEMENT_WARNING_TIMEOUT_MS);
    }

    private void acknowledgeMovementWarning() {
        Log.d(TAG, "User acknowledged movement warning");
        cancelMovementWarningTimeout();
        
        // Reset movement detection with current location
        if (lastLocation != null) {
            resetMovementDetection(lastLocation);
        }
        
        showNetworkStatusMessage("✅ Lanjutkan plogging di area baru!", false);
    }

    private void autoFinishDueToNoMovement() {
        if (!isTracking || getContext() == null || !isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Auto-finishing session due to no movement response");

            showNetworkStatusMessage("⏰ Session berakhir otomatis - tidak ada pergerakan", true);

            // Dismiss movement warning dialog if still showing
            if (movementWarningDialog != null && movementWarningDialog.isShowing()) {
                movementWarningDialog.dismiss();
            }

            // Show final notification
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("⏰ Session Berakhir Otomatis")
                    .setMessage("Session plogging telah berakhir karena Anda tidak bergerak ke lokasi baru.\n\n" +
                            "Progress Anda telah disimpan.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        finishPloggingConfirmed();
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    private void resetMovementDetection(Location newStartLocation) {
        Log.d(TAG, "Resetting movement detection - user has moved significantly");
        startLocationForMovement = newStartLocation;
        movementCheckStartTime = System.currentTimeMillis();
        hasShownMovementWarning = false;
        cancelMovementWarningTimeout();
        startMovementDetectionTimer();
    }

    private void cancelMovementDetectionTimer() {
        if (movementCheckHandler != null && movementCheckRunnable != null) {
            movementCheckHandler.removeCallbacks(movementCheckRunnable);
            movementCheckRunnable = null;
        }
    }

    private void cancelMovementWarningTimeout() {
        if (movementCheckHandler != null && movementWarningTimeoutRunnable != null) {
            movementCheckHandler.removeCallbacks(movementWarningTimeoutRunnable);
            movementWarningTimeoutRunnable = null;
        }
    }    private void stopMovementDetection() {
        Log.d(TAG, "Stopping movement detection");
        cancelMovementDetectionTimer();
        cancelMovementWarningTimeout();
        
        if (movementWarningDialog != null && movementWarningDialog.isShowing()) {
            movementWarningDialog.dismiss();
            movementWarningDialog = null;
        }
        
        // Comprehensive reset of all movement detection variables
        startLocationForMovement = null;
        movementCheckStartTime = 0;
        hasShownMovementWarning = false;
        
        // Clear any remaining callbacks from handler
        if (movementCheckHandler != null) {
            movementCheckHandler.removeCallbacksAndMessages(null);
        }
        
        // Reset runnable variables to null
        movementCheckRunnable = null;
        movementWarningTimeoutRunnable = null;
        
        Log.d(TAG, "Movement detection completely stopped and reset");
    }

    private void createDefaultUser() {
        executor.execute(() -> {
            try {
                // Check if any user exists first
                int userCount = db.userDao().getUserCount();
                if (userCount == 0) {
                    // Create default user
                    UserEntity defaultUser = new UserEntity();
                    defaultUser.setUsername("PloggingUser");
                    defaultUser.setFirstName("Plogging");
                    defaultUser.setLastName("User");
                    defaultUser.setEmail("user@glean.app");
                    defaultUser.setCreatedAt(System.currentTimeMillis());
                    defaultUser.setPoints(0);
                    
                    long newUserId = db.userDao().insert(defaultUser);
                    
                    // Save user ID to preferences
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                    prefs.edit().putInt("USER_ID", (int) newUserId).apply();
                    
                    // Also save to USER_PREFS
                    SharedPreferences userPrefs = requireActivity().getSharedPreferences("USER_PREFS", 0);
                    userPrefs.edit().putInt("USER_ID", (int) newUserId).apply();
                    
                    userId = (int) newUserId;
                    
                    Log.d(TAG, "Default user created with ID: " + newUserId);
                } else {
                    // Get first available user
                    UserEntity firstUser = db.userDao().getFirstUser();
                    if (firstUser != null) {
                        userId = firstUser.getId();
                        
                        // Save to preferences
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        prefs.edit().putInt("USER_ID", userId).apply();
                        
                        SharedPreferences userPrefs = requireActivity().getSharedPreferences("USER_PREFS", 0);
                        userPrefs.edit().putInt("USER_ID", userId).apply();
                        
                        Log.d(TAG, "Using existing user with ID: " + userId);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating default user", e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error initializing user", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void verifyUserExists() {
        executor.execute(() -> {
            try {
                UserEntity user = db.userDao().getUserByIdSync(userId);
                if (user == null) {
                    // User doesn't exist, create default user
                    Log.w(TAG, "User with ID " + userId + " not found, creating default user");
                    requireActivity().runOnUiThread(() -> createDefaultUser());
                } else {
                    Log.d(TAG, "User verified: " + user.getUsername());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error verifying user", e);
                requireActivity().runOnUiThread(() -> createDefaultUser());
            }
        });
    }

    private void initializeGoogleServices() {
        try {
            // Initialize FusedLocationProviderClient
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
            
            // Check Google Play Services availability
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = apiAvailability.isGooglePlayServicesAvailable(requireContext());
            
            if (resultCode == ConnectionResult.SUCCESS) {
                Log.d(TAG, "Google Play Services is available");
            } else if (apiAvailability.isUserResolvableError(resultCode)) {
                Log.w(TAG, "Google Play Services error (user resolvable): " + resultCode);
                // Could show dialog to user to resolve the issue
                apiAvailability.getErrorDialog(requireActivity(), resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.e(TAG, "Google Play Services not available: " + resultCode);
                Toast.makeText(requireContext(), "Google Play Services required for location features", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Google Services", e);
        }    }

    private void initializeCameraLaunchers() {
        // Initialize camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Camera permission granted");
                    launchCamera();
                } else {
                    Log.w(TAG, "Camera permission denied");
                    Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
            }
        );

        // Initialize camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "Photo captured successfully");
                    if (photoUri != null) {
                        savePloggingPhotoToDatabase();
                    }
                } else {
                    Log.w(TAG, "Photo capture cancelled or failed");
                    // Clean up the temporary file if photo capture failed
                    if (photoUri != null && currentPhotoPath != null) {
                        try {
                            java.io.File photoFile = new java.io.File(currentPhotoPath);
                            if (photoFile.exists()) {
                                photoFile.delete();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error cleaning up photo file", e);
                        }
                    }
                }
                // Reset photo capture variables
                photoUri = null;
                currentPhotoPath = null;
            }
        );
    }

    // Network monitoring methods
    private void checkNetworkStatus() {
        if (connectivityManager == null) return;
        
        boolean networkAvailable = false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                networkAvailable = capabilities != null && 
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
        } else {
            @SuppressWarnings("deprecation")
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            networkAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        
        if (networkAvailable != isNetworkAvailable) {
            isNetworkAvailable = networkAvailable;
            if (isNetworkAvailable) {
                onNetworkAvailable();
            } else {
                onNetworkLost();
            }
        }
    }
    
    private void registerNetworkCallbacks() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
                NetworkRequest.Builder builder = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
            } else if (networkReceiver != null) {
                IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                requireContext().registerReceiver(networkReceiver, filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering network callbacks", e);
        }
    }
    
    private void unregisterNetworkCallbacks() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } else if (networkReceiver != null) {
                try {
                    requireContext().unregisterReceiver(networkReceiver);
                } catch (IllegalArgumentException e) {
                    // Receiver was not registered
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering network callbacks", e);
        }
    }
    
    private void onNetworkAvailable() {
        Log.d(TAG, "Network connection available");
        enablePloggingFeatures();
        hideNetworkWarning();
        
        if (wasTrackingBeforeNetworkLoss && hasActiveSession() && isGpsEnabled) {
            showNetworkStatusMessage("🌐 Internet connection restored! You can resume tracking.", false);
        } else {
            updateUIForNetworkState(true);
            showNetworkStatusMessage("🌐 Internet Connected!", false);
        }
    }
    
    private void onNetworkLost() {
        Log.d(TAG, "Network connection lost");
        
        wasTrackingBeforeNetworkLoss = isTracking;
        
        if (isTracking) {
            internalPauseTracking();
            showNetworkStatusMessage("🌐 Internet connection lost! Tracking paused.", true);
        }
        
        // Disable plogging features when network is lost
        isPloggingEnabled = false;
        updateUIForNetworkState(false);
        
        showOfflineModeDialog();
    }
      private void hideNetworkWarning() {
        if (networkSnackbar != null) {
            networkSnackbar.dismiss();
            networkSnackbar = null;
        }
        
        if (networkDialog != null && networkDialog.isShowing()) {
            networkDialog.dismiss();
            networkDialog = null;
        }
        
        // Hide network warning indicator
        if (binding != null && binding.networkStatusIndicator != null) {
            binding.networkStatusIndicator.setVisibility(View.GONE);
        }
    }
    
    private void enableLimitedPloggingFeatures() {
        // Enable basic pllogging features for offline mode
        if (isGpsEnabled) {
            isPloggingEnabled = true;
            enablePloggingFeatures();
            showNetworkStatusMessage("📱 Offline mode enabled - basic features available", false);
        } else {
            showNetworkStatusMessage("📍 Location services still required for offline mode", true);
        }
    }
    
    // Auto-finish timer methods
    private void checkAndRestoreAutoFinishTimer() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        long networkLossTime = prefs.getLong("NETWORK_LOSS_TIME", 0);
        boolean wasAutoFinishActive = prefs.getBoolean("AUTO_FINISH_ACTIVE", false);
        
        if (wasAutoFinishActive && networkLossTime > 0) {
            long elapsedTime = System.currentTimeMillis() - networkLossTime;
            if (elapsedTime < AUTO_FINISH_TIMEOUT_MS) {
                // Continue the timer from where it left off
                long remainingTime = AUTO_FINISH_TIMEOUT_MS - elapsedTime;
                startAutoFinishTimer(remainingTime);
            } else {
                // Timer should have already expired, trigger auto-finish
                autoFinishDueToNetworkLoss();
            }
        }
    }
    
    private void startAutoFinishTimer(long timeoutMs) {
        cancelAutoFinishTimer();
        
        autoFinishRunnable = () -> {
            if (!isNetworkAvailable && hasActiveSession()) {
                autoFinishDueToNetworkLoss();
            }
        };
        
        // Set warning timer (1 minute before auto-finish)
        long warningTime = Math.max(timeoutMs - 60000, 0);
        if (warningTime > 0) {
            warningRunnable = () -> {
                if (!isNetworkAvailable && !hasShownWarning) {
                    hasShownWarning = true;
                    showNetworkStatusMessage("⚠️ Session will auto-finish in 1 minute due to no internet", true);
                }
            };
            autoFinishHandler.postDelayed(warningRunnable, warningTime);
        }
        
        autoFinishHandler.postDelayed(autoFinishRunnable, timeoutMs);
        
        // Save timer state
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit()
            .putLong("NETWORK_LOSS_TIME", System.currentTimeMillis())
            .putBoolean("AUTO_FINISH_ACTIVE", true)
            .apply();
    }
    
    private void cancelAutoFinishTimer() {
        if (autoFinishHandler != null) {
            if (autoFinishRunnable != null) {
                autoFinishHandler.removeCallbacks(autoFinishRunnable);
                autoFinishRunnable = null;
            }
            if (warningRunnable != null) {
                autoFinishHandler.removeCallbacks(warningRunnable);
                warningRunnable = null;
            }
        }
        
        hasShownWarning = false;
        networkLossStartTime = 0;
        
        // Clear timer state
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit()
            .putLong("NETWORK_LOSS_TIME", 0)
            .putBoolean("AUTO_FINISH_ACTIVE", false)
            .apply();
    }
      private void autoFinishDueToNetworkLoss() {
        if (hasActiveSession()) {
            showNetworkStatusMessage("⏰ Session auto-finished due to prolonged internet loss", true);
            finishPloggingConfirmed();
        }
        cancelAutoFinishTimer();
    }
}


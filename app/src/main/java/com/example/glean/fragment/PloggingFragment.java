package com.example.glean.fragment;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.databinding.FragmentPloggingBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.dao.DaoTrash;
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
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1004;

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
    private LocationCallback trackingCallback;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private BroadcastReceiver networkReceiver;
    private boolean isNetworkAvailable = false;
    private boolean isPloggingEnabled = false;
    private Snackbar networkSnackbar;
    private AlertDialog networkDialog;

    private View noInternetLayout;
    private Button btnRetryConnection;
    private Button btnOpenSettings;
    private Button btnContinueOffline;

    private int currentTrashCount = 0;
    private int currentPoints = 0;
    
    private boolean wasTrackingBeforeNetworkLoss = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        userId = prefs.getInt("USER_ID", -1);
        executor = Executors.newSingleThreadExecutor();
        
        initializeNetworkMonitoring();
        initializeGoogleServices();
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
        }

        restoreTrackingSession();

        updateUIForTrackingState(isTracking);
        updateUIForNetworkState(isNetworkAvailable);

        binding.btnStartStop.setOnClickListener(v -> toggleTracking());
        binding.btnCollectTrash.setOnClickListener(v -> navigateToTrashCollection());
        binding.btnFinish.setOnClickListener(v -> finishPlogging());

        checkNetworkStatus();
    }

    private void initializeNoInternetViews() {
        noInternetLayout = binding.layoutNoInternet.getRoot();
        btnRetryConnection = noInternetLayout.findViewById(R.id.btn_retry_connection);
        btnOpenSettings = noInternetLayout.findViewById(R.id.btn_open_settings);
        btnContinueOffline = noInternetLayout.findViewById(R.id.btn_continue_offline);

        btnRetryConnection.setOnClickListener(v -> {
            btnRetryConnection.setText("🔄 Checking...");
            btnRetryConnection.setEnabled(false);
            
            binding.getRoot().postDelayed(() -> {
                checkNetworkStatus();
                btnRetryConnection.setText("🔄 Try Again");
                btnRetryConnection.setEnabled(true);
            }, 1500);
        });

        btnOpenSettings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to open settings", Toast.LENGTH_SHORT).show();
            }
        });

        btnContinueOffline.setOnClickListener(v -> {
            showOfflineModeDialog();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        registerNetworkCallbacks();
        
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
    }

    private void updateTrashUIAlternative() {
        if (binding == null) return;
        
        if (binding.tvTrashCount != null) {
            binding.tvTrashCount.setText(String.valueOf(currentTrashCount));
        }
        
        if (binding.tvDistance != null) {
            if (currentTrashCount > 0 || currentPoints > 0) {
                String combinedText = String.format(Locale.getDefault(), 
                    "%.2f km | 🗑️ %d | ⭐ %d", 
                    totalDistance / 1000f, currentTrashCount, currentPoints);
                binding.tvDistance.setText(combinedText);
            } else {
                binding.tvDistance.setText(String.format(Locale.getDefault(), 
                    "%.2f km", totalDistance / 1000f));
            }
        }
        
        if (binding.btnCollectTrash != null) {
            if (isTracking) {
                String buttonText;
                if (currentTrashCount > 0) {
                    buttonText = String.format("🗑️ +%d pts", currentPoints);
                } else {
                    buttonText = "📸 Trash";
                }
                binding.btnCollectTrash.setText(buttonText);
            } else {
                binding.btnCollectTrash.setText("📸 Trash");
            }
        }
    }

    private void toggleTracking() {
        if (!isPloggingEnabled) {
            showNetworkStatusMessage("⚠️ Please enable internet connection first", true);
            checkNetworkStatus();
            return;
        }

        if (isTracking) {
            pauseTracking();
        } else {
            if (hasActiveSession()) {
                continueTracking();
            } else {
                startTracking();
            }
        }
    }

    private void startTracking() {
        if (!isPloggingEnabled) {
            showNetworkStatusMessage("⚠️ Internet connection required for plogging", true);
            return;
        }

        executor.execute(() -> {
            RecordEntity record = new RecordEntity();
            record.setUserId(userId);
            record.setStartTime(System.currentTimeMillis());
            record.setTotalDistance(0);
            record.setDuration(0);
            record.setTrashCount(0);

            long recordId = db.recordDao().insert(record);
            currentRecordId = (int) recordId;

            requireActivity().runOnUiThread(() -> {
                isTracking = true;
                
                currentTrashCount = 0;
                currentPoints = 0;
                totalDistance = 0f;
                
                saveTrackingSession(true, currentRecordId, System.currentTimeMillis(), 0f);
                
                updateUIForTrackingState(true);
                updateTrashUIAlternative();

                binding.chronometer.setBase(SystemClock.elapsedRealtime());
                binding.chronometer.start();

                startContinuousLocationTracking();

                String statusMessage = isNetworkAvailable ? 
                    "🏃‍♂️ Plogging started with full GPS accuracy!" :
                    "🏃‍♂️ Plogging started in offline mode!";
                    
                showNetworkStatusMessage(statusMessage, false);
            });
        });

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Intent serviceIntent = new Intent(requireContext(), LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_START_TRACKING);
            serviceIntent.putExtra("RECORD_ID", currentRecordId);
            requireActivity().startService(serviceIntent);
        }
    }

    private void pauseTracking() {
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

        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_STOP_TRACKING);
        requireActivity().startService(serviceIntent);
        
        showNetworkStatusMessage("⏸️ Plogging paused. Tap Continue to resume.", false);
    }

    private void stopTracking() {
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

        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_STOP_TRACKING);
        requireActivity().startService(serviceIntent);
        
        showNetworkStatusMessage("🛑 Plogging stopped completely.", false);
    }

    private void continueTracking() {
        if (currentRecordId == -1) {
            showNetworkStatusMessage("❌ No active session found", true);
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
            }

            startContinuousLocationTracking();

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
            pauseTracking();
        }

        if (currentRecordId != -1) {
            executor.execute(() -> {
                try {
                    RecordEntity record = db.recordDao().getRecordByIdSync(currentRecordId);
                    if (record != null) {
                        long elapsedTimeMillis = SystemClock.elapsedRealtime() - binding.chronometer.getBase();

                        int finalTrashCount = db.trashDao().getTrashCountByRecordIdSync(currentRecordId);
                        int finalPoints = db.trashDao().getTotalPointsByRecordIdSync(currentRecordId);

                        record.setDuration(elapsedTimeMillis);
                        record.setEndTime(System.currentTimeMillis());
                        record.setTotalDistance(totalDistance);
                        record.setTrashCount(finalTrashCount);
                        
                        db.recordDao().update(record);
                        
                        updateUserPoints(finalPoints);
                        
                        requireActivity().runOnUiThread(() -> {
                            stopTracking();
                            
                            String completionMessage = String.format(Locale.getDefault(),
                                "🎉 Plogging Complete!\n" +
                                "📍 Distance: %.2f km\n" +
                                "🗑️ Trash: %d items\n" +
                                "⭐ Points: %d",
                                totalDistance / 1000f, finalTrashCount, finalPoints);

                            new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Plogging Selesai")
                                .setMessage(completionMessage)
                                .setPositiveButton("OK", (dialog, which) -> {
                                    try {
                                        NavController navController = Navigation.findNavController(requireView());
                                        Bundle args = new Bundle();
                                        args.putInt("RECORD_ID", record.getId());
                                        navController.navigate(R.id.action_ploggingFragment_to_summaryFragment, args);
                                    } catch (Exception e) {
                                        Toast.makeText(requireContext(), completionMessage, Toast.LENGTH_LONG).show();
                                    }
                                })
                                .setNegativeButton("Tampilkan di Peta", (dialog, which) -> {
                                    showRouteOnMap(currentRecordId);
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
            executor.execute(() -> {
                try {
                    UserEntity user = db.userDao().getUserByIdSync(userId);
                    if (user != null) {
                        int currentPoints = user.getTotalPoints();
                        user.setTotalPoints(currentPoints + pointsEarned);
                        db.userDao().update(user);
                    }
                } catch (Exception e) {
                    
                }
            });
        }
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
                isTracking ? "resumed" : "available",
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
    }

    private void navigateToTrashCollection() {
        if (isTracking && currentRecordId != -1) {
            try {
                showNetworkStatusMessage("📸 Opening trash collection...", false);
                
                NavController navController = Navigation.findNavController(requireView());
                Bundle args = new Bundle();
                args.putInt("RECORD_ID", currentRecordId);
                args.putInt("CURRENT_TRASH_COUNT", currentTrashCount);
                args.putInt("CURRENT_POINTS", currentPoints);
                navController.navigate(R.id.action_ploggingFragment_to_trashMLFragment, args);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Trash collection feature temporarily unavailable", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Start plogging first", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUIForTrackingState(boolean tracking) {
        if (binding == null) return;
        
        if (tracking) {
            binding.btnStartStop.setText("Pause Tracking");
            binding.btnStartStop.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
            binding.btnCollectTrash.setEnabled(true);
            binding.btnFinish.setEnabled(true);
        } else {
            if (hasActiveSession()) {
                binding.btnStartStop.setText("Continue Plogging");
                binding.btnStartStop.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
            } else {
                String buttonText = isPloggingEnabled ? "Start Plogging" : "Internet Required";
                binding.btnStartStop.setText(buttonText);
                binding.btnStartStop.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            }
            binding.btnCollectTrash.setEnabled(false);
            binding.btnFinish.setEnabled(hasActiveSession());
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
                    .setSmallestDisplacement(1f);
                
            trackingCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null || !isTracking) return;
                    
                    Location location = locationResult.getLastLocation();
                    if (location != null && mMap != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        
                        routePoints.add(currentLatLng);
                        
                        addCurrentLocationMarker(currentLatLng, location.getAccuracy());
                        
                        if (lastLocation != null) {
                            float distance = lastLocation.distanceTo(location);
                            totalDistance += distance;

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideNetworkWarning();
        unregisterNetworkCallbacks();
        
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void updateUIForNetworkState(boolean networkAvailable) {
        if (binding == null) return;

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
    }

    private void enablePloggingFeatures() {
        isPloggingEnabled = true;
        
        if (binding != null) {
            binding.btnStartStop.setEnabled(true);
            binding.btnCollectTrash.setEnabled(isTracking);
            binding.btnFinish.setEnabled(hasActiveSession());
            
            updateUIForTrackingState(isTracking);
            
            binding.btnStartStop.setAlpha(1.0f);
            binding.btnCollectTrash.setAlpha(isTracking ? 1.0f : 0.6f);
            binding.btnFinish.setAlpha(hasActiveSession() ? 1.0f : 0.6f);
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

    private void initializeGoogleServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(requireContext());
        
        if (resultCode == ConnectionResult.SUCCESS) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        } else if (apiAvailability.isUserResolvableError(resultCode)) {
            apiAvailability.getErrorDialog(requireActivity(), resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
        } else {
            Toast.makeText(requireContext(), "Google Play Services required for location features", Toast.LENGTH_LONG).show();
        }
    }

    private void registerNetworkCallbacks() {
        if (connectivityManager == null) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
                NetworkRequest.Builder builder = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
                
                connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
            } else if (networkReceiver != null) {
                IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                requireContext().registerReceiver(networkReceiver, filter);
            }
        } catch (Exception e) {
            
        }
    }

    private void unregisterNetworkCallbacks() {
        try {
            if (connectivityManager != null && networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
            
            if (networkReceiver != null) {
                try {
                    requireContext().unregisterReceiver(networkReceiver);
                } catch (IllegalArgumentException e) {
                    
                }
            }
        } catch (Exception e) {
            
        }
    }

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
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            networkAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        
        if (networkAvailable != isNetworkAvailable) {
            if (networkAvailable) {
                onNetworkAvailable();
            } else {
                onNetworkLost();
            }
        }
    }

    private void onNetworkAvailable() {
        if (!isNetworkAvailable) {
            isNetworkAvailable = true;
            
            hideNoInternetScreen();
            hideNetworkWarning();
            enablePloggingFeatures();
            
            if (wasTrackingBeforeNetworkLoss && hasActiveSession()) {
                showNetworkStatusMessage("🌐 Internet reconnected! Your session is ready to continue.", false);
            } else {
                showNetworkStatusMessage("🌐 Internet Connected - Plogging Enabled!", false);
            }
            
            updateUIForNetworkState(true);
        }
    }

    private void onNetworkLost() {
        if (isNetworkAvailable) {
            isNetworkAvailable = false;
            wasTrackingBeforeNetworkLoss = isTracking;
            
            showNoInternetScreen();
            disablePloggingFeatures();
            updateUIForNetworkState(false);
        }
    }

    private void showNoInternetScreen() {
        if (binding != null) {
            binding.layoutPloggingContent.setVisibility(View.GONE);
            binding.layoutNoInternet.getRoot().setVisibility(View.VISIBLE);
        }
    }

    private void hideNoInternetScreen() {
        if (binding != null) {
            binding.layoutPloggingContent.setVisibility(View.VISIBLE);
            binding.layoutNoInternet.getRoot().setVisibility(View.GONE);
        }
    }

    private void disablePloggingFeatures() {
        isPloggingEnabled = false;
        
        if (isTracking) {
            isTracking = false;
            if (fusedLocationClient != null && trackingCallback != null) {
                fusedLocationClient.removeLocationUpdates(trackingCallback);
                trackingCallback = null;
            }
        }
    }

    private void enableLimitedPloggingFeatures() {
        isPloggingEnabled = true;
        
        hideNoInternetScreen();
        
        if (binding != null) {
            binding.btnStartStop.setEnabled(true);
            binding.btnCollectTrash.setEnabled(isTracking);
            binding.btnFinish.setEnabled(hasActiveSession());
            
            if (hasActiveSession()) {
                binding.btnStartStop.setText("Continue Plogging (Offline)");
                binding.btnStartStop.setOnClickListener(v -> {
                    if (isTracking) {
                        pauseTracking();
                    } else {
                        continueTracking();
                    }
                });
            } else {
                binding.btnStartStop.setText("Start Plogging (Offline)");
                binding.btnStartStop.setOnClickListener(v -> toggleTracking());
            }
            
            binding.btnStartStop.setAlpha(1.0f);
            binding.btnCollectTrash.setAlpha(isTracking ? 1.0f : 0.6f);
            binding.btnFinish.setAlpha(hasActiveSession() ? 1.0f : 0.6f);
        }
        
        Toast.makeText(requireContext(), "📱 Offline mode enabled - Limited functionality", Toast.LENGTH_LONG).show();
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null && ActivityCompat.checkSelfPermission(requireContext(), 
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    
                    if (fusedLocationClient == null) {
                        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
                    }
                    
                    requestUltraHighPrecisionLocation();
                }
                
                Toast.makeText(requireContext(), "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Location permission required for plogging", Toast.LENGTH_LONG).show();
                
                if (binding != null) {
                    binding.btnStartStop.setEnabled(false);
                    binding.btnStartStop.setText("Location Permission Required");
                }
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Storage permission required to save images", Toast.LENGTH_LONG).show();
            }
        }
    }
}
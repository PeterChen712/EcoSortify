package com.example.glean.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // Add this import
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.databinding.FragmentPloggingBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PloggingFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 1003;

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

    // Tambahkan variabel untuk marker lokasi saat ini
    private Marker currentLocationMarker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        userId = prefs.getInt("USER_ID", -1);
        executor = Executors.newSingleThreadExecutor();
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

        // Get map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set initial UI state
        updateUIForTrackingState(false);

        // Set click listeners
        binding.btnStartStop.setOnClickListener(v -> toggleTracking());
        binding.btnCollectTrash.setOnClickListener(v -> navigateToTrashCollection());
        binding.btnFinish.setOnClickListener(v -> finishPlogging());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        // Enable my location with enhanced UI settings
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        
        // Set map type ke NORMAL (bukan HYBRID) untuk tampilan standar
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setBuildingsEnabled(true); // Enable 3D buildings untuk detail
        mMap.setIndoorEnabled(true); // Enable indoor maps
        mMap.setTrafficEnabled(false); // Disable traffic untuk clarity
        
        // Set initial location dengan zoom sedang
        LatLng defaultLocation = new LatLng(-5.1477, 119.4327); // Makassar
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));
        
        // Langsung request lokasi presisi tinggi
        requestUltraHighPrecisionLocation();
    }

    private void requestUltraHighPrecisionLocation() {
        try {
            // Konfigurasi request lokasi dengan presisi maksimal
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(1000)  // 1 detik
                    .setFastestInterval(500)  // 0.5 detik
                    .setNumUpdates(10) // Ambil 10 reading untuk presisi terbaik
                    .setSmallestDisplacement(0.1f); // Update untuk pergerakan 10 cm
        
            LocationCallback ultraPrecisionCallback = new LocationCallback() {
                private int updateCount = 0;
                private Location bestLocation = null;
                private float bestAccuracy = Float.MAX_VALUE;
                
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null || locationResult.getLocations().isEmpty()) {
                        Log.w("PloggingFragment", "No location result");
                        return;
                    }
                    
                    for (Location location : locationResult.getLocations()) {
                        updateCount++;
                        
                        Log.d("PloggingFragment", String.format(
                            "Location Update #%d: Lat=%.8f, Lng=%.8f, Accuracy=%.2fm, Provider=%s",
                            updateCount, location.getLatitude(), location.getLongitude(),
                            location.getAccuracy(), location.getProvider()
                        ));
                        
                        // Simpan lokasi dengan akurasi terbaik
                        if (location.getAccuracy() < bestAccuracy) {
                            bestLocation = location;
                            bestAccuracy = location.getAccuracy();
                            
                            if (mMap != null) {
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                
                                // Zoom level 22 untuk detail maksimal tetapi tetap mode normal
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 22f), 2000, null);
                                
                                // Tambahkan marker lokasi saat ini
                                addCurrentLocationMarker(currentLatLng, location.getAccuracy());
                                
                                lastLocation = location;
                                
                                Log.d("PloggingFragment", String.format(
                                    "Updated to best location: Accuracy=%.2fm, Zoom=22 (Normal Mode)",
                                    location.getAccuracy()
                                ));
                            }
                        }
                        
                        // Berhenti jika sudah mendapat akurasi sangat tinggi (< 5 meter)
                        if (location.getAccuracy() < 5f && updateCount >= 3) {
                            Log.d("PloggingFragment", "High accuracy achieved, stopping updates");
                            fusedLocationClient.removeLocationUpdates(this);
                            break;
                        }
                        
                        // Atau berhenti setelah 10 updates
                        if (updateCount >= 10) {
                            Log.d("PloggingFragment", "Max updates reached, using best location");
                            fusedLocationClient.removeLocationUpdates(this);
                            break;
                        }
                    }
                }
            };
            
            // Coba getLastLocation terlebih dahulu
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null && location.getAccuracy() < 20f) {
                            // Jika ada lokasi cache yang cukup akurat, gunakan sebagai starting point
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 20f));
                            
                            // Tambahkan marker lokasi saat ini untuk cached location
                            addCurrentLocationMarker(currentLatLng, location.getAccuracy());
                            
                            lastLocation = location;
                            Log.d("PloggingFragment", "Using cached location as starting point");
                        }
                        
                        // Tetap request updates untuk presisi maksimal
                        if (ActivityCompat.checkSelfPermission(requireContext(), 
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.requestLocationUpdates(locationRequest, 
                                                                      ultraPrecisionCallback, 
                                                                      Looper.getMainLooper());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("PloggingFragment", "Failed to get cached location", e);
                        
                        // Langsung request updates
                        if (ActivityCompat.checkSelfPermission(requireContext(), 
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.requestLocationUpdates(locationRequest, 
                                                                      ultraPrecisionCallback, 
                                                                      Looper.getMainLooper());
                        }
                    });
                
        } catch (SecurityException e) {
            Log.e("PloggingFragment", "Security exception requesting location", e);
        }
    }

    // Method untuk menambahkan marker lokasi saat ini
    private void addCurrentLocationMarker(LatLng location, float accuracy) {
        if (mMap == null) return;
        
        // Hapus marker lokasi sebelumnya jika ada
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }
        
        // Buat marker baru untuk lokasi saat ini
        String title = "🚶 Posisi Saya";
        String snippet = String.format(Locale.getDefault(), 
            "Akurasi GPS: ±%.1fm\nSiap untuk plogging!", accuracy);
        
        currentLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)) // Warna hijau untuk plogging
                .anchor(0.5f, 0.5f)); // Center the marker
        
        Log.d("PloggingFragment", "Current location marker added for plogging at: " + 
              location.latitude + ", " + location.longitude);
    }

    // Method tambahan untuk continuous location tracking saat plogging
    private void startContinuousLocationTracking() {
        try {
            LocationRequest continuousRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(2000)  // 2 detik untuk tracking
                    .setFastestInterval(1000)  // 1 detik
                    .setSmallestDisplacement(1f); // Update setiap 1 meter
                
            LocationCallback trackingCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null || !isTracking) return;
                    
                    Location location = locationResult.getLastLocation();
                    if (location != null && mMap != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        
                        // Update route tracking
                        routePoints.add(currentLatLng);
                        
                        // Update marker lokasi saat ini selama tracking
                        addCurrentLocationMarker(currentLatLng, location.getAccuracy());
                        
                        // Calculate distance if we have a previous location
                        if (lastLocation != null) {
                            float distance = lastLocation.distanceTo(location);
                            totalDistance += distance;
                            
                            // Update UI dengan jarak total
                            requireActivity().runOnUiThread(() -> {
                                binding.tvDistance.setText(String.format(Locale.getDefault(), 
                                    "%.2f km", totalDistance / 1000f));
                            });
                        }
                        
                        lastLocation = location;
                        
                        Log.d("PloggingFragment", String.format(
                            "Tracking: Distance=%.2fm, Total=%.2fkm, Accuracy=%.2fm",
                            totalDistance, totalDistance/1000f, location.getAccuracy()
                        ));
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
            Log.e("PloggingFragment", "Error starting continuous tracking", e);
        }
    }

    private void getCurrentLocationForPlogging() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            // Zoom level 20 untuk view yang sangat detail (level jalan)
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 20f));
                            lastLocation = location;
                            Log.d("PloggingFragment", "High precision location: " + 
                                  location.getLatitude() + ", " + location.getLongitude() + 
                                  " Accuracy: " + location.getAccuracy() + "m");
                        } else {
                            requestLocationUpdates();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("PloggingFragment", "Failed to get location", e);
                        requestLocationUpdates();
                    });
        } catch (SecurityException e) {
            Log.e("PloggingFragment", "Security exception", e);
        }
    }

    private void requestLocationUpdates() {
        try {
            // Pengaturan lokasi dengan akurasi maksimal
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000); // 5 detik - lebih cepat
            locationRequest.setFastestInterval(2000); // 2 detik
            locationRequest.setNumUpdates(3); // Ambil 3 reading untuk akurasi yang lebih baik
            locationRequest.setSmallestDisplacement(1f); // Update jika bergerak 1 meter
            
            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) return;
                    
                    Location location = locationResult.getLastLocation();
                    if (location != null && mMap != null) {
                        // Filter lokasi berdasarkan akurasi (hanya terima jika akurasi < 50 meter)
                        if (location.getAccuracy() < 50f) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            // Zoom level 20 untuk detail maksimal
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 20f));
                            lastLocation = location;
                            Log.d("PloggingFragment", "High accuracy location updated: " + 
                                  location.getLatitude() + ", " + location.getLongitude() + 
                                  " Accuracy: " + location.getAccuracy() + "m");
                            
                            // Hapus updates setelah mendapat lokasi akurat
                            fusedLocationClient.removeLocationUpdates(this);
                        } else {
                            Log.d("PloggingFragment", "Location accuracy too low: " + location.getAccuracy() + "m");
                        }
                    }
                }
            };
            
            if (ActivityCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, 
                                                          locationCallback, 
                                                          Looper.getMainLooper());
            }
        } catch (SecurityException e) {
            Log.e("PloggingFragment", "Error requesting location updates", e);
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(requireContext(), "Location permission is needed for tracking", Toast.LENGTH_LONG).show();
        }

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void toggleTracking() {
        if (isTracking) {
            // Stop tracking
            stopTracking();
        } else {
            // Start tracking
            startTracking();
        }
    }

    private void startTracking() {
        isTracking = true;
        updateUIForTrackingState(true);

        // Start chronometer
        binding.chronometer.setBase(SystemClock.elapsedRealtime());
        binding.chronometer.start();

        // Create new record in database
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // Use the available constructor and set additional fields
        RecordEntity record = new RecordEntity(userId, currentDate, System.currentTimeMillis());
        
        // Set additional fields using setters
        record.setEndTime(0L);
        record.setDuration(0L);
        record.setTotalDistance(0f);
        record.setTrashCount(0);
        record.setNotes("Plogging Session");

        executor.execute(() -> {
            long id = db.recordDao().insert(record);
            currentRecordId = (int) id;
        });

        // Start location updates
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Implement location updates service
            // This would be a foreground service that tracks location changes
            Intent serviceIntent = new Intent(requireContext(), LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_START_TRACKING);
            serviceIntent.putExtra("RECORD_ID", currentRecordId);
            requireActivity().startService(serviceIntent);
        }
    }

    private void stopTracking() {
        isTracking = false;
        updateUIForTrackingState(false);

        // Stop chronometer
        binding.chronometer.stop();

        // Stop location updates
        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_STOP_TRACKING);
        requireActivity().startService(serviceIntent);
    }

    private void finishPlogging() {
        if (isTracking) {
            // Stop tracking first
            stopTracking();
        }

        // Update record with final statistics
        if (currentRecordId != -1) {
            executor.execute(() -> {
                // Use synchronous method instead of LiveData
                RecordEntity record = db.recordDao().getRecordByIdSync(currentRecordId);
                if (record != null) {
                    // Calculate elapsed time in milliseconds
                    long elapsedTimeMillis = SystemClock.elapsedRealtime() - binding.chronometer.getBase();

                    record.setDuration(elapsedTimeMillis);
                    record.setEndTime(System.currentTimeMillis());
                    record.setTotalDistance(totalDistance);
                    
                    // Calculate trash count and update record
                    int trashCount = db.trashDao().getTrashCountByRecordIdSync(currentRecordId);
                    record.setTrashCount(trashCount);
                    
                    // Update record using the update method
                    db.recordDao().update(record);
                    
                    // Update user statistics
                    requireActivity().runOnUiThread(() -> {
                        // Navigate to summary fragment
                        NavController navController = Navigation.findNavController(requireView());
                        Bundle args = new Bundle();
                        args.putInt("RECORD_ID", currentRecordId);
                        navController.navigate(R.id.action_ploggingFragment_to_summaryFragment, args);
                    });
                }
            });
        }
    }

    private void navigateToTrashCollection() {
        // Navigate to trash classification fragment
        if (isTracking && currentRecordId != -1) {
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putInt("RECORD_ID", currentRecordId);
            navController.navigate(R.id.action_ploggingFragment_to_trashMLFragment, args);
        } else {
            Toast.makeText(requireContext(), "Start plogging first", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUIForTrackingState(boolean tracking) {
        if (tracking) {
            binding.btnStartStop.setText("Pause");
            binding.btnCollectTrash.setEnabled(true);
            binding.btnFinish.setEnabled(true);
        } else {
            binding.btnStartStop.setText("Start");
            binding.btnCollectTrash.setEnabled(false);
            binding.btnFinish.setEnabled(currentRecordId != -1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize map
                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void initializeGoogleServices() {
        try {
            // Check Google Play Services availability first
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = apiAvailability.isGooglePlayServicesAvailable(requireContext());
            
            if (resultCode == ConnectionResult.SUCCESS) {
                // Initialize location services only if Play Services is available
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
                Log.d("PloggingFragment", "Google Play Services initialized successfully");
            } else {
                Log.e("PloggingFragment", "Google Play Services not available: " + resultCode);
                if (apiAvailability.isUserResolvableError(resultCode)) {
                    apiAvailability.getErrorDialog(requireActivity(), resultCode, 
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
                } else {
                    showFallbackMessage();
                }
            }
        } catch (Exception e) {
            Log.e("PloggingFragment", "Critical error initializing Google services", e);
            showFallbackMessage();
        }
    }

    private void showFallbackMessage() {
        if (getContext() != null) {
            new AlertDialog.Builder(requireContext())
                .setTitle("Location Services Unavailable")
                .setMessage("This device doesn't support the required location services. Some features may be limited.")
                .setPositiveButton("Continue", (dialog, which) -> {
                    // Continue with limited functionality
                    setupFallbackMode();
                })
                .setNegativeButton("Exit", (dialog, which) -> {
                    requireActivity().finish();
                })
                .show();
        }
    }

    private void setupFallbackMode() {
        // Implement fallback functionality without Google Play Services
        binding.btnStartStop.setEnabled(false);
        binding.btnStartStop.setText("Location Services Required");
        
        // Also disable other buttons that require location services
        binding.btnCollectTrash.setEnabled(false);
        binding.btnFinish.setEnabled(false);
        
        Toast.makeText(requireContext(), "Running in limited mode", Toast.LENGTH_LONG).show();
    }
}
package com.example.glean.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.databinding.FragmentTrashMapBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.TrashEntity;
import com.example.glean.config.ApiConfig;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrashMapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private FragmentTrashMapBinding binding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private AppDatabase db;
    private ExecutorService executor;
    private Map<Marker, TrashEntity> markerTrashMap = new HashMap<>();
    private String currentFilter = "All";    // Add these missing variables
    private Marker currentLocationMarker;
    private LocationCallback locationCallback; // Add this line
    private LocationCallback ultraDetailLocationCallback; // Callback for ultra detail location requests
    private LocationCallback newLocationCallback; // Callback for new location requests

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTrashMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get map fragment
        initializeMap();

        // Set click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.fabFilter.setOnClickListener(v -> showFilterOptions());
        
        // Tambahkan listener untuk test:
        // TEMPORARY DEBUG BUTTON
        binding.fabFilter.setOnLongClickListener(v -> {
            // Test marker dengan lokasi default
            LatLng testLocation = new LatLng(-5.1477, 119.4327);
            addCurrentLocationMarker(testLocation, 10.0f);
            debugMarkerStatus();
            Toast.makeText(requireContext(), "Test marker added!", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        // Tambahkan kontrol detail level (jika ada di layout)
        // binding.btnZoomDetail1.setOnClickListener(v -> zoomToDetailLevel(1));
        // binding.btnZoomDetail2.setOnClickListener(v -> zoomToDetailLevel(2));
        // binding.btnZoomDetail3.setOnClickListener(v -> zoomToDetailLevel(3));
        // binding.btnZoomDetail4.setOnClickListener(v -> zoomToDetailLevel(4));
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.trash_map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // You can verify the API key is loaded correctly (for debugging)
        Log.d("TrashMapFragment", "Using Maps API Key: " + ApiConfig.getMapsApiKey().substring(0, 5) + "...");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set info window click listener
        mMap.setOnInfoWindowClickListener(this);

        // Enable UI controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        
        // Set map type ke NORMAL (bukan satelit atau hybrid)
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setBuildingsEnabled(true); // Enable 3D buildings untuk detail
        mMap.setIndoorEnabled(true); // Enable indoor maps
        mMap.setTrafficEnabled(false); // Disable traffic untuk clarity
    
        // Check for location permission
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        // Enable my location button
        mMap.setMyLocationEnabled(true);
        
        // Set initial location dengan zoom sedang
        LatLng defaultLocation = new LatLng(-5.1477, 119.4327);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));

        // Request lokasi dengan presisi ultra tinggi
        requestUltraDetailLocation();

        // Load trash points
        loadTrashPoints();
    }

    private void requestUltraDetailLocation() {
        try {
            // Konfigurasi untuk presisi maksimal
            LocationRequest ultraDetailRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(500)   // 0.5 detik - sangat cepat
                    .setFastestInterval(250) // 0.25 detik - ultra cepat
                    .setNumUpdates(15)  // 15 reading untuk presisi terbaik
                    .setSmallestDisplacement(0.05f); // Update untuk pergerakan 5 cm
            
            ultraDetailLocationCallback = new LocationCallback() {
                private int updateCount = 0;
                private Location bestLocation = null;
                private float bestAccuracy = Float.MAX_VALUE;
                private List<Location> locationHistory = new ArrayList<>();
                  @Override
                public void onLocationResult(LocationResult locationResult) {
                    // Safety check: ensure fragment is still attached
                    if (!isAdded() || getContext() == null) {
                        Log.w("TrashMapFragment", "Location result received but fragment is detached - ignoring");
                        return;
                    }
                    
                    if (locationResult == null || locationResult.getLocations().isEmpty()) {
                        Log.w("TrashMapFragment", "No location in result");
                        return;
                    }
                    
                    for (Location location : locationResult.getLocations()) {
                        updateCount++;
                        locationHistory.add(location);
                        
                        Log.d("TrashMapFragment", String.format(
                            "Ultra Detail Update #%d: Lat=%.10f, Lng=%.10f, Accuracy=%.3fm, Speed=%.2fm/s",
                            updateCount, location.getLatitude(), location.getLongitude(),
                            location.getAccuracy(), location.getSpeed()
                        ));
                        
                        // Filter berdasarkan akurasi dan konsistensi
                        if (isLocationReliable(location) && location.getAccuracy() < bestAccuracy) {
                            bestLocation = location;
                            bestAccuracy = location.getAccuracy();
                            
                            if (mMap != null) {
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                
                                // Zoom level 22 untuk detail maksimal (individual buildings)
                                mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 22f), 
                                    1500, // Animasi 1.5 detik
                                    new GoogleMap.CancelableCallback() {
                                        @Override
                                        public void onFinish() {
                                            Log.d("TrashMapFragment", "Camera animation completed at max zoom");
                                            
                                            // Tambahkan atau update marker lokasi saat ini SETELAH animasi selesai
                                            addCurrentLocationMarker(currentLatLng, location.getAccuracy());
                                            
                                            // Debug marker status
                                            debugMarkerStatus();
                                        }
                                        
                                        @Override
                                        public void onCancel() {
                                            Log.d("TrashMapFragment", "Camera animation cancelled");
                                            
                                            // Tetap tambahkan marker meski animasi dibatalkan
                                            addCurrentLocationMarker(currentLatLng, location.getAccuracy());
                                            debugMarkerStatus();
                                        }
                                    }
                                );
                                
                                Log.d("TrashMapFragment", String.format(
                                    "Ultra detail location set: Accuracy=%.3fm, Zoom=22, Provider=%s",
                                    location.getAccuracy(), location.getProvider()
                                ));
                            }
                        }
                        
                        // Stop jika akurasi sangat tinggi (< 3 meter)
                        if (location.getAccuracy() < 3f && updateCount >= 5) {
                            Log.d("TrashMapFragment", "Ultra high accuracy achieved!");
                            fusedLocationClient.removeLocationUpdates(this);
                            
                            // Tampilkan informasi final
                            showLocationInfo(bestLocation != null ? bestLocation : location);
                            break;
                        }
                        
                        // Stop setelah 15 updates
                        if (updateCount >= 15) {
                            Log.d("TrashMapFragment", "Max updates reached, using best available location");
                            fusedLocationClient.removeLocationUpdates(this);
                            
                            if (bestLocation != null) {
                                showLocationInfo(bestLocation);
                                
                                // Pastikan marker lokasi saat ini ditambahkan
                                if (mMap != null) {
                                    LatLng finalLocation = new LatLng(bestLocation.getLatitude(), bestLocation.getLongitude());
                                    addCurrentLocationMarker(finalLocation, bestLocation.getAccuracy());
                                }
                            }
                            break;
                        }
                    }
                }
            };
            
            // Multi-step location acquisition
            // Step 1: Coba getLastLocation
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), cachedLocation -> {
                        if (cachedLocation != null) {
                            Log.d("TrashMapFragment", String.format(
                                "Cached location: Accuracy=%.2fm, Age=%ds",
                                cachedLocation.getAccuracy(),
                                (System.currentTimeMillis() - cachedLocation.getTime()) / 1000
                            ));
                            
                            // Jika lokasi cache masih fresh (< 30 detik) dan akurat (< 50m)
                            if (cachedLocation.getAccuracy() < 50f && 
                                (System.currentTimeMillis() - cachedLocation.getTime()) < 30000) {
                                
                                LatLng cachedLatLng = new LatLng(cachedLocation.getLatitude(), cachedLocation.getLongitude());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cachedLatLng, 20f));
                                
                                // Tambahkan marker untuk lokasi cache
                                addCurrentLocationMarker(cachedLatLng, cachedLocation.getAccuracy());
                            }
                        }                        // Step 2: Request ultra detail updates
                        if (isAdded() && getContext() != null && 
                            ActivityCompat.checkSelfPermission(getContext(), 
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.requestLocationUpdates(ultraDetailRequest, 
                                                                      ultraDetailLocationCallback,
                                                                      Looper.getMainLooper());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TrashMapFragment", "Failed to get cached location", e);                        // Langsung ke ultra detail updates
                        if (isAdded() && getContext() != null && 
                            ActivityCompat.checkSelfPermission(getContext(), 
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.requestLocationUpdates(ultraDetailRequest, 
                                                                      ultraDetailLocationCallback,
                                                                      Looper.getMainLooper());
                        }
                    });
                
        } catch (SecurityException e) {
            Log.e("TrashMapFragment", "Security exception in ultra detail location", e);
        }
    }

    // Method untuk menambahkan marker lokasi saat ini
    private void addCurrentLocationMarker(LatLng location, float accuracy) {
        if (mMap == null) {
            Log.w("TrashMapFragment", "Map is null, cannot add current location marker");
            return;
        }
        
        if (location == null) {
            Log.w("TrashMapFragment", "Location is null, cannot add current location marker");
            return;
        }
        
        try {
            // Hapus marker lokasi sebelumnya jika ada
            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
                Log.d("TrashMapFragment", "Removed previous current location marker");
            }
            
            // Buat marker baru untuk lokasi saat ini
            String title = "📍 Lokasi Saya";
            String snippet = String.format(Locale.getDefault(), 
                "Akurasi: ±%.1fm\nLat: %.6f\nLng: %.6f", 
                accuracy, location.latitude, location.longitude);
            
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(location)
                    .title(title)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) // Warna biru untuk lokasi saya
                    .anchor(0.5f, 0.5f) // Center the marker
                    .draggable(false) // Tidak bisa di-drag
                    .flat(false) // 3D marker
                    .visible(true); // Pastikan marker terlihat
        
            currentLocationMarker = mMap.addMarker(markerOptions);
            
            if (currentLocationMarker != null) {
                Log.d("TrashMapFragment", String.format(
                    "✅ Current location marker successfully added at: %.6f, %.6f with accuracy ±%.1fm",
                    location.latitude, location.longitude, accuracy
                ));
                
                // Pastikan marker terlihat dengan menampilkan info window sebentar
                currentLocationMarker.showInfoWindow();
                
                // Sembunyikan info window setelah 2 detik
                new android.os.Handler().postDelayed(() -> {
                    if (currentLocationMarker != null) {
                        currentLocationMarker.hideInfoWindow();
                    }
                }, 2000);
            } else {
                Log.e("TrashMapFragment", "❌ Failed to add current location marker - marker is null");
            }
            
        } catch (Exception e) {
            Log.e("TrashMapFragment", "❌ Exception while adding current location marker", e);
        }
    }

    private void loadTrashPoints() {
        // Use LiveData properly
        db.trashDao().getAllTrash().observe(getViewLifecycleOwner(), trashList -> {
            if (trashList != null && !trashList.isEmpty()) {
                updateMapMarkers(trashList);
            } else {
                // Jangan clear semua marker, hanya clear marker sampah
                if (mMap != null) {
                    // Simpan referensi current location marker
                    LatLng currentLocationPosition = null;
                    float currentLocationAccuracy = 0f;
                    if (currentLocationMarker != null) {
                        currentLocationPosition = currentLocationMarker.getPosition();
                        String snippet = currentLocationMarker.getSnippet();
                        if (snippet != null && snippet.contains("Akurasi:")) {
                            try {
                                String accuracyStr = snippet.substring(snippet.indexOf("±") + 1, snippet.indexOf("m"));
                                currentLocationAccuracy = Float.parseFloat(accuracyStr);
                            } catch (Exception e) {
                                currentLocationAccuracy = 10f;
                            }
                        }
                    }
                    
                    // Clear map dan marker trash
                    mMap.clear();
                    markerTrashMap.clear();
                    currentLocationMarker = null;
                    
                    // Tambahkan kembali current location marker jika ada
                    if (currentLocationPosition != null) {
                        addCurrentLocationMarker(currentLocationPosition, currentLocationAccuracy);
                    }
                }
            }
        });
    }
    
    private void filterTrashPoints(String filterType) {
        currentFilter = filterType;
        
        if (filterType.equals("All")) {
            // Load all trash points
            db.trashDao().getAllTrash().observe(getViewLifecycleOwner(), trashList -> {
                if (trashList != null) {
                    updateMapMarkers(trashList);
                }
            });
        } else {
            // Load filtered trash points
            db.trashDao().getTrashByType(filterType).observe(getViewLifecycleOwner(), trashList -> {
                if (trashList != null) {
                    updateMapMarkers(trashList);
                } else {
                    // Clear map if no filtered data
                    if (mMap != null) {
                        mMap.clear();
                        markerTrashMap.clear();
                    }
                }
            });
        }
    }

    private void updateMapMarkers(List<TrashEntity> trashList) {
        if (mMap == null) return;
        
        // Simpan referensi marker lokasi saat ini sebelum clear
        LatLng currentLocationPosition = null;
        float currentLocationAccuracy = 0f;
        if (currentLocationMarker != null) {
            currentLocationPosition = currentLocationMarker.getPosition();
            String snippet = currentLocationMarker.getSnippet();
            if (snippet != null && snippet.contains("Akurasi:")) {
                try {
                    String accuracyStr = snippet.substring(snippet.indexOf("±") + 1, snippet.indexOf("m"));
                    currentLocationAccuracy = Float.parseFloat(accuracyStr);
                } catch (Exception e) {
                    currentLocationAccuracy = 10f; // Default accuracy
                }
            }
        }
        
        // Clear semua marker
        mMap.clear();
        markerTrashMap.clear();
        currentLocationMarker = null;

        // Tambahkan kembali marker lokasi saat ini jika ada
        if (currentLocationPosition != null) {
            addCurrentLocationMarker(currentLocationPosition, currentLocationAccuracy);
        }

        // Tambahkan marker sampah
        for (TrashEntity trash : trashList) {
            if (trash.getLatitude() != 0 && trash.getLongitude() != 0) {
                LatLng position = new LatLng(trash.getLatitude(), trash.getLongitude());
                
                // Set marker color based on trash type
                float markerColor = getMarkerColorForTrashType(trash.getTrashType());
                  String title = trash.getTrashType() != null ? trash.getTrashType() : getString(R.string.unknown);
                String snippet = getString(R.string.tap_for_details);
                
                // Add additional info to snippet if available
                if (trash.getDescription() != null && !trash.getDescription().isEmpty()) {
                    snippet = trash.getDescription();
                }
                
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(title)
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
                
                if (marker != null) {
                    markerTrashMap.put(marker, trash);
                }
            }
        }
        
        // TAMBAHKAN INI: Pastikan marker lokasi saat ini tetap ada setelah update
        if (currentLocationPosition != null) {
            addCurrentLocationMarker(currentLocationPosition, currentLocationAccuracy);
        }
    }

    private float getMarkerColorForTrashType(String trashType) {
        if (trashType == null) return BitmapDescriptorFactory.HUE_RED;
        
        switch (trashType.toLowerCase()) {
            case "plastic":
            case "plastic bottle":
            case "plastic bag":
                return BitmapDescriptorFactory.HUE_YELLOW;
            case "paper":
                return BitmapDescriptorFactory.HUE_BLUE;
            case "glass":
                return BitmapDescriptorFactory.HUE_GREEN;
            case "metal":
            case "metal can":
                return BitmapDescriptorFactory.HUE_ORANGE;
            case "organic":
            case "food waste":
                return BitmapDescriptorFactory.HUE_MAGENTA;
            case "electronic":
                return BitmapDescriptorFactory.HUE_CYAN;
            case "hazardous":
                return BitmapDescriptorFactory.HUE_VIOLET;
            case "cigarette butt":
                return BitmapDescriptorFactory.HUE_ROSE;
            default:
                return BitmapDescriptorFactory.HUE_RED;
        }
    }

    private void showFilterOptions() {
        String[] trashTypes = {"All", "Plastic", "Paper", "Glass", "Metal", "Organic", "Electronic", "Hazardous", "Cigarette Butt", "Other"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Filter Trash By Type");
        builder.setItems(trashTypes, (dialog, which) -> {
            String selectedType = trashTypes[which];
            filterTrashPoints(selectedType);
            Toast.makeText(requireContext(), "Showing " + selectedType + " trash", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        // Cek apakah marker yang diklik adalah marker lokasi saat ini
        if (marker.equals(currentLocationMarker)) {
            // Tampilkan toast dengan informasi lokasi detail
            Toast.makeText(requireContext(), 
                "📍 Ini lokasi Anda saat ini", 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Handle marker sampah
        TrashEntity trash = markerTrashMap.get(marker);
        if (trash != null) {
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putInt("TRASH_ID", (int)trash.getId());
            navController.navigate(R.id.action_trashMapFragment_to_trashDetailFragment, args);
        }
    }

    // Method untuk validasi reliabilitas lokasi
    private boolean isLocationReliable(Location location) {
        // Filter lokasi yang tidak reliable
        if (location.getAccuracy() > 100f) return false; // Akurasi terlalu rendah
        if (location.getSpeed() > 50f) return false; // Kecepatan tidak masuk akal untuk pejalan kaki
        if (!location.hasAccuracy()) return false; // Tidak ada informasi akurasi
        
        // Tambahan validasi berdasarkan provider
        String provider = location.getProvider();
        if (provider != null && provider.equals("passive")) return false; // Hindari passive provider
        
        return true;
    }    // Method untuk menampilkan informasi lokasi detail
    private void showLocationInfo(Location location) {
        if (location == null) return;
        
        // Safety check: ensure fragment is still attached
        if (!isAdded() || getContext() == null) {
            Log.w("TrashMapFragment", "Fragment not attached, skipping location info display");
            return;
        }
        
        String locationInfo = String.format(Locale.getDefault(),
            "📍 Ultra Detail Location\n" +
            "Lat: %.8f\n" +
            "Lng: %.8f\n" +
            "Accuracy: ±%.2fm\n" +
            "Provider: %s\n" +
            "Altitude: %.1fm",
            location.getLatitude(),
            location.getLongitude(),
            location.getAccuracy(),
            location.getProvider(),
            location.getAltitude()
        );
        
        Log.d("TrashMapFragment", locationInfo);
        
        // Tampilkan toast dengan info singkat - use getContext() instead of requireContext()
        if (getContext() != null) {
            Toast.makeText(getContext(), 
                String.format("📍 Location: ±%.1fm accuracy", location.getAccuracy()), 
                Toast.LENGTH_SHORT).show();
        }
    }

    // Method untuk zoom ke level detail tertentu
    private void zoomToDetailLevel(int detailLevel) {
        if (mMap == null) return;
        
        LatLng currentCenter = mMap.getCameraPosition().target;
        float zoomLevel;
        
        switch (detailLevel) {
            case 1: // Neighborhood level
                zoomLevel = 18f;
                break;
            case 2: // Street level  
                zoomLevel = 20f;
                break;
            case 3: // Building level
                zoomLevel = 21f;
                break;
            case 4: // Ultra detail level
                zoomLevel = 22f;
                break;
            default:
                zoomLevel = 19f;
        }
        
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentCenter, zoomLevel), 2000, null);
        
        Log.d("TrashMapFragment", "Zoomed to detail level " + detailLevel + " (zoom " + zoomLevel + ")");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Log.d("TrashMapFragment", "Location permission granted");
                
                if (mMap != null) {
                    try {
                        if (ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            
                            mMap.setMyLocationEnabled(true);
                            
                            // Start location tracking
                            requestUltraDetailLocation();
                            
                            Toast.makeText(requireContext(), 
                                "📍 Location access granted! Finding your position...", 
                                Toast.LENGTH_SHORT).show();
                        }
                    } catch (SecurityException e) {
                        Log.e("TrashMapFragment", "Security exception after permission granted", e);
                    }
                }
            } else {
                // Permission denied
                Log.w("TrashMapFragment", "Location permission denied");
                
                // Check if user selected "Don't ask again"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    
                    // User selected "Don't ask again" - show settings dialog
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Location Permission Required")
                            .setMessage("Location access has been permanently denied. To enable location features, please go to Settings > Apps > Glean > Permissions and enable Location.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                try {
                                    android.content.Intent intent = new android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    android.net.Uri uri = android.net.Uri.fromParts("package", 
                                        requireContext().getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    Log.e("TrashMapFragment", "Error opening settings", e);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    // User just denied this time
                    Toast.makeText(requireContext(), 
                        "Location permission is required for full map functionality", 
                        Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    private void requestLocationPermission() {
        try {
            // Check if we should show rationale
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                
                // Show explanation dialog
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("📍 Permission Required")
                        .setMessage("Glean needs location access to:\n\n" +
                                  "• Show your current position on the map\n" +
                                  "• Help you find nearby trash points\n" +
                                  "• Enable accurate trash reporting\n\n" +
                                  "Your location data stays on your device.")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            requestPermissions();
                        })
                        .setNegativeButton("Not Now", (dialog, which) -> {
                            Toast.makeText(requireContext(), 
                                "Map functionality will be limited without location access", 
                                Toast.LENGTH_LONG).show();
                        })
                        .setCancelable(false)
                        .show();
            } else {
                // Directly request permission
                requestPermissions();
            }
        } catch (Exception e) {
            Log.e("TrashMapFragment", "Error requesting location permission", e);
            Toast.makeText(requireContext(), "Error requesting location permission", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method untuk request permissions
    private void requestPermissions() {
        try {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            Log.e("TrashMapFragment", "Error in requestPermissions", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();        try {
            // Stop location updates
            if (fusedLocationClient != null) {
                if (locationCallback != null) {
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                    locationCallback = null;
                }
                if (ultraDetailLocationCallback != null) {
                    fusedLocationClient.removeLocationUpdates(ultraDetailLocationCallback);
                    ultraDetailLocationCallback = null;
                }
                if (newLocationCallback != null) {
                    fusedLocationClient.removeLocationUpdates(newLocationCallback);
                    newLocationCallback = null;
                }
            }
            
            // Clear map resources
            if (mMap != null) {
                mMap.clear();
                mMap.setOnMapClickListener(null);
                mMap.setOnInfoWindowClickListener(null);
                mMap = null;
            }
            
            // Clear binding
            binding = null;
            
        } catch (Exception e) {
            Log.e("Fragment", "Error in cleanup", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        try {
            // Shutdown executors
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            Log.e("Fragment", "Error shutting down executor", e);
        }
    }

    private void requestNewLocation() {
        try {
            // Konfigurasi request lokasi dengan akurasi tinggi
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(3000)  // 3 detik
                    .setFastestInterval(1000)  // 1 detik
                    .setNumUpdates(5) // Ambil beberapa reading untuk akurasi terbaik
                    .setSmallestDisplacement(0.5f); // Update jika bergerak 0.5 meter
                
            newLocationCallback = new LocationCallback() {
                private int updateCount = 0;
                private Location bestLocation = null;
                  @Override
                public void onLocationResult(LocationResult locationResult) {
                    // Safety check: ensure fragment is still attached
                    if (!isAdded() || getContext() == null) {
                        Log.w("TrashMapFragment", "New location result received but fragment is detached - ignoring");
                        return;
                    }
                    
                    if (locationResult == null || locationResult.getLocations().isEmpty()) {
                        Log.w("TrashMapFragment", "Location result is empty");
                        return;
                    }
                    
                    Location location = locationResult.getLocations().get(0);
                    updateCount++;
                    
                    Log.d("TrashMapFragment", String.format(
                        "New location update #%d: Lat=%.8f, Lng=%.8f, Accuracy=%.2fm",
                        updateCount, location.getLatitude(), location.getLongitude(), location.getAccuracy()
                    ));
                    
                    // Pilih lokasi dengan akurasi terbaik
                    if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
                        bestLocation = location;
                    }
                    
                    // Setelah beberapa update atau mendapat akurasi yang baik
                    if (updateCount >= 3 || location.getAccuracy() < 20f) {
                        if (bestLocation != null && mMap != null) {
                            LatLng currentLatLng = new LatLng(bestLocation.getLatitude(), bestLocation.getLongitude());
                            // Zoom level 19 untuk detail tinggi
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 19f));
                            Log.d("TrashMapFragment", String.format(
                                "Best location set: Lat=%.8f, Lng=%.8f, Accuracy=%.2fm after %d updates",
                                bestLocation.getLatitude(), bestLocation.getLongitude(),
                                bestLocation.getAccuracy(), updateCount
                            ));
                            
                            // Tampilkan info lokasi
                            showLocationInfo(bestLocation);
                        }
                        
                        // Stop location updates
                        fusedLocationClient.removeLocationUpdates(this);
                    }
                }
            };            // Request location updates
            if (ActivityCompat.checkSelfPermission(getContext(), 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, 
                                                         newLocationCallback,
                                                         Looper.getMainLooper());
            }} catch (SecurityException e) {
            Log.e("TrashMapFragment", "Error requesting new location", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.error_getting_location), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method untuk debug status marker
    private void debugMarkerStatus() {
        Log.d("TrashMapFragment", "=== MARKER DEBUG STATUS ===");
        Log.d("TrashMapFragment", "Map is null: " + (mMap == null));
        Log.d("TrashMapFragment", "Current location marker is null: " + (currentLocationMarker == null));
        Log.d("TrashMapFragment", "Trash markers count: " + markerTrashMap.size());
        
        if (currentLocationMarker != null) {
            Log.d("TrashMapFragment", "Current marker position: " + currentLocationMarker.getPosition());
            Log.d("TrashMapFragment", "Current marker visible: " + currentLocationMarker.isVisible());
            Log.d("TrashMapFragment", "Current marker title: " + currentLocationMarker.getTitle());
        }
        Log.d("TrashMapFragment", "=== END DEBUG ===");
    }
}

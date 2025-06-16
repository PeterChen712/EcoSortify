package com.example.glean.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.databinding.FragmentPloggingSummaryBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.LocationPointEntity;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.TrashEntity;
import com.example.glean.model.UserEntity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PloggingSummaryFragment extends Fragment implements OnMapReadyCallback {    private static final String TAG = "PloggingSummaryFragment";
      private FragmentPloggingSummaryBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private int recordId;
    private GoogleMap mMap;
    
    private int currentUserId;
    private RecordEntity currentRecord;
    private Location lastKnownLocation;
    private List<LocationPointEntity> routePoints;    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        // Get current user ID from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", requireContext().MODE_PRIVATE);
        currentUserId = prefs.getInt("current_user_id", -1);
        
        // Get record ID from arguments
        if (getArguments() != null) {
            recordId = getArguments().getInt("RECORD_ID", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPloggingSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        // Set click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnShare.setOnClickListener(v -> shareActivity());
        binding.btnShareCommunity.setOnClickListener(v -> shareToCommunitiy());
        binding.btnSaveToGallery.setOnClickListener(v -> savePloggingResultToGallery());
        binding.btnRetry.setOnClickListener(v -> loadActivityData());
        
        // Load activity data
        loadActivityData();
    }    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Enable zoom controls and proper map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        
        // Set appropriate map type for plogging (shows streets and details clearly)
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        
        // Set minimum zoom to prevent extreme world view
        mMap.setMinZoomPreference(10.0f);
        
        Log.d(TAG, "Google Map ready, loading route data for record ID: " + recordId);
        
        // Load route data when map is ready
        if (recordId != -1) {
            loadRouteData();
        } else {
            Log.w(TAG, "No valid record ID to load route data");
        }
    }

    private void loadActivityData() {
        if (recordId == -1) {
            showError("Invalid record ID");
            return;
        }

        showLoading(true);
        
        executor.execute(() -> {
            try {
                // Load record data synchronously
                RecordEntity record = db.recordDao().getRecordByIdSync(recordId);
                
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    
                    if (record != null) {
                        currentRecord = record;
                        displayActivityData(record);
                        showMainContent(true);
                    } else {
                        showError("Plogging record not found");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading activity data", e);
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showError("Failed to load plogging data: " + e.getMessage());
                });
            }
        });
    }    private void loadRouteData() {
        if (recordId == -1 || mMap == null) {
            Log.w(TAG, "Cannot load route data - recordId: " + recordId + ", mMap: " + (mMap != null));
            return;
        }

        Log.d(TAG, "🗺️  Loading route data for record ID: " + recordId);

        executor.execute(() -> {
            try {
                // First verify the record exists
                RecordEntity record = db.recordDao().getRecordByIdSync(recordId);
                if (record == null) {                    Log.e(TAG, "❌ CRITICAL: Record ID " + recordId + " does not exist in database!");
                    requireActivity().runOnUiThread(() -> {
                        hideMapLoading();
                        showMapError("No route data available for this plogging session");
                        updateRouteInfo(0);
                    });
                    return;
                }
                
                Log.d(TAG, "✅ Record exists: " + record.toString());
                Log.d(TAG, "   Distance in record: " + record.getDistance() + "m");
                Log.d(TAG, "   Duration: " + record.getDuration() + "ms");
                Log.d(TAG, "   User ID: " + record.getUserId());
                Log.d(TAG, "   Created: " + new java.util.Date(record.getCreatedAt()));
                
                // Load location points for the route
                List<LocationPointEntity> locationPoints = db.locationPointDao().getLocationPointsByRecordIdSync(recordId);
                
                Log.d(TAG, "📍 Query result: " + (locationPoints != null ? locationPoints.size() : 0) + " location points");
                
                if (locationPoints != null && !locationPoints.isEmpty()) {
                    Log.d(TAG, "✅ Location points found:");
                    for (int i = 0; i < Math.min(locationPoints.size(), 5); i++) {
                        LocationPointEntity point = locationPoints.get(i);
                        Log.d(TAG, "   Point " + (i+1) + ": (" + point.getLatitude() + ", " + point.getLongitude() + 
                              ") distance=" + point.getDistanceFromLast() + "m timestamp=" + new java.util.Date(point.getTimestamp()));
                    }
                    if (locationPoints.size() > 5) {
                        Log.d(TAG, "   ... and " + (locationPoints.size() - 5) + " more points");
                    }
                } else {
                    Log.w(TAG, "⚠️  NO LOCATION POINTS found for record " + recordId);
                    
                    // Additional debugging - check if there are ANY location points in the database
                    List<LocationPointEntity> allLocationPoints = db.locationPointDao().getAllLocationPointsSync();
                    Log.d(TAG, "   Total location points in entire database: " + allLocationPoints.size());
                    
                    if (!allLocationPoints.isEmpty()) {
                        Log.d(TAG, "   Sample location points from database:");
                        for (int i = 0; i < Math.min(allLocationPoints.size(), 3); i++) {
                            LocationPointEntity point = allLocationPoints.get(i);
                            Log.d(TAG, "     Point: recordId=" + point.getRecordId() + 
                                  " coords=(" + point.getLatitude() + ", " + point.getLongitude() + 
                                  ") timestamp=" + new java.util.Date(point.getTimestamp()));
                        }
                    }
                    
                    // Check if the query itself is working
                    int pointCountForRecord = db.locationPointDao().getLocationPointCountByRecordId(recordId);
                    Log.d(TAG, "   Direct count query for record " + recordId + ": " + pointCountForRecord + " points");
                }
                
                requireActivity().runOnUiThread(() -> {
                    hideMapLoading();
                    
                    if (locationPoints != null && !locationPoints.isEmpty()) {
                        routePoints = locationPoints;
                        displayRouteOnMap(locationPoints);
                        updateRouteInfo(locationPoints.size());
                        Log.d(TAG, "✅ Successfully displayed route with " + locationPoints.size() + " points");
                    } else {
                        Log.w(TAG, "No route data available for record " + recordId);
                        showMapError("No route data available for this plogging session");
                        // Show a default location if we have record data
                        if (currentRecord != null) {
                            // Try to center on a default location (you can customize this)
                            LatLng defaultLocation = new LatLng(-5.1356, 119.4215); // Example: Makassar
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading route data for record " + recordId, e);
                requireActivity().runOnUiThread(() -> {
                    hideMapLoading();
                    showMapError("Failed to load route: " + e.getMessage());
                });
            }
        });
    }

    private void displayRouteOnMap(List<LocationPointEntity> locationPoints) {
        if (mMap == null || locationPoints.isEmpty()) return;

        mMap.clear();

        // Convert to LatLng list and create polyline
        List<LatLng> routeLatLngs = new ArrayList<>();
        for (LocationPointEntity point : locationPoints) {
            routeLatLngs.add(new LatLng(point.getLatitude(), point.getLongitude()));
        }        // Add polyline for the route with better visibility
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(routeLatLngs)
                .width(12f)  // Thicker line for better visibility
                .color(getResources().getColor(R.color.primary_color))
                .geodesic(true)  // Better for long distances
                .pattern(null); // Solid line
        mMap.addPolyline(polylineOptions);

        // Add start marker (green) with custom icon
        if (routeLatLngs.size() > 0) {
            LatLng startPoint = routeLatLngs.get(0);
            mMap.addMarker(new MarkerOptions()
                    .position(startPoint)
                    .title("🚀 Start Point")
                    .snippet("Plogging dimulai di sini")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            
            Log.d(TAG, "Start point: " + startPoint.latitude + ", " + startPoint.longitude);
        }

        // Add finish marker (red) with custom icon
        if (routeLatLngs.size() > 1) {
            LatLng endPoint = routeLatLngs.get(routeLatLngs.size() - 1);
            mMap.addMarker(new MarkerOptions()
                    .position(endPoint)
                    .title("🏁 Finish Point")
                    .snippet("Plogging selesai di sini")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            
            Log.d(TAG, "Finish point: " + endPoint.latitude + ", " + endPoint.longitude);
        }

        // Add distance markers every 500m for longer routes
        if (routeLatLngs.size() > 10) {
            addDistanceMarkers(routeLatLngs);
        }// Fit camera to show entire route with proper zoom level
        if (routeLatLngs.size() > 1) {
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (LatLng point : routeLatLngs) {
                boundsBuilder.include(point);
            }
            LatLngBounds bounds = boundsBuilder.build();
            
            try {
                // Calculate the distance between northeast and southwest corners
                float[] results = new float[1];
                android.location.Location.distanceBetween(
                    bounds.southwest.latitude, bounds.southwest.longitude,
                    bounds.northeast.latitude, bounds.northeast.longitude,
                    results
                );
                float boundsDiagonalMeters = results[0];
                
                Log.d(TAG, "Route bounds diagonal distance: " + boundsDiagonalMeters + " meters");
                
                // If the route area is very small (less than 200m diagonal), use a fixed zoom
                if (boundsDiagonalMeters < 200) {
                    // Calculate center point of route
                    double centerLat = (bounds.northeast.latitude + bounds.southwest.latitude) / 2;
                    double centerLng = (bounds.northeast.longitude + bounds.southwest.longitude) / 2;
                    LatLng center = new LatLng(centerLat, centerLng);
                    
                    // Use zoom level 17 for small routes (good street-level detail)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 17f));
                    Log.d(TAG, "Using fixed zoom for small route area");
                } else {
                    // For larger routes, use bounds with appropriate padding
                    int padding = Math.max(150, (int)(boundsDiagonalMeters * 0.1)); // Dynamic padding
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                    Log.d(TAG, "Using bounds-based zoom with padding: " + padding);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fitting camera to bounds", e);
                // Fallback: center on first point with reasonable zoom
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(routeLatLngs.get(0), 16f));
            }
        } else if (routeLatLngs.size() == 1) {
            // Single point - use good street level zoom
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(routeLatLngs.get(0), 17f));
            Log.d(TAG, "Single point route - using zoom level 17");
        }
    }    private void updateRouteInfo(int pointCount) {
        String routeInfo = String.format(Locale.getDefault(), 
            "📍 Route with %d tracking points", pointCount);
        binding.tvRouteInfo.setText(routeInfo);
        binding.tvRouteInfo.setTextColor(getResources().getColor(android.R.color.black));
        Log.d(TAG, "Updated route info: " + routeInfo);
    }

    private void showMapError(String message) {
        binding.tvRouteInfo.setText("⚠️ " + message);
        binding.tvRouteInfo.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        Log.w(TAG, "Map error: " + message);
    }

    private void hideMapLoading() {
        binding.mapLoadingOverlay.setVisibility(View.GONE);
    }    private void displayActivityData(RecordEntity record) {
        // Set date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date(record.getCreatedAt()));
        binding.tvDate.setText(formattedDate);
        
        // Set location
        String location = record.getDescription() != null && !record.getDescription().isEmpty() 
            ? record.getDescription() 
            : "Plogging Session";
        binding.tvLocation.setText("📍 " + location);
        
        // Format start time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String startTime = sdf.format(new Date(record.getCreatedAt()));
        binding.tvStartTime.setText("Started at " + startTime);
        
        // Format distance
        float distanceKm = record.getDistance() / 1000f;
        binding.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));
        
        // Format duration
        long durationSeconds = record.getDuration() / 1000;
        int hours = (int) (durationSeconds / 3600);
        int minutes = (int) ((durationSeconds % 3600) / 60);
        int seconds = (int) (durationSeconds % 60);
        String durationStr;
        if (hours > 0) {
            durationStr = String.format(Locale.getDefault(), "%d hr %d min %d sec", hours, minutes, seconds);
        } else {
            durationStr = String.format(Locale.getDefault(), "%d min %d sec", minutes, seconds);
        }
        binding.tvDuration.setText(durationStr);
        
        // Set points
        binding.tvPoints.setText(String.valueOf(record.getPoints()));        // Set trash collected
        int trashCount = record.getPoints() / 10; // Assuming 10 points per trash item
        binding.tvTrashCollected.setText(String.valueOf(trashCount));
        
        // NOTE: Plogging documentation photo loading removed per requirement
    }private void savePloggingResultToGallery(String completionMessage) {
        if (currentRecord == null) {
            Toast.makeText(requireContext(), "No record data available", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date(currentRecord.getCreatedAt()));
        int trashCount = currentRecord.getPoints() / 10;
        
        String shareText = String.format(Locale.getDefault(),
            "I just completed an amazing plogging session! 🏃‍♂️🌱\n\n" +
            "📅 Date: %s\n" +
            "🏃 Distance: %.2f km\n" +
            "⏱️ Duration: %s\n" +
            "🗑️ Trash collected: %d items\n" +
            "🏆 Points earned: %d\n\n" +
            "Join me in making our environment cleaner! #Plogging #CleanEnvironment #GleanGo",
            formattedDate,
            currentRecord.getDistance() / 1000f,
            formatDuration(currentRecord.getDuration()),
            trashCount,
            currentRecord.getPoints()
        );
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Plogging Achievement");
        
        startActivity(Intent.createChooser(shareIntent, "Share your plogging achievement"));
    }    private void shareToCommunitiy() {
        // Simple success message instead of complex sharing
        Toast.makeText(requireContext(), "Great plogging session! Keep up the good work! 🎉", 
                      Toast.LENGTH_LONG).show();
    }

    private void savePloggingResultToGallery() {
        if (currentRecord == null) {
            Toast.makeText(requireContext(), "No data to save", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create summary message
            String summaryMessage = String.format(Locale.getDefault(),
                    "🎉 Plogging Complete!\n" +
                            "📍 Distance: %.2f km\n" +
                            "🗑️ Trash: %d items\n" +
                            "⭐ Points: %d\n" +
                            "⏱️ Duration: %s",
                    currentRecord.getDistance() / 1000f, 
                    currentRecord.getPoints() / 10,
                    currentRecord.getPoints(),
                    formatDuration(currentRecord.getDuration()));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                savePloggingImageAndroid10Plus(summaryMessage);
            } else {
                savePloggingImageLegacy(summaryMessage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving to gallery", e);
            Toast.makeText(requireContext(), "Failed to save to gallery", Toast.LENGTH_SHORT).show();
        }
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private void savePloggingImageAndroid10Plus(String completionMessage) {
        try {
            Bitmap bitmap = createPloggingSummaryBitmap(completionMessage);

            if (bitmap != null) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME,
                        "Plogging_Result_" + System.currentTimeMillis() + ".jpg");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/Glean/");

                Uri uri = requireContext().getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (OutputStream outputStream = requireContext().getContentResolver()
                            .openOutputStream(uri)) {
                        if (outputStream != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                            Toast.makeText(requireContext(), "Plogging result saved to gallery! 📸", 
                                         Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving image (Android 10+)", e);
            Toast.makeText(requireContext(), "Error saving to gallery", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePloggingImageLegacy(String completionMessage) {
        try {
            Bitmap bitmap = createPloggingSummaryBitmap(completionMessage);

            if (bitmap != null) {
                File picturesDir = android.os.Environment
                        .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
                File gleanDir = new File(picturesDir, "Glean");

                if (!gleanDir.exists()) {
                    gleanDir.mkdirs();
                }

                File imageFile = new File(gleanDir,
                        "Plogging_Result_" + System.currentTimeMillis() + ".jpg");

                try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);

                    android.media.MediaScannerConnection.scanFile(requireContext(),
                            new String[]{imageFile.getAbsolutePath()}, null, null);

                    Toast.makeText(requireContext(), "Plogging result saved to gallery! 📸", 
                                 Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving image (Legacy)", e);
            Toast.makeText(requireContext(), "Error saving to gallery", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap createPloggingSummaryBitmap(String completionMessage) {
        try {
            int width = 800;
            int height = 600;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            canvas.drawColor(Color.WHITE);

            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(24f);
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.parseColor("#4CAF50"));
            titlePaint.setTextSize(32f);
            titlePaint.setAntiAlias(true);
            titlePaint.setTypeface(Typeface.DEFAULT_BOLD);

            canvas.drawText("🌱 Glean Plogging Result", 50, 80, titlePaint);

            String[] lines = completionMessage.split("\n");
            int yPosition = 150;

            for (String line : lines) {
                canvas.drawText(line, 50, yPosition, textPaint);
                yPosition += 40;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String timestamp = "Generated on: " + sdf.format(new Date());

            Paint timestampPaint = new Paint();
            timestampPaint.setColor(Color.GRAY);
            timestampPaint.setTextSize(18f);
            timestampPaint.setAntiAlias(true);

            canvas.drawText(timestamp, 50, height - 50, timestampPaint);
            canvas.drawText("Generated by Glean App", 50, height - 20, timestampPaint);

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error creating summary bitmap", e);
            return null;
        }
    }

    private String createPloggingPostContent(RecordEntity record) {
        float distanceKm = record.getDistance() / 1000f;
        int durationMin = (int) (record.getDuration() / 60000);
        int trashCount = record.getPoints() / 10;
        
        return String.format(Locale.getDefault(),
                "🏃‍♂️ Just completed an amazing plogging session!\n\n" +
                "📊 Stats:\n" +
                "• Distance: %.2f km\n" +
                "• Duration: %d minutes\n" +
                "• Trash collected: %d items\n" +
                "• Points earned: %d\n\n" +
                "Every small action makes a big difference! 🌱\n" +
                "#GleanGo #Plogging #MakeTheWorldClean",
                distanceKm, durationMin, trashCount, record.getPoints());
    }

    private String formatDuration(long durationMillis) {
        long durationSeconds = durationMillis / 1000;
        int hours = (int) (durationSeconds / 3600);
        int minutes = (int) ((durationSeconds % 3600) / 60);
        int seconds = (int) (durationSeconds % 60);
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d hr %d min %d sec", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d min %d sec", minutes, seconds);
        }
    }

    private void showLoginPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Join Community")
                .setMessage("Sign in to share your achievements with the GleanGo community!")
                .setPositiveButton("Sign In", (dialog, which) -> {
                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigate(R.id.communityFeedFragment);
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.layoutMainContent.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.layoutError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        binding.layoutError.setVisibility(View.VISIBLE);
        binding.layoutMainContent.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.GONE);
        binding.tvError.setText(message);
    }

    private void showMainContent(boolean show) {
        binding.layoutMainContent.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.layoutError.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Adds distance markers every 500m along the route
     * @param routeLatLngs List of route coordinates
     */
    private void addDistanceMarkers(List<LatLng> routeLatLngs) {
        if (mMap == null || routeLatLngs == null || routeLatLngs.size() < 2) {
            Log.w(TAG, "Cannot add distance markers: map or route data not available");
            return;
        }

        Log.d(TAG, "Adding distance markers for route with " + routeLatLngs.size() + " points");
        
        float totalDistance = 0f;
        final float MARKER_INTERVAL = 500f; // 500 meters
        int markerCount = 1;
        
        // Track the last marker position to avoid duplicates
        LatLng lastMarkerPosition = routeLatLngs.get(0);
        
        for (int i = 1; i < routeLatLngs.size(); i++) {
            LatLng previousPoint = routeLatLngs.get(i - 1);
            LatLng currentPoint = routeLatLngs.get(i);
            
            // Calculate distance between consecutive points
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                previousPoint.latitude, previousPoint.longitude,
                currentPoint.latitude, currentPoint.longitude,
                results
            );
            
            totalDistance += results[0];
            
            // Check if we should add a marker at this interval
            if (totalDistance >= markerCount * MARKER_INTERVAL) {
                // Calculate the exact position for the marker on the route
                LatLng markerPosition = interpolatePosition(previousPoint, currentPoint, 
                    (markerCount * MARKER_INTERVAL - (totalDistance - results[0])) / results[0]);
                
                // Avoid adding markers too close to start/finish or previous markers
                if (isValidMarkerPosition(markerPosition, lastMarkerPosition, routeLatLngs.get(0), 
                    routeLatLngs.get(routeLatLngs.size() - 1))) {
                    
                    float distanceKm = (markerCount * MARKER_INTERVAL) / 1000f;
                    String title = String.format("%.1f km", distanceKm);
                    
                    // Create custom marker for distance checkpoints
                    MarkerOptions markerOptions = new MarkerOptions()
                        .position(markerPosition)
                        .title(title)
                        .snippet("Checkpoint jarak")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                        .anchor(0.5f, 0.5f);
                    
                    mMap.addMarker(markerOptions);
                    lastMarkerPosition = markerPosition;
                    
                    Log.d(TAG, "Added distance marker at " + distanceKm + "km: " + 
                        markerPosition.latitude + ", " + markerPosition.longitude);
                }
                
                markerCount++;
            }
        }
        
        Log.d(TAG, "Total distance: " + (totalDistance / 1000f) + "km, Added " + (markerCount - 1) + " distance markers");
    }
    
    /**
     * Interpolates position between two points based on fraction
     * @param start Start point
     * @param end End point
     * @param fraction Fraction between 0 and 1
     * @return Interpolated position
     */
    private LatLng interpolatePosition(LatLng start, LatLng end, double fraction) {
        double lat = start.latitude + (end.latitude - start.latitude) * fraction;
        double lng = start.longitude + (end.longitude - start.longitude) * fraction;
        return new LatLng(lat, lng);
    }
    
    /**
     * Checks if marker position is valid (not too close to other important markers)
     * @param markerPos Position to check
     * @param lastMarkerPos Last marker position
     * @param startPos Route start position
     * @param endPos Route end position
     * @return True if position is valid for marker placement
     */
    private boolean isValidMarkerPosition(LatLng markerPos, LatLng lastMarkerPos, LatLng startPos, LatLng endPos) {
        final float MIN_DISTANCE = 100f; // Minimum 100m between markers
        
        // Check distance from start point
        float[] distanceFromStart = new float[1];
        android.location.Location.distanceBetween(
            markerPos.latitude, markerPos.longitude,
            startPos.latitude, startPos.longitude,
            distanceFromStart
        );
        
        if (distanceFromStart[0] < MIN_DISTANCE) {
            return false;
        }
        
        // Check distance from end point
        float[] distanceFromEnd = new float[1];
        android.location.Location.distanceBetween(
            markerPos.latitude, markerPos.longitude,
            endPos.latitude, endPos.longitude,
            distanceFromEnd
        );
        
        if (distanceFromEnd[0] < MIN_DISTANCE) {
            return false;
        }
        
        // Check distance from last marker
        if (lastMarkerPos != null) {
            float[] distanceFromLast = new float[1];
            android.location.Location.distanceBetween(
                markerPos.latitude, markerPos.longitude,
                lastMarkerPos.latitude, lastMarkerPos.longitude,
                distanceFromLast
            );
            
            if (distanceFromLast[0] < MIN_DISTANCE) {
                return false;
            }
        }
          return true;    }    /**
     * Shares the plogging activity details
     */
    private void shareActivity() {
        try {
            String shareText = "Saya baru saja menyelesaikan plogging!\n" +
                    "Jarak: " + binding.tvDistance.getText().toString() + "\n" +
                    "Durasi: " + binding.tvDuration.getText().toString() + "\n" +
                    "Sampah Terkumpul: " + binding.tvTrashCollected.getText().toString() + "\n" +
                    "Mari bergabung untuk membersihkan lingkungan! #Plogging #Glean";

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Bagikan aktivitas plogging"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing activity", e);
            Toast.makeText(getContext(), "Gagal membagikan aktivitas", Toast.LENGTH_SHORT).show();
        }
    }
}

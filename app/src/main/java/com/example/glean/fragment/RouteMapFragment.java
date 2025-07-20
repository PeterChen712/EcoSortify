package com.example.glean.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.glean.R;
import com.example.glean.databinding.FragmentRouteMapBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.LocationPointEntity;
import com.example.glean.model.RecordEntity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RouteMapFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "RouteMapFragment";
    private static final String ARG_RECORD_ID = "record_id";

    private FragmentRouteMapBinding binding;
    private GoogleMap mMap;
    private AppDatabase db;
    private ExecutorService executor;
    private int recordId = -1;
    private RecordEntity recordData;
    private List<LatLng> routePoints;

    public static RouteMapFragment newInstance(int recordId) {
        RouteMapFragment fragment = new RouteMapFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_RECORD_ID, recordId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();

        if (getArguments() != null) {
            recordId = getArguments().getInt(ARG_RECORD_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRouteMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI();
        initializeMap();
        loadRouteData();
    }

    private void setupUI() {
        // Back button listener
        binding.btnBack.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigateUp();
        });

        // Show loading initially
        showLoading(true);
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void loadRouteData() {
        if (recordId == -1) {
            showError("Record ID tidak valid");
            return;
        }

        executor.execute(() -> {
            try {
                // Load record data
                recordData = db.recordDao().getRecordByIdSync(recordId);
                if (recordData == null) {
                    requireActivity().runOnUiThread(() -> {
                        showError("Data record tidak ditemukan");
                    });
                    return;
                }                // Load location points for this record
                List<LocationPointEntity> locationPoints = db.locationPointDao().getLocationPointsByRecordIdSync(recordId);
                
                // Convert LocationPointEntity to LatLng
                routePoints = new ArrayList<>();
                for (LocationPointEntity point : locationPoints) {
                    routePoints.add(new LatLng(point.getLatitude(), point.getLongitude()));
                }

                requireActivity().runOnUiThread(() -> {
                    updateUI();
                    if (mMap != null) {
                        displayRouteOnMap();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading route data", e);
                requireActivity().runOnUiThread(() -> {
                    showError("Gagal memuat data rute: " + e.getMessage());
                });
            }
        });
    }

    private void updateUI() {
        if (recordData == null) return;

        // Update route statistics
        binding.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", recordData.getDistance() / 1000f));
        
        // Format duration
        long durationMs = recordData.getDuration();
        long hours = durationMs / (1000 * 60 * 60);
        long minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60);
        binding.tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", hours, minutes));

        // Get trash count for this record
        executor.execute(() -> {
            try {
                int trashCount = db.trashDao().getTrashCountByRecordIdSync(recordId);
                requireActivity().runOnUiThread(() -> {
                    binding.tvTrashCount.setText(String.valueOf(trashCount));
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading trash count", e);
            }
        });

        // Format start and finish times
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        binding.tvStartTime.setText(timeFormat.format(new Date(recordData.getCreatedAt())));
        binding.tvFinishTime.setText(timeFormat.format(new Date(recordData.getUpdatedAt())));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        if (routePoints != null && !routePoints.isEmpty()) {
            displayRouteOnMap();
        }
    }

    private void displayRouteOnMap() {
        if (mMap == null || routePoints == null || routePoints.isEmpty()) {
            showLoading(false);
            return;
        }

        try {
            // Clear existing markers and polylines
            mMap.clear();

            LatLng startPoint = routePoints.get(0);
            LatLng endPoint = routePoints.get(routePoints.size() - 1);

            // Add start marker (green)
            mMap.addMarker(new MarkerOptions()
                    .position(startPoint)
                    .title("🚀 Titik Mulai")
                    .snippet("Plogging dimulai di sini")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // Add finish marker (red)
            mMap.addMarker(new MarkerOptions()
                    .position(endPoint)
                    .title("🏁 Titik Selesai")
                    .snippet("Plogging berakhir di sini")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Draw route polyline
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(routePoints)
                    .width(8f)
                    .color(getResources().getColor(R.color.primary_color, null))
                    .geodesic(true);

            mMap.addPolyline(polylineOptions);

            // Fit camera to show entire route
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (LatLng point : routePoints) {
                boundsBuilder.include(point);
            }

            LatLngBounds bounds = boundsBuilder.build();
            int padding = 100; // pixels
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

            showLoading(false);

        } catch (Exception e) {
            Log.e(TAG, "Error displaying route on map", e);
            showError("Gagal menampilkan rute di peta");
        }
    }

    private void showLoading(boolean show) {
        if (binding != null) {
            binding.layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        showLoading(false);
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        
        // Navigate back after showing error
        if (binding != null && binding.btnBack != null) {
            binding.getRoot().postDelayed(() -> {
                NavController navController = Navigation.findNavController(binding.btnBack);
                navController.navigateUp();
            }, 2000);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        binding = null;
    }
}

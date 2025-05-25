package com.example.glean.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import com.example.glean.model.LocationPointEntity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapHelper {
    private static final String TAG = "MapHelper";
    private final Context context;

    public MapHelper(Context context) {
        this.context = context;
    }

    /**
     * Draw a route on the map based on location points
     */
    public static void drawRouteOnMap(GoogleMap map, List<LocationPointEntity> locationPoints) {
        if (map == null || locationPoints == null || locationPoints.isEmpty()) {
            Log.e(TAG, "Cannot draw route: map or location points are null/empty");
            return;
        }

        // Create a line to connect all points
        PolylineOptions polylineOptions = new PolylineOptions()
                .width(10)
                .color(Color.BLUE);

        // Add all points to the polyline and build bounds
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (LocationPointEntity point : locationPoints) {
            LatLng position = new LatLng(point.getLatitude(), point.getLongitude());
            polylineOptions.add(position);
            boundsBuilder.include(position);
        }

        // Add the polyline to the map
        map.addPolyline(polylineOptions);

        // Add start and end markers
        if (locationPoints.size() > 0) {
            // Start marker
            LocationPointEntity startPoint = locationPoints.get(0);
            LatLng startLatLng = new LatLng(startPoint.getLatitude(), startPoint.getLongitude());
            map.addMarker(new MarkerOptions()
                    .position(startLatLng)
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // End marker
            LocationPointEntity endPoint = locationPoints.get(locationPoints.size() - 1);
            LatLng endLatLng = new LatLng(endPoint.getLatitude(), endPoint.getLongitude());
            map.addMarker(new MarkerOptions()
                    .position(endLatLng)
                    .title("End")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        // Zoom to show the entire route with padding
        try {
            LatLngBounds bounds = boundsBuilder.build();
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error building bounds: " + e.getMessage());
        }
    }

    /**
     * Capture a screenshot of the map
     */
    public void captureMapScreenshot(GoogleMap map) {
        if (map == null) {
            Log.w(TAG, "Cannot capture screenshot: map is null");
            return;
        }

        // Use descriptive parameter name to avoid conflicts
        map.snapshot(capturedBitmap -> {
            if (capturedBitmap != null) {
                saveMapScreenshot(capturedBitmap);
            } else {
                Log.e(TAG, "Failed to capture map screenshot");
            }
        });
    }

    private void saveMapScreenshot(Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "map_screenshot_" + timeStamp + ".png";

            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs();
            }

            File imageFile = new File(storageDir, fileName);

            try (FileOutputStream out = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                Log.d(TAG, "Map screenshot saved: " + imageFile.getAbsolutePath());
            }

        } catch (IOException e) {
            Log.e(TAG, "Error saving map screenshot", e);
        }
    }

    /**
     * Center map on a specific location with zoom level
     */
    public static void centerMapOnLocation(GoogleMap map, LatLng location, float zoomLevel) {
        if (map == null || location == null) {
            return;
        }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel));
    }

    /**
     * Add a marker for trash item
     */
    public static void addTrashMarker(GoogleMap map, LatLng location, String trashType) {
        if (map == null || location == null) {
            return;
        }

        // Set marker color based on trash type
        float markerColor = BitmapDescriptorFactory.HUE_RED;

        switch (trashType.toLowerCase()) {
            case "plastic":
                markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                break;
            case "paper":
                markerColor = BitmapDescriptorFactory.HUE_BLUE;
                break;
            case "glass":
                markerColor = BitmapDescriptorFactory.HUE_GREEN;
                break;
            case "metal":
                markerColor = BitmapDescriptorFactory.HUE_ORANGE;
                break;
            case "organic":
                markerColor = BitmapDescriptorFactory.HUE_MAGENTA;
                break;
        }

        map.addMarker(new MarkerOptions()
                .position(location)
                .title(trashType)
                .snippet("Click for details")
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
    }

    public LatLngBounds getBoundsForLocations(List<LatLng> locations) {
        if (locations == null || locations.isEmpty()) {
            return null;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng location : locations) {
            builder.include(location);
        }
        return builder.build();
    }

    public double calculateDistance(LatLng start, LatLng end) {
        final int R = 6371; // Radius of the Earth in km

        double latDistance = Math.toRadians(end.latitude - start.latitude);
        double lonDistance = Math.toRadians(end.longitude - start.longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in km
    }
}
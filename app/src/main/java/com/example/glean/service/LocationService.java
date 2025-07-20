package com.example.glean.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.glean.db.AppDatabase;
import com.example.glean.helper.NotificationHelper;
import com.example.glean.model.LocationPointEntity;
import com.example.glean.model.RecordEntity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    
    public static final String ACTION_START_TRACKING = "com.example.glean.ACTION_START_TRACKING";
    public static final String ACTION_STOP_TRACKING = "com.example.glean.ACTION_STOP_TRACKING";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean isTracking = false;
    private AppDatabase db;
    private ExecutorService executor;
    private int recordId = -1;
    private Location lastLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        db = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        createLocationRequest();
        createLocationCallback();
        
        // Ensure notification channels are created
        NotificationHelper.createNotificationChannels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_TRACKING.equals(action)) {
                recordId = intent.getIntExtra("RECORD_ID", -1);
                startLocationTracking();
            } else if (ACTION_STOP_TRACKING.equals(action)) {
                stopLocationTracking();
            }
        }
        return START_STICKY;
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // Update location every 10 seconds
        locationRequest.setFastestInterval(5000); // Fastest update interval 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                
                for (Location location : locationResult.getLocations()) {
                    onNewLocation(location);
                }
            }
        };
    }    private void onNewLocation(Location location) {
        Log.d(TAG, "📍 LocationService received new location: (" + location.getLatitude() + ", " + location.getLongitude() + ")");
        Log.d(TAG, "   Current record ID: " + recordId);
        
        if (recordId != -1) {
            // Calculate distance from last location if available
            float distance = 0;
            if (lastLocation != null) {
                distance = location.distanceTo(lastLocation);
                Log.d(TAG, "   Distance from last location: " + distance + "m");
            } else {
                Log.d(TAG, "   First location point (no previous location)");
            }
            
            // Make distance effectively final for lambda
            final float finalDistance = distance;
            
            // Store location point in database
            LocationPointEntity locationPoint = new LocationPointEntity(
                    recordId,
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAltitude(),
                    System.currentTimeMillis(),
                    finalDistance
            );
            
            Log.d(TAG, "🔄 LocationService: Saving location point to database...");
            
            executor.execute(() -> {
                try {
                    // Verify record exists before inserting
                    RecordEntity record = db.recordDao().getRecordByIdSync(recordId);
                    if (record == null) {
                        Log.e(TAG, "❌ LocationService ERROR: Record ID " + recordId + " does not exist!");
                        return;
                    }
                    
                    // Get count before insertion
                    int countBefore = db.locationPointDao().getLocationPointCountByRecordId(recordId);
                    
                    // Insert location point
                    long insertedId = db.locationPointDao().insert(locationPoint);
                    Log.d(TAG, "✅ LocationService: Location point inserted with ID " + insertedId);
                    
                    // Verify insertion
                    int countAfter = db.locationPointDao().getLocationPointCountByRecordId(recordId);
                    Log.d(TAG, "   LocationService: Points before=" + countBefore + ", after=" + countAfter);
                    
                    // Update route distance in record
                    if (finalDistance > 0) {
                        db.recordDao().updateDistance(recordId, finalDistance);
                        Log.d(TAG, "✅ LocationService: Record distance updated by " + finalDistance + "m");
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ LocationService ERROR saving location point: " + e.getMessage(), e);
                }
            });
            
            lastLocation = location;
        } else {
            Log.w(TAG, "⚠️  LocationService: Cannot save location - no active record (recordId = " + recordId + ")");
        }
    }

    private void startLocationTracking() {
        if (!isTracking) {
            isTracking = true;
            
            try {
                fusedLocationClient.requestLocationUpdates(
                        locationRequest, locationCallback, Looper.getMainLooper());
                
                // Use NotificationHelper to create the notification
                Notification notification = NotificationHelper.createTrackingNotification(this).build();
                startForeground(NotificationHelper.NOTIFICATION_ID_TRACKING, notification);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopLocationTracking() {
        isTracking = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isTracking) {
            stopLocationTracking();
        }
        executor.shutdown();
    }
}
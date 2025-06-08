package com.example.glean.util;

import android.content.Context;
import android.util.Log;

import com.example.glean.db.AppDatabase;
import com.example.glean.model.LocationPointEntity;
import com.example.glean.model.RecordEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Debug utility for location tracking issues
 * Helps diagnose why location points are not being saved or displayed
 */
public class LocationTrackingDebugger {
    private static final String TAG = "LocationDebugger";
    private final AppDatabase db;
    private final ExecutorService executor;
    
    public LocationTrackingDebugger(Context context) {
        db = AppDatabase.getInstance(context);
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Comprehensive debug check for a specific record
     */
    public void debugLocationTracking(int recordId) {
        executor.execute(() -> {
            Log.d(TAG, "🔍 STARTING LOCATION TRACKING DEBUG FOR RECORD ID: " + recordId);
            Log.d(TAG, "================================================");
            
            try {
                // 1. Check if record exists
                RecordEntity record = db.recordDao().getRecordByIdSync(recordId);
                if (record == null) {
                    Log.e(TAG, "❌ CRITICAL: Record with ID " + recordId + " does NOT exist!");
                    Log.e(TAG, "This explains why location points cannot be saved (foreign key constraint)");
                    listAllRecords();
                    return;
                }
                
                Log.d(TAG, "✅ Record exists: " + record.toString());
                Log.d(TAG, "   - User ID: " + record.getUserId());
                Log.d(TAG, "   - Distance: " + record.getDistance());
                Log.d(TAG, "   - Duration: " + record.getDuration());
                Log.d(TAG, "   - Created: " + new java.util.Date(record.getCreatedAt()));
                
                // 2. Check location points for this record
                List<LocationPointEntity> locationPoints = db.locationPointDao().getLocationPointsByRecordIdSync(recordId);
                Log.d(TAG, "📍 Found " + locationPoints.size() + " location points for record " + recordId);
                
                if (locationPoints.isEmpty()) {
                    Log.w(TAG, "⚠️  NO LOCATION POINTS found for record " + recordId);
                    Log.w(TAG, "This explains why the map shows 'No route data available'");
                } else {
                    Log.d(TAG, "✅ Location points details:");
                    for (int i = 0; i < Math.min(locationPoints.size(), 5); i++) {
                        LocationPointEntity point = locationPoints.get(i);
                        Log.d(TAG, "   Point " + (i+1) + ": (" + point.getLatitude() + ", " + point.getLongitude() + 
                              ") at " + new java.util.Date(point.getTimestamp()) + 
                              " distance: " + point.getDistanceFromLast() + "m");
                    }
                    if (locationPoints.size() > 5) {
                        Log.d(TAG, "   ... and " + (locationPoints.size() - 5) + " more points");
                    }
                }
                
                // 3. Check database statistics
                int totalLocationPoints = db.locationPointDao().getLocationPointCountByRecordId(recordId);
                float totalDistance = db.locationPointDao().getTotalDistanceByRecordId(recordId);
                
                Log.d(TAG, "📊 Database statistics for record " + recordId + ":");
                Log.d(TAG, "   - Total location points: " + totalLocationPoints);
                Log.d(TAG, "   - Calculated total distance: " + totalDistance + "m");
                Log.d(TAG, "   - Record stored distance: " + record.getDistance() + "m");
                
                // 4. Check foreign key integrity
                checkForeignKeyIntegrity(recordId);
                
                // 5. Test location point insertion
                testLocationPointInsertion(recordId);
                
            } catch (Exception e) {
                Log.e(TAG, "❌ ERROR during debug process: " + e.getMessage(), e);
            }
            
            Log.d(TAG, "================================================");
            Log.d(TAG, "🔍 LOCATION TRACKING DEBUG COMPLETED");
        });
    }
    
    /**
     * List all records in the database for debugging
     */
    private void listAllRecords() {
        try {
            List<RecordEntity> allRecords = db.recordDao().getAllRecordsSync();
            Log.d(TAG, "📋 All records in database (" + allRecords.size() + " total):");
            for (RecordEntity record : allRecords) {
                Log.d(TAG, "   Record ID " + record.getId() + ": User=" + record.getUserId() + 
                      ", Distance=" + record.getDistance() + "m, Created=" + new java.util.Date(record.getCreatedAt()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing records: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check foreign key integrity
     */
    private void checkForeignKeyIntegrity(int recordId) {
        try {
            // Check if there are any orphaned location points
            List<LocationPointEntity> allLocationPoints = db.locationPointDao().getAllLocationPointsSync();
            int orphanedCount = 0;
            
            for (LocationPointEntity point : allLocationPoints) {
                RecordEntity record = db.recordDao().getRecordByIdSync(point.getRecordId());
                if (record == null) {
                    orphanedCount++;
                    Log.w(TAG, "⚠️  Orphaned location point found: ID=" + point.getId() + 
                          ", RecordId=" + point.getRecordId() + " (record doesn't exist)");
                }
            }
            
            if (orphanedCount == 0) {
                Log.d(TAG, "✅ Foreign key integrity check passed - no orphaned location points");
            } else {
                Log.w(TAG, "⚠️  Found " + orphanedCount + " orphaned location points");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking foreign key integrity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test inserting a location point to see if it works
     */
    private void testLocationPointInsertion(int recordId) {
        try {
            Log.d(TAG, "🧪 Testing location point insertion for record " + recordId);
            
            // Create a test location point
            LocationPointEntity testPoint = new LocationPointEntity(
                recordId,
                -5.135399,  // Test coordinates (Makassar area)
                119.423790,
                10.0,
                System.currentTimeMillis(),
                5.0f
            );
            
            // Try to insert it
            long insertedId = db.locationPointDao().insert(testPoint);
            Log.d(TAG, "✅ Test location point inserted with ID: " + insertedId);
            
            // Verify it was inserted
            LocationPointEntity retrievedPoint = db.locationPointDao().getLocationPointByIdSync((int) insertedId);
            if (retrievedPoint != null) {
                Log.d(TAG, "✅ Test location point successfully retrieved");
                Log.d(TAG, "   Coordinates: (" + retrievedPoint.getLatitude() + ", " + retrievedPoint.getLongitude() + ")");
                Log.d(TAG, "   Record ID: " + retrievedPoint.getRecordId());
                
                // Clean up test point
                db.locationPointDao().delete(retrievedPoint);
                Log.d(TAG, "✅ Test location point cleaned up");
            } else {
                Log.e(TAG, "❌ Test location point could not be retrieved after insertion");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Test location point insertion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Monitor location point insertion in real-time
     */
    public void monitorLocationInsertion(int recordId, double latitude, double longitude, float distance) {
        executor.execute(() -> {
            Log.d(TAG, "🔄 MONITORING: About to insert location point");
            Log.d(TAG, "   Record ID: " + recordId);
            Log.d(TAG, "   Coordinates: (" + latitude + ", " + longitude + ")");
            Log.d(TAG, "   Distance: " + distance + "m");
            Log.d(TAG, "   Timestamp: " + new java.util.Date());
            
            // Check record exists before insertion
            RecordEntity record = db.recordDao().getRecordByIdSync(recordId);
            if (record == null) {
                Log.e(TAG, "❌ CRITICAL: Cannot insert location point - record " + recordId + " does not exist!");
                return;
            }
            
            try {
                // Get count before insertion
                int countBefore = db.locationPointDao().getLocationPointCountByRecordId(recordId);
                
                // Create and insert location point
                LocationPointEntity locationPoint = new LocationPointEntity(
                    recordId, latitude, longitude, 0.0, System.currentTimeMillis(), distance
                );
                
                long insertedId = db.locationPointDao().insert(locationPoint);
                
                // Get count after insertion
                int countAfter = db.locationPointDao().getLocationPointCountByRecordId(recordId);
                
                Log.d(TAG, "✅ Location point inserted with ID: " + insertedId);
                Log.d(TAG, "   Points before: " + countBefore + ", after: " + countAfter);
                
                if (countAfter == countBefore + 1) {
                    Log.d(TAG, "✅ Insertion verified successfully");
                } else {
                    Log.e(TAG, "❌ Insertion verification failed - count didn't increase as expected");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Location point insertion failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Get summary statistics for debugging
     */
    public void printDatabaseSummary() {
        executor.execute(() -> {
            try {
                List<RecordEntity> allRecords = db.recordDao().getAllRecordsSync();
                List<LocationPointEntity> allLocationPoints = db.locationPointDao().getAllLocationPointsSync();
                
                Log.d(TAG, "📊 DATABASE SUMMARY:");
                Log.d(TAG, "   Total records: " + allRecords.size());
                Log.d(TAG, "   Total location points: " + allLocationPoints.size());
                
                // Group location points by record
                for (RecordEntity record : allRecords) {
                    int pointCount = db.locationPointDao().getLocationPointCountByRecordId(record.getId());
                    Log.d(TAG, "   Record " + record.getId() + ": " + pointCount + " location points");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error generating database summary: " + e.getMessage(), e);
            }
        });
    }
    
    public void close() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}

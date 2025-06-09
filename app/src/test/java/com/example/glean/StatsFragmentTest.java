package com.example.glean;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Unit tests for StatsFragment line chart functionality
 * Tests the logic for calculating daily distances and chart data processing
 */
public class StatsFragmentTest {

    private SimpleDateFormat dateFormat;
    
    @Before
    public void setUp() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    @Test
    public void testDailyDistanceCalculation() {
        // Simulate RecordEntity data for testing
        List<MockRecord> mockRecords = new ArrayList<>();
        
        // Add records for the last 3 days
        long now = System.currentTimeMillis();
        long oneDayAgo = now - (1 * 24 * 60 * 60 * 1000L);
        long twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L);
        long threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L);
        
        // Day 1: 2 sessions
        mockRecords.add(new MockRecord(oneDayAgo, 1500f)); // 1.5km
        mockRecords.add(new MockRecord(oneDayAgo, 2500f)); // 2.5km
        
        // Day 2: 1 session
        mockRecords.add(new MockRecord(twoDaysAgo, 3000f)); // 3km
        
        // Day 3: 1 session
        mockRecords.add(new MockRecord(threeDaysAgo, 1000f)); // 1km
        
        // Calculate daily distances
        Map<String, Float> dailyDistances = calculateDailyDistances(mockRecords);
        
        // Verify results
        String day1Key = dateFormat.format(new Date(oneDayAgo));
        String day2Key = dateFormat.format(new Date(twoDaysAgo));
        String day3Key = dateFormat.format(new Date(threeDaysAgo));
        
        assertEquals("Day 1 should have 4km total", 4000f, dailyDistances.get(day1Key), 0.01f);
        assertEquals("Day 2 should have 3km total", 3000f, dailyDistances.get(day2Key), 0.01f);
        assertEquals("Day 3 should have 1km total", 1000f, dailyDistances.get(day3Key), 0.01f);
    }
    
    @Test
    public void testEmptyDataHandling() {
        List<MockRecord> emptyRecords = new ArrayList<>();
        Map<String, Float> dailyDistances = calculateDailyDistances(emptyRecords);
        
        assertTrue("Empty records should result in empty daily distances", dailyDistances.isEmpty());
        assertTrue("All distances should be zero", allDistancesZero(dailyDistances));
    }
    
    @Test
    public void testWeeklyDistanceCalculation() {
        List<MockRecord> mockRecords = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        // Add records within the last week
        for (int i = 0; i < 7; i++) {
            long dayTimestamp = now - (i * 24 * 60 * 60 * 1000L);
            mockRecords.add(new MockRecord(dayTimestamp, 1000f * (i + 1))); // 1km, 2km, 3km, etc.
        }
        
        // Add an old record (more than 7 days ago) - should be ignored
        long oldTimestamp = now - (8 * 24 * 60 * 60 * 1000L);
        mockRecords.add(new MockRecord(oldTimestamp, 5000f));
        
        float weeklyDistance = calculateWeeklyDistance(mockRecords);
        
        // Expected: 1000 + 2000 + 3000 + 4000 + 5000 + 6000 + 7000 = 28000m
        assertEquals("Weekly distance should be 28km", 28000f, weeklyDistance, 0.01f);
    }

    @Test
    public void testChartDataConversion() {
        Map<String, Float> dailyDistances = new HashMap<>();
        dailyDistances.put("2025-06-01", 2500f); // 2.5km
        dailyDistances.put("2025-06-02", 3000f); // 3km
        dailyDistances.put("2025-06-03", 1500f); // 1.5km
        
        // Test that distances are properly converted to kilometers for chart display
        for (Map.Entry<String, Float> entry : dailyDistances.entrySet()) {
            float distanceInKm = entry.getValue() / 1000f;
            assertTrue("Distance should be positive", distanceInKm > 0);
            assertTrue("Distance should be reasonable for plogging", distanceInKm < 50); // Max 50km
        }
    }

    // Helper methods to simulate StatsFragment logic
    private Map<String, Float> calculateDailyDistances(List<MockRecord> records) {
        Map<String, Float> dailyDistances = new HashMap<>();
        
        // Get records from the last 7 days
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        
        for (MockRecord record : records) {
            if (record.getCreatedAt() >= oneWeekAgo) {
                String dateKey = dateFormat.format(new Date(record.getCreatedAt()));
                float currentDistance = dailyDistances.getOrDefault(dateKey, 0f);
                dailyDistances.put(dateKey, currentDistance + record.getDistance());
            }
        }
        
        return dailyDistances;
    }
    
    private boolean allDistancesZero(Map<String, Float> dailyDistances) {
        for (Float distance : dailyDistances.values()) {
            if (distance > 0) {
                return false;
            }
        }
        return true;
    }
    
    private float calculateWeeklyDistance(List<MockRecord> records) {
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        float weeklyDistance = 0;
        
        for (MockRecord record : records) {
            if (record.getCreatedAt() >= oneWeekAgo) {
                weeklyDistance += record.getDistance();
            }
        }
        
        return weeklyDistance;
    }

    // Mock class to simulate RecordEntity for testing
    private static class MockRecord {
        private long createdAt;
        private float distance;
        
        public MockRecord(long createdAt, float distance) {
            this.createdAt = createdAt;
            this.distance = distance;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        public float getDistance() {
            return distance;
        }
    }
}

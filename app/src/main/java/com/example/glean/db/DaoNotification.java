package com.example.glean.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.NotificationEntity;

import java.util.List;

@Dao
public interface DaoNotification {
    @Insert
    long insert(NotificationEntity notification);

    @Update
    void update(NotificationEntity notification);

    @Delete
    void delete(NotificationEntity notification);

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<NotificationEntity>> getNotificationsByUserId(int userId);
    
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    LiveData<Integer> getUnreadNotificationCount(int userId);
    
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    void markAllAsRead(int userId);
    
    @Query("DELETE FROM notifications WHERE userId = :userId AND timestamp < :timestamp")
    void deleteOldNotifications(int userId, long timestamp);
}
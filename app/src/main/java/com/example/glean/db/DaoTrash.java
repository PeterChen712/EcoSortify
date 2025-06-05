package com.example.glean.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.TrashEntity;

import java.util.List;

@Dao
public interface DaoTrash {
    
    @Insert
    long insert(TrashEntity trash);
    
    @Update
    void update(TrashEntity trash);
    
    @Delete
    void delete(TrashEntity trash);
    
    @Query("SELECT * FROM trash WHERE id = :trashId")
    LiveData<TrashEntity> getTrashById(int trashId);
    
    @Query("SELECT * FROM trash WHERE id = :trashId")
    TrashEntity getTrashByIdSync(int trashId);
    
    @Query("SELECT * FROM trash WHERE recordId = :recordId ORDER BY timestamp DESC")
    LiveData<List<TrashEntity>> getTrashByRecordId(int recordId);
    
    @Query("SELECT * FROM trash WHERE recordId = :recordId ORDER BY timestamp DESC")
    List<TrashEntity> getTrashByRecordIdSync(int recordId);
    
    @Query("SELECT * FROM trash WHERE trashType = :trashType ORDER BY timestamp DESC")
    LiveData<List<TrashEntity>> getTrashByType(String trashType);
    
    @Query("SELECT * FROM trash WHERE trashType LIKE '%' || :trashType || '%' ORDER BY timestamp DESC")
    LiveData<List<TrashEntity>> getTrashByTypeContaining(String trashType);
    
    @Query("SELECT * FROM trash WHERE trashType = :trashType ORDER BY timestamp DESC")
    List<TrashEntity> getTrashByTypeSync(String trashType);
    
    @Query("SELECT COUNT(*) FROM trash WHERE recordId = :recordId")
    int getTrashCountByRecordIdSync(int recordId);
    
    @Query("SELECT SUM(CASE " +
           "WHEN trashType = 'plastic' THEN 10 " +
           "WHEN trashType = 'paper' THEN 8 " +
           "WHEN trashType = 'metal' THEN 15 " +
           "WHEN trashType = 'glass' THEN 12 " +
           "WHEN trashType = 'organic' THEN 5 " +
           "ELSE 10 " +  // Default points for unrecognized types
           "END) FROM trash WHERE recordId = :recordId")
    int getTotalPointsByRecordIdSync(int recordId);
    
    @Query("SELECT * FROM trash ORDER BY timestamp DESC")
    LiveData<List<TrashEntity>> getAllTrash();
    
    @Query("SELECT * FROM trash ORDER BY timestamp DESC")
    List<TrashEntity> getAllTrashSync();
    
    @Query("SELECT DISTINCT trashType FROM trash WHERE trashType IS NOT NULL AND trashType != '' ORDER BY trashType")
    LiveData<List<String>> getAllTrashTypes();
    
    @Query("DELETE FROM trash WHERE recordId = :recordId")
    void deleteTrashByRecordId(int recordId);
    
    @Query("SELECT trashType FROM trash WHERE recordId = :recordId")
    LiveData<List<String>> getTrashTypesByRecordId(int recordId);
    
    @Query("SELECT t.* FROM trash t " +
           "INNER JOIN records r ON t.recordId = r.id " +
           "WHERE r.userId = :userId " +
           "ORDER BY t.timestamp DESC")
    List<TrashEntity> getTrashByUserIdSync(int userId);
}
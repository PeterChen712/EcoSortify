package com.example.glean.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.UserEntity;

import java.util.List;

@Dao
public interface DaoUser {
    
    @Insert
    long insert(UserEntity user);
    
    @Update
    void update(UserEntity user);
    
    @Delete
    void delete(UserEntity user);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<UserEntity> getUserById(int userId);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    UserEntity getUserByIdSync(int userId);
    
    @Query("SELECT * FROM users WHERE email = :email")
    LiveData<UserEntity> getUserByEmail(String email);
    
    @Query("SELECT * FROM users WHERE email = :email")
    UserEntity getUserByEmailSync(String email);
    
    @Query("SELECT * FROM users WHERE username = :username")
    LiveData<UserEntity> getUserByUsername(String username);
    
    @Query("SELECT * FROM users WHERE username = :username")
    UserEntity getUserByUsernameSync(String username);
    
    @Query("SELECT * FROM users WHERE email = :email AND password = :password")
    UserEntity validateLogin(String email, String password);
    
    @Query("SELECT * FROM users ORDER BY id DESC")
    LiveData<List<UserEntity>> getAllUsers();
    
    @Query("SELECT * FROM users ORDER BY id DESC")
    List<UserEntity> getAllUsersSync();
    
    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    int checkEmailExists(String email);
    
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    int checkUsernameExists(String username);
    
    @Query("UPDATE users SET password = :newPassword WHERE id = :userId")
    void updatePassword(int userId, String newPassword);
    
    @Query("UPDATE users SET profileImagePath = :imagePath WHERE id = :userId")
    void updateProfileImage(int userId, String imagePath);
    
    @Query("DELETE FROM users WHERE id = :userId")
    void deleteUserById(int userId);

    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
    
    @Query("SELECT * FROM users LIMIT 1")
    UserEntity getFirstUser();
}

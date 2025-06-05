package com.example.glean.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Delete
    void delete(UserEntity user);

    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<UserEntity> getUserById(int userId);

    @Query("SELECT * FROM users WHERE id = :userId")
    UserEntity getUserByIdSync(int userId);

    @Query("SELECT * FROM users WHERE username = :username")
    LiveData<UserEntity> getUserByUsername(String username);

    @Query("SELECT * FROM users WHERE email = :email")
    LiveData<UserEntity> getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    LiveData<UserEntity> getUserByCredentials(String username, String password);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    UserEntity getUserByCredentialsSync(String username, String password);

    @Query("SELECT * FROM users")
    LiveData<List<UserEntity>> getAllUsers();

    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    int checkUsernameExists(String username);

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    int checkEmailExists(String email);

    @Query("UPDATE users SET points = points + :points WHERE id = :userId")
    void addPoints(int userId, int points);

    @Query("UPDATE users SET points = :points WHERE id = :userId")
    void updatePoints(int userId, int points);

    @Query("UPDATE users SET profileImagePath = :imagePath WHERE id = :userId")
    void updateProfileImage(int userId, String imagePath);

    @Query("UPDATE users SET activeDecoration = :decoration WHERE id = :userId")
    void updateActiveDecoration(int userId, String decoration);

    @Query("SELECT MAX(points) FROM users")
    int getMaxPoints();

    @Query("SELECT * FROM users ORDER BY points DESC LIMIT :limit")
    LiveData<List<UserEntity>> getTopUsers(int limit);

    @Query("DELETE FROM users WHERE id = :userId")
    void deleteById(int userId);
}
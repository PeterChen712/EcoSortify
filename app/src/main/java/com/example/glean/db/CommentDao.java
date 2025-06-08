package com.example.glean.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.CommentEntity;

import java.util.List;

@Dao
public interface CommentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertComment(CommentEntity comment);
    
    @Update
    void updateComment(CommentEntity comment);
    
    @Delete
    void deleteComment(CommentEntity comment);
    
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    LiveData<List<CommentEntity>> getCommentsByPostId(int postId);
    
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    List<CommentEntity> getCommentsByPostIdSync(int postId);
    
    @Query("SELECT * FROM comments WHERE id = :commentId")
    LiveData<CommentEntity> getCommentById(int commentId);
    
    @Query("SELECT * FROM comments WHERE id = :commentId")
    CommentEntity getCommentByIdSync(int commentId);
    
    @Query("SELECT * FROM comments WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<CommentEntity>> getCommentsByUserId(int userId);
    
    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    int getCommentCountByPostId(int postId);
    
    @Query("DELETE FROM comments WHERE postId = :postId")
    void deleteCommentsByPostId(int postId);
    
    @Query("DELETE FROM comments WHERE id = :commentId")
    void deleteCommentById(int commentId);
}

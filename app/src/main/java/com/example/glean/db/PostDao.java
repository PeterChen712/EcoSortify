package com.example.glean.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.glean.model.PostEntity;

import java.util.List;

@Dao
public interface PostDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPost(PostEntity post);
    
    @Update
    void updatePost(PostEntity post);
    
    @Delete
    void deletePost(PostEntity post);
    
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    LiveData<List<PostEntity>> getAllPosts();
    
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    List<PostEntity> getAllPostsSync();
    
    @Query("SELECT * FROM posts WHERE id = :postId")
    LiveData<PostEntity> getPostById(int postId);
    
    @Query("SELECT * FROM posts WHERE id = :postId")
    PostEntity getPostByIdSync(int postId);
    
    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<PostEntity>> getPostsByUserId(int userId);
    
    @Query("UPDATE posts SET likeCount = :likeCount, isLiked = :isLiked WHERE id = :postId")
    void updatePostLike(int postId, int likeCount, boolean isLiked);
    
    @Query("UPDATE posts SET commentCount = :commentCount WHERE id = :postId")
    void updatePostCommentCount(int postId, int commentCount);
    
    @Query("UPDATE posts SET isBookmarked = :isBookmarked WHERE id = :postId")
    void updatePostBookmark(int postId, boolean isBookmarked);
    
    @Query("SELECT * FROM posts WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    LiveData<List<PostEntity>> getBookmarkedPosts();
    
    @Query("DELETE FROM posts WHERE id = :postId")
    void deletePostById(int postId);
    
    @Query("SELECT COUNT(*) FROM posts")
    int getPostCount();
    
    @Query("SELECT COUNT(*) FROM posts WHERE location = :location AND userId = :userId")
    int checkPostExistsByLocationAndUser(String location, int userId);
    
    @Query("DELETE FROM posts")
    void deleteAll();
}

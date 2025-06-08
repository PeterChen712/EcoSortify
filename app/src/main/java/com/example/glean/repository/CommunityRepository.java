package com.example.glean.repository;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.example.glean.db.AppDatabase;
import com.example.glean.db.CommentDao;
import com.example.glean.db.PostDao;
import com.example.glean.model.CommentEntity;
import com.example.glean.model.PostEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityRepository {
    private PostDao postDao;
    private CommentDao commentDao;
    private ExecutorService executor;
    
    public CommunityRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        postDao = database.postDao();
        commentDao = database.commentDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    // Post operations
    public LiveData<List<PostEntity>> getAllPosts() {
        return postDao.getAllPosts();
    }
    
    public LiveData<PostEntity> getPostById(int postId) {
        return postDao.getPostById(postId);
    }
    
    public void insertPost(PostEntity post, OnPostInsertedListener listener) {
        executor.execute(() -> {
            long postId = postDao.insertPost(post);
            post.setId((int) postId);
            if (listener != null) {
                listener.onPostInserted(post);
            }
        });
    }
    
    public void updatePostLike(int postId, int likeCount, boolean isLiked) {
        executor.execute(() -> postDao.updatePostLike(postId, likeCount, isLiked));
    }
    
    public void updatePostCommentCount(int postId, int commentCount) {
        executor.execute(() -> postDao.updatePostCommentCount(postId, commentCount));
    }
    
    // Comment operations
    public LiveData<List<CommentEntity>> getCommentsByPostId(int postId) {
        return commentDao.getCommentsByPostId(postId);
    }
    
    public void insertComment(CommentEntity comment, OnCommentInsertedListener listener) {
        executor.execute(() -> {
            long commentId = commentDao.insertComment(comment);
            comment.setId((int) commentId);
            
            // Update post comment count
            int commentCount = commentDao.getCommentCountByPostId(comment.getPostId());
            postDao.updatePostCommentCount(comment.getPostId(), commentCount);
            
            if (listener != null) {
                listener.onCommentInserted(comment);
            }
        });
    }
    
    public void deleteComment(CommentEntity comment) {
        executor.execute(() -> {
            commentDao.deleteComment(comment);
            // Update post comment count
            int commentCount = commentDao.getCommentCountByPostId(comment.getPostId());
            postDao.updatePostCommentCount(comment.getPostId(), commentCount);
        });
    }
    
    public interface OnPostInsertedListener {
        void onPostInserted(PostEntity post);
    }
    
    public interface OnCommentInsertedListener {
        void onCommentInserted(CommentEntity comment);
    }
}

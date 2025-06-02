package com.example.glean.helper;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.glean.model.CommentModel;
import com.example.glean.model.CommunityPostModel;
import com.example.glean.model.FirebaseUserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    
    // Collections
    private static final String USERS_COLLECTION = "users";
    private static final String POSTS_COLLECTION = "community_posts";
    private static final String COMMENTS_COLLECTION = "comments";
    private static final String LIKES_COLLECTION = "likes";
    
    // Storage paths
    private static final String IMAGES_PATH = "images/";
    private static final String PROFILE_IMAGES_PATH = "profile_images/";
    private static final String POST_IMAGES_PATH = "post_images/";
    
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private final Context context;

    public FirebaseHelper(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    // Authentication Methods
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public void signOut() {
        auth.signOut();
    }

    // User Methods
    public void createOrUpdateUser(FirebaseUserModel user, OnCompleteListener<Void> listener) {
        if (!isUserLoggedIn()) {
            listener.onComplete(null);
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(user)
                .addOnCompleteListener(listener);
    }

    public void getUserProfile(String uid, OnSuccessListener<FirebaseUserModel> onSuccess, OnFailureListener onFailure) {
        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        FirebaseUserModel user = documentSnapshot.toObject(FirebaseUserModel.class);
                        onSuccess.onSuccess(user);
                    } else {
                        onFailure.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }

    // Community Posts Methods
    public void createPost(CommunityPostModel post, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (!isUserLoggedIn()) {
            onFailure.onFailure(new Exception("User not logged in"));
            return;
        }

        firestore.collection(POSTS_COLLECTION)
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    String postId = documentReference.getId();
                    // Update post with its ID
                    documentReference.update("id", postId)
                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(postId))
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    public void getCommunityPosts(int limit, OnSuccessListener<List<CommunityPostModel>> onSuccess, OnFailureListener onFailure) {
        Query query = firestore.collection(POSTS_COLLECTION)
                .whereEqualTo("isPublic", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CommunityPostModel> posts = queryDocumentSnapshots.toObjects(CommunityPostModel.class);
                    onSuccess.onSuccess(posts);
                })
                .addOnFailureListener(onFailure);
    }

    public void getUserPosts(String userId, OnSuccessListener<List<CommunityPostModel>> onSuccess, OnFailureListener onFailure) {
        firestore.collection(POSTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CommunityPostModel> posts = queryDocumentSnapshots.toObjects(CommunityPostModel.class);
                    onSuccess.onSuccess(posts);
                })
                .addOnFailureListener(onFailure);
    }

    // Like Methods
    public void toggleLike(String postId, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        if (!isUserLoggedIn()) {
            onFailure.onFailure(new Exception("User not logged in"));
            return;
        }

        String userId = getCurrentUser().getUid();
        DocumentReference likeRef = firestore.collection(LIKES_COLLECTION)
                .document(postId + "_" + userId);

        likeRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Unlike
                likeRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            updateLikeCount(postId, -1);
                            onSuccess.onSuccess(false);
                        })
                        .addOnFailureListener(onFailure);
            } else {
                // Like
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("postId", postId);
                likeData.put("userId", userId);
                likeData.put("timestamp", System.currentTimeMillis());

                likeRef.set(likeData)
                        .addOnSuccessListener(aVoid -> {
                            updateLikeCount(postId, 1);
                            onSuccess.onSuccess(true);
                        })
                        .addOnFailureListener(onFailure);
            }
        }).addOnFailureListener(onFailure);
    }

    private void updateLikeCount(String postId, int increment) {
        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(postRef);
            long currentCount = snapshot.getLong("likeCount") != null ? snapshot.getLong("likeCount") : 0;
            transaction.update(postRef, "likeCount", currentCount + increment);
            return null;
        });
    }

    // Comments Methods
    public void addComment(CommentModel comment, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (!isUserLoggedIn()) {
            onFailure.onFailure(new Exception("User not logged in"));
            return;
        }

        firestore.collection(COMMENTS_COLLECTION)
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    String commentId = documentReference.getId();
                    documentReference.update("id", commentId);
                    updateCommentCount(comment.getPostId(), 1);
                    onSuccess.onSuccess(commentId);
                })
                .addOnFailureListener(onFailure);
    }

    public void getComments(String postId, OnSuccessListener<List<CommentModel>> onSuccess, OnFailureListener onFailure) {
        firestore.collection(COMMENTS_COLLECTION)
                .whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CommentModel> comments = queryDocumentSnapshots.toObjects(CommentModel.class);
                    onSuccess.onSuccess(comments);
                })
                .addOnFailureListener(onFailure);
    }

    private void updateCommentCount(String postId, int increment) {
        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(postRef);
            long currentCount = snapshot.getLong("commentCount") != null ? snapshot.getLong("commentCount") : 0;
            transaction.update(postRef, "commentCount", currentCount + increment);
            return null;
        });
    }

    // Storage Methods
    public void uploadImage(String imagePath, String storagePath, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (imagePath == null || imagePath.isEmpty()) {
            onFailure.onFailure(new Exception("Image path is empty"));
            return;
        }

        File file = new File(imagePath);
        if (!file.exists()) {
            onFailure.onFailure(new Exception("File does not exist"));
            return;
        }

        Uri fileUri = Uri.fromFile(file);
        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference().child(storagePath + fileName);

        UploadTask uploadTask = storageRef.putFile(fileUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                onSuccess.onSuccess(uri.toString());
            }).addOnFailureListener(onFailure);
        }).addOnFailureListener(onFailure);
    }

    public void uploadProfileImage(String imagePath, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        uploadImage(imagePath, PROFILE_IMAGES_PATH, onSuccess, onFailure);
    }

    public void uploadPostImage(String imagePath, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        uploadImage(imagePath, POST_IMAGES_PATH, onSuccess, onFailure);
    }

    // Additional methods for PostDetailFragment
    public void getPost(String postId, OnSuccessListener<CommunityPostModel> onSuccess, OnFailureListener onFailure) {
        firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        CommunityPostModel post = documentSnapshot.toObject(CommunityPostModel.class);
                        onSuccess.onSuccess(post);
                    } else {
                        onFailure.onFailure(new Exception("Post not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }

    public void isPostLikedByUser(String postId, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        if (!isUserLoggedIn()) {
            onSuccess.onSuccess(false);
            return;
        }

        String userId = getCurrentUser().getUid();
        DocumentReference likeRef = firestore.collection(LIKES_COLLECTION)
                .document(postId + "_" + userId);

        likeRef.get()
                .addOnSuccessListener(documentSnapshot -> onSuccess.onSuccess(documentSnapshot.exists()))
                .addOnFailureListener(onFailure);
    }

    // User statistics methods
    public void updateUserStats(String userId, Map<String, Object> stats, OnCompleteListener<Void> listener) {
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(stats)
                .addOnCompleteListener(listener);
    }

    // Community leaderboard (for future features)
    public void getTopUsers(int limit, OnSuccessListener<List<FirebaseUserModel>> onSuccess, OnFailureListener onFailure) {
        firestore.collection(USERS_COLLECTION)
                .whereEqualTo("isPublicProfile", true)
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FirebaseUserModel> users = queryDocumentSnapshots.toObjects(FirebaseUserModel.class);
                    onSuccess.onSuccess(users);
                })
                .addOnFailureListener(onFailure);
    }

    // Delete methods
    public void deletePost(String postId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (!isUserLoggedIn()) {
            onFailure.onFailure(new Exception("User not logged in"));
            return;
        }

        firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void deleteComment(String commentId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (!isUserLoggedIn()) {
            onFailure.onFailure(new Exception("User not logged in"));
            return;
        }

        firestore.collection(COMMENTS_COLLECTION)
                .document(commentId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // Search methods
    public void searchPosts(String query, OnSuccessListener<List<CommunityPostModel>> onSuccess, OnFailureListener onFailure) {
        // Note: Firestore doesn't support full-text search, so this is a basic implementation
        // For production, you'd use Algolia or similar service
        
        firestore.collection(POSTS_COLLECTION)
                .whereEqualTo("isPublic", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100) // Get recent posts and filter locally
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CommunityPostModel> allPosts = queryDocumentSnapshots.toObjects(CommunityPostModel.class);
                    List<CommunityPostModel> filteredPosts = new ArrayList<>();
                    
                    String lowerQuery = query.toLowerCase();
                    for (CommunityPostModel post : allPosts) {
                        if (post.getContent() != null && 
                            post.getContent().toLowerCase().contains(lowerQuery)) {
                            filteredPosts.add(post);
                        }
                    }
                    
                    onSuccess.onSuccess(filteredPosts);
                })
                .addOnFailureListener(onFailure);
    }

    // Utility Methods
    public boolean isOnline() {
        // Simple network check - you can enhance this
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }

    public interface FirebaseCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}
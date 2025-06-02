package com.example.glean.model;

public class CommentModel {
    private String id;
    private String postId;
    private String userId;
    private String userName;
    private String userProfileUrl;
    private String content;
    private long timestamp;
    private int likeCount;

    public CommentModel() {
        // Required for Firebase
    }

    public CommentModel(String postId, String userId, String userName, String content) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.likeCount = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserProfileUrl() { return userProfileUrl; }
    public void setUserProfileUrl(String userProfileUrl) { this.userProfileUrl = userProfileUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
}
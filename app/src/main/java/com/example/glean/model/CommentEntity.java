package com.example.glean.model;

public class CommentEntity {
    private String id;
    private String postId;
    private String username;
    private String content;
    private long timestamp;
    private String userAvatar;
    
    public CommentEntity() {}
    
    public CommentEntity(String id, String postId, String username, String content, long timestamp) {
        this.id = id;
        this.postId = postId;
        this.username = username;
        this.content = content;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getPostId() {
        return postId;
    }
    
    public void setPostId(String postId) {
        this.postId = postId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getUserAvatar() {
        return userAvatar;
    }
    
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
}

package com.example.glean.model;

public class PostEntity {
    private String id;
    private String username;
    private String userAvatar;
    private String content;
    private String imageUrl;
    private int likeCount;
    private int commentCount;
    private long timestamp;
    private boolean isLiked;
    private boolean isBookmarked;
    private String location;
    private float trashWeight;
    private float distance;
    
    public PostEntity() {}
    
    public PostEntity(String id, String username, String content, String imageUrl, 
                     int likeCount, int commentCount, long timestamp) {
        this.id = id;
        this.username = username;
        this.content = content;
        this.imageUrl = imageUrl;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getUserAvatar() {
        return userAvatar;
    }
    
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public int getLikeCount() {
        return likeCount;
    }
    
    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
    
    public int getCommentCount() {
        return commentCount;
    }
    
    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isLiked() {
        return isLiked;
    }
    
    public void setLiked(boolean liked) {
        isLiked = liked;
    }
    
    public boolean isBookmarked() {
        return isBookmarked;
    }
    
    public void setBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public float getTrashWeight() {
        return trashWeight;
    }
    
    public void setTrashWeight(float trashWeight) {
        this.trashWeight = trashWeight;
    }
    
    public float getDistance() {
        return distance;
    }
    
    public void setDistance(float distance) {
        this.distance = distance;
    }
}

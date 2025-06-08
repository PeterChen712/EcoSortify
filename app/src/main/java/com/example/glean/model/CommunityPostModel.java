package com.example.glean.model;

import java.util.List;
import java.util.Map;

public class CommunityPostModel {
    private String id;
    private String userId;
    private String userName;
    private String userProfileUrl;
    private String content;
    private String imageUrl;
    private String location;
    private double latitude;
    private double longitude;
    private long timestamp;
    private int likeCount;
    private int commentCount;
    private String category; // "plogging", "tips", "achievement"
    private Map<String, Object> metadata; // Additional data like distance, trash_count
    private List<String> tags;
    private boolean isPublic;    public CommunityPostModel() {
        // Default constructor
    }

    public CommunityPostModel(String userId, String userName, String content) {
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.likeCount = 0;
        this.commentCount = 0;
        this.isPublic = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserProfileUrl() { return userProfileUrl; }
    public void setUserProfileUrl(String userProfileUrl) { this.userProfileUrl = userProfileUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
}
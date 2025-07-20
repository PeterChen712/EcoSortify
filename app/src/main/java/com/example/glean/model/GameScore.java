package com.example.ecosortify.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "game_scores")
public class GameScore {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int score;
    private long timestamp;
    
    public GameScore(int score, long timestamp) {
        this.score = score;
        this.timestamp = timestamp;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
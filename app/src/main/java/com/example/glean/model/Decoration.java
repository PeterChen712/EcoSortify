package com.example.glean.model;

public class Decoration {
    private int id;
    private String name;
    private String description;
    private int price;
    private int imageResourceId;
    private boolean isOwned;

    public Decoration(int id, String name, String description, int price, int imageResourceId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageResourceId = imageResourceId;
        this.isOwned = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
    }

    public boolean isOwned() {
        return isOwned;
    }

    public void setOwned(boolean owned) {
        isOwned = owned;
    }
}
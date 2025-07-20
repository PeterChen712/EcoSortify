package com.example.glean.model;

public class ProfileSkin {
    private String id;
    private String name;
    private int price;
    private int drawableResource;
    private boolean unlocked;
    private boolean selected;
    private boolean isGif; // New property to identify GIF backgrounds
    
    public ProfileSkin(String id, String name, int price, int drawableResource, boolean unlocked, boolean selected) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.drawableResource = drawableResource;
        this.unlocked = unlocked;
        this.selected = selected;
        this.isGif = false; // Default to false
    }
    
    public ProfileSkin(String id, String name, int price, int drawableResource, boolean unlocked, boolean selected, boolean isGif) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.drawableResource = drawableResource;
        this.unlocked = unlocked;
        this.selected = selected;
        this.isGif = isGif;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getDrawableResource() { return drawableResource; }
    public boolean isUnlocked() { return unlocked; }
    public boolean isSelected() { return selected; }
    public boolean isGif() { return isGif; }
    
    // Setters
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public void setGif(boolean gif) { this.isGif = gif; }
    
    public String getPriceText() {
        return price == 0 ? "Free" : price + " Points";
    }
}

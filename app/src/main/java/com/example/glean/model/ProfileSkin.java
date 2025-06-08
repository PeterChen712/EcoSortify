package com.example.glean.model;

public class ProfileSkin {
    private String id;
    private String name;
    private int price;
    private int drawableResource;
    private boolean unlocked;
    private boolean selected;
    
    public ProfileSkin(String id, String name, int price, int drawableResource, boolean unlocked, boolean selected) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.drawableResource = drawableResource;
        this.unlocked = unlocked;
        this.selected = selected;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getDrawableResource() { return drawableResource; }
    public boolean isUnlocked() { return unlocked; }
    public boolean isSelected() { return selected; }
    
    // Setters
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public void setSelected(boolean selected) { this.selected = selected; }
    
    public String getPriceText() {
        return price == 0 ? "Free" : price + " Points";
    }
}

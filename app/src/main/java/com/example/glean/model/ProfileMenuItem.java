package com.example.glean.model;

public class ProfileMenuItem {
    private String title;
    private String subtitle;
    private int iconResId;
    private String action;
    private boolean hasAction;

    public ProfileMenuItem(String title, String subtitle, int iconResId, String action) {
        this.title = title;
        this.subtitle = subtitle;
        this.iconResId = iconResId;
        this.action = action;
        this.hasAction = action != null && !action.isEmpty();
    }

    public ProfileMenuItem(String title, int iconResId, String action) {
        this(title, null, iconResId, action);
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
        this.hasAction = action != null && !action.isEmpty();
    }

    public boolean hasAction() {
        return hasAction;
    }

    public void setHasAction(boolean hasAction) {
        this.hasAction = hasAction;
    }
}
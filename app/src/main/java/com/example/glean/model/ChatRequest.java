package com.example.glean.model;

import java.util.ArrayList;
import java.util.List;

public class ChatRequest {
    private List<Content> contents; 

    public ChatRequest(List<Content> contents) {
        this.contents = contents;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public static class Content {
        private String role; 
        private List<Part> parts;

        public Content(String role, Part part) {
            this.role = role;
            this.parts = new ArrayList<>();
            this.parts.add(part);
        }

        public Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
        }

        public String getRole() {
            return role;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private String text;
        private InlineData inlineData; // Add for image support

        public Part(String text) {
            this.text = text;
        }

        public Part() {
            // Default constructor
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public InlineData getInlineData() {
            return inlineData;
        }

        public void setInlineData(InlineData inlineData) {
            this.inlineData = inlineData;
        }
    }

    public static class InlineData {
        private String mimeType;
        private String data;

        public InlineData(String mimeType, String data) {
            this.mimeType = mimeType;
            this.data = data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
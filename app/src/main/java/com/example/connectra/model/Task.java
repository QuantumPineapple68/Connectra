package com.example.connectra.model;

public class Task {
    private String title;
    private String description;

    public Task() {
        // Default constructor required for Firebase
    }

    public Task(String title) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}

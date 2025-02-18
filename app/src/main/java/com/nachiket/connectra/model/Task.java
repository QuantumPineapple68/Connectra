package com.nachiket.connectra.model;

public class Task {
    private String id;
    private String title;
    private String description;

    public Task() {
        // Default constructor required for Firebase
    }

    public Task(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
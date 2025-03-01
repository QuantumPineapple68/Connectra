package com.nachiket.connectra.model;

public class User {
    private String id;
    private String name;
    private String username;
    private String profileImage;
    private String connectionStatus; // none, pending, sent, connected

    public User() {
        // Required for Firebase
    }

    public User(String id, String name, String username, String profileImage) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.profileImage = profileImage;
        this.connectionStatus = "none";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
}
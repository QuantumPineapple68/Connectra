package com.nachiket.connectra.model;

public class ConnectionRequest {
    private String senderId;
    private long timestamp;
    private String status; // pending, accepted, rejected

    public ConnectionRequest() {
        // Required for Firebase
    }

    public ConnectionRequest(String senderId, long timestamp, String status) {
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
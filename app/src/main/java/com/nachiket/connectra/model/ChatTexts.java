package com.nachiket.connectra.model;

public class ChatTexts {
    private String messageId;
    private String message;
    private String senderId;
    private String receiverId;
    private long timestamp;
    private boolean read;
    private String reaction;
    private String reactionBy;

    // Default constructor for Firebase
    public ChatTexts() {
        this.read = false;  // Default to unread
    }

    public ChatTexts(String senderId, String receiverId, String message, long timestamp, String reaction, String reactionBy) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.read = false;
        this.reaction = reaction;
        this.reactionBy = reactionBy;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }

    public String getReactionBy() {
        return reactionBy;
    }

    public void setReactionBy(String reactionBy) {
        this.reactionBy = reactionBy;
    }
}
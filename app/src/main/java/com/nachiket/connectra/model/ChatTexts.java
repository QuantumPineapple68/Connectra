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
    private String replyToId;       // ID of the message being replied to
    private String replyToText;     // Content of the message being replied to
    private String replyToSenderId; // Sender ID of the message being replied to

    // Default constructor required for Firebase
    public ChatTexts() {
    }

    // Constructor with basic fields
    public ChatTexts(String messageId, String message, String senderId, String receiverId, long timestamp, boolean read) {
        this.messageId = messageId;
        this.message = message;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.read = read;
    }

    // Constructor with reply functionality
    public ChatTexts(String messageId, String message, String senderId, String receiverId, long timestamp, boolean read,
                     String replyToId, String replyToText, String replyToSenderId) {
        this.messageId = messageId;
        this.message = message;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.read = read;
        this.replyToId = replyToId;
        this.replyToText = replyToText;
        this.replyToSenderId = replyToSenderId;
    }

    // Getters and setters for all fields
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
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

    // Getters and setters for reply functionality
    public String getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(String replyToId) {
        this.replyToId = replyToId;
    }

    public String getReplyToText() {
        return replyToText;
    }

    public void setReplyToText(String replyToText) {
        this.replyToText = replyToText;
    }

    public String getReplyToSenderId() {
        return replyToSenderId;
    }

    public void setReplyToSenderId(String replyToSenderId) {
        this.replyToSenderId = replyToSenderId;
    }
}
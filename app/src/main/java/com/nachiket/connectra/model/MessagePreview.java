package com.nachiket.connectra.model;

public class MessagePreview {
    private String messageId;
    private String chatId;
    private String partnerId;
    private String partnerName;
    private String partnerUsername;
    private String partnerProfileImage;
    private String lastMessage;
    private long timestamp;
    private boolean read;
    private boolean profileApproved;
    private boolean isOnline;
    private long lastSeen;

    public MessagePreview() {
        // Required for Firebase
    }

    public MessagePreview(String messageId, String chatId, String partnerId, String partnerName,
                          String partnerUsername, String partnerProfileImage, String lastMessage,
                          long timestamp, boolean read, boolean profileApproved) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.partnerUsername = partnerUsername;
        this.partnerProfileImage = partnerProfileImage;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.read = read;
        this.profileApproved = profileApproved;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getPartnerUsername() {
        return partnerUsername;
    }

    public void setPartnerUsername(String partnerUsername) {
        this.partnerUsername = partnerUsername;
    }

    public String getPartnerProfileImage() {
        return partnerProfileImage;
    }

    public void setPartnerProfileImage(String partnerProfileImage) {
        this.partnerProfileImage = partnerProfileImage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
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

    public boolean isProfileApproved() {
        return profileApproved;
    }

    public void setProfileApproved(boolean profileApproved) {
        this.profileApproved = profileApproved;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}
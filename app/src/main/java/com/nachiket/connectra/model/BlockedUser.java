package com.nachiket.connectra.model;

public class BlockedUser {
    private String blockedUserId;
    private long blockTimestamp;

    public BlockedUser() {
        // Required empty constructor for Firebase
    }

    public BlockedUser(String blockedUserId) {
        this.blockedUserId = blockedUserId;
        this.blockTimestamp = System.currentTimeMillis();
    }

    public String getBlockedUserId() {
        return blockedUserId;
    }

    public long getBlockTimestamp() {
        return blockTimestamp;
    }
}
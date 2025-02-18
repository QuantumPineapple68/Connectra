package com.nachiket.connectra.model;

public class NewUser {
    private String name;
    private String myskill;
    private String goalskill;
    private String gender;
    private String age;
    private String id;
    private String userName;
    private String profileImage;
    private float rating;
    private String bio;
    private String cerf;
    private boolean hasUnreadMessages;
    private long lastMessageTimestamp;
    private boolean profileAprooved;
    private boolean cerfApproved;
    boolean isBanned = false;

    public NewUser(String name, String myskill, String goalskill, String gender, String age, String id, String userName, String bio, String profileImage, float rating, String cerf, boolean profileAprooved, boolean cerfApproved, boolean isBanned) {
        this.name = name;
        this.myskill = myskill;
        this.goalskill = goalskill;
        this.gender = gender;
        this.age = age;
        this.id = id;
        this.userName = userName;
        this.bio = bio;
        this.profileImage = profileImage;
        this.rating = rating;
        this.cerf = cerf;
        this.hasUnreadMessages = false;
        this.lastMessageTimestamp = 0;
        this.profileAprooved = profileAprooved;
        this.cerfApproved = cerfApproved;
        this.isBanned = isBanned;
    }

    // Default constructor for Firebase
    public NewUser() {
    }

    public boolean hasUnreadMessages() {
        return hasUnreadMessages;
    }

    public void setHasUnreadMessages(boolean hasUnreadMessages) {
        this.hasUnreadMessages = hasUnreadMessages;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getCerf() {
        return cerf;
    }

    public void setCerf(String cerf) {
        this.cerf = cerf;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getUsername() {
        return userName;
    }

    public void setUserId(String userId) {
        this.userName = userId;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    // Getters and setters
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getMyskill() {
        return myskill;
    }

    public void setMyskill(String myskill) {
        this.myskill = myskill;
    }

    public String getGoalskill() {
        return goalskill;
    }

    public void setGoalskill(String goalskill) {
        this.goalskill = goalskill;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isProfileAprooved() {
        return profileAprooved;
    }

    public void setProfileAprooved(boolean profileAprooved) {
        this.profileAprooved = profileAprooved;
    }

    public boolean isCerfApproved() {
        return cerfApproved;
    }

    public void setCerfApproved(boolean cerfApproved) {
        this.cerfApproved = cerfApproved;
    }

    public boolean isBanned() {
        return isBanned;
    }
    public void setBanned(boolean banned) {
        isBanned = banned;
    }


}
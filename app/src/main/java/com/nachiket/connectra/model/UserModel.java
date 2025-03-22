package com.nachiket.connectra.model;

public class UserModel {
    private String id;
    private String name;
    private String username;
    private String email;
    private String profileImage;
    private int age;
    private String gender;
    private String goalSkill;
    private String mySkill;
    private boolean profileApproved;
    private boolean banned;
    private boolean certApproved;
    private double rev;

    public UserModel() {
        // Required for Firebase
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getGoalSkill() {
        return goalSkill;
    }

    public void setGoalSkill(String goalSkill) {
        this.goalSkill = goalSkill;
    }

    public String getMySkill() {
        return mySkill;
    }

    public void setMySkill(String mySkill) {
        this.mySkill = mySkill;
    }

    public boolean isProfileApproved() {
        return profileApproved;
    }

    public void setProfileApproved(boolean profileApproved) {
        this.profileApproved = profileApproved;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public boolean isCertApproved() {
        return certApproved;
    }

    public void setCertApproved(boolean certApproved) {
        this.certApproved = certApproved;
    }

    public double getRev() {
        return rev;
    }

    public void setRev(double rev) {
        this.rev = rev;
    }
}

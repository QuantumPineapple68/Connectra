package com.example.connectra.Fragments;

public class NewUser {
    private String name;
    private String myskill;
    private String goalskill;
    private String gender;
    private String age;
    private String id;
    private String userName;
    private String profileImage;
    private String bio;


    // Default constructor for Firebase
    public NewUser() {
    }

    public NewUser(String name, String myskill, String goalskill, String gender, String age, String id, String userName, String bio) {
        this.name = name;
        this.myskill = myskill;
        this.goalskill = goalskill;
        this.gender = gender;
        this.age = age;
        this.id = id;
        this.userName = userName;
        this.bio = bio;
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMyskill() { return myskill; }
    public void setMyskill(String myskill) { this.myskill = myskill; }

    public String getGoalskill() { return goalskill; }
    public void setGoalskill(String goalskill) { this.goalskill = goalskill; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

}

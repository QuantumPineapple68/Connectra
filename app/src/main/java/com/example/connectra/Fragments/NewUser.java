package com.example.connectra.Fragments;

public class NewUser {
    private String name;
    private String myskill;
    private String goalskill;
    private String gender;

    // Default constructor for Firebase
    public NewUser() {
    }

    public NewUser(String name, String myskill, String goalskill, String gender) {
        this.name = name;
        this.myskill = myskill;
        this.goalskill = goalskill;
        this.gender = gender;
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
}

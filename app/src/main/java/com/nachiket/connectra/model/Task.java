package com.nachiket.connectra.model;

import com.google.firebase.auth.FirebaseAuth;

public class Task {
    private String id;
    private String title;
    private boolean checked;
    private String ownerId;

    public Task() {}

    public Task(String id, String title, boolean checked) {
        this.id = id;
        this.title = title;
        this.checked = checked;
        this.ownerId = FirebaseAuth.getInstance().getUid();
    }

    public String getOwnerId() { return ownerId; }

    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isChecked() { return checked; }

    public void setChecked(boolean checked) { this.checked = checked; }
}
package com.example.taskmateapp;

public class Task {
    private String id;
    private String title;
    private boolean completed;
    private String uid;

    public Task() {
        // Required for Firebase
    }

    public Task(String title, boolean completed) {
        this.title = title;
        this.completed = completed;
    }

    public Task(String id, String title, boolean completed, String uid) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.uid = uid;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getUid() {
        return uid;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
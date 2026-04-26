package org.example.entities;

import java.util.Date;

public class EventNotification {

    private int id;
    private int userId;
    private int eventId;
    private String title;
    private String message;
    private Date createdAt;
    private boolean read;

    public EventNotification() {
    }

    public EventNotification(int userId, int eventId, String title, String message, Date createdAt, boolean read) {
        this.userId = userId;
        this.eventId = eventId;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
        this.read = read;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
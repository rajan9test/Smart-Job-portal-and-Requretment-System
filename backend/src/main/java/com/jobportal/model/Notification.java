package com.jobportal.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Notification implements Identifiable {

    private Long id;
    private final Long recipientUserId;
    private final String message;
    private final NotificationChannel channel;
    private final LocalDateTime createdAt;
    private boolean read;

    public Notification(Long recipientUserId, String message, NotificationChannel channel, LocalDateTime createdAt) {
        this.recipientUserId = Objects.requireNonNull(recipientUserId, "recipientUserId");
        this.message = Objects.requireNonNull(message, "message");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public String getMessage() {
        return message;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void markRead() {
        this.read = true;
    }

    @Override
    public String toString() {
        return "Notification{to=%d, channel=%s, message='%s'}".formatted(recipientUserId, channel, message);
    }
}

package com.jobportal.service;

import com.jobportal.exception.UnauthorizedException;
import com.jobportal.model.Notification;
import com.jobportal.model.User;
import com.jobportal.repository.NotificationRepository;

import java.util.Comparator;
import java.util.List;

/** Read side of in-app notifications (the write side is NotificationDispatcher). */
public class InboxService {

    private final NotificationRepository notifications;

    public InboxService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /** The user's in-app notifications, newest first. */
    public List<Notification> getInbox(User user) {
        return notifications.findByRecipientUserId(user.getId()).stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed()
                        .thenComparing(Notification::getId, Comparator.reverseOrder()))
                .toList();
    }

    public long unreadCount(User user) {
        return notifications.findByRecipientUserId(user.getId()).stream().filter(n -> !n.isRead()).count();
    }

    public void markRead(User user, Long notificationId) {
        notifications.findById(notificationId)
                .filter(n -> n.getRecipientUserId().equals(user.getId()))
                .orElseThrow(() -> new UnauthorizedException("Not your notification"))
                .markRead();
    }

    public void markAllRead(User user) {
        notifications.findByRecipientUserId(user.getId()).forEach(Notification::markRead);
    }
}

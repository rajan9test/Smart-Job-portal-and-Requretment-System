package com.jobportal.notification;

import com.jobportal.model.Notification;
import com.jobportal.model.NotificationChannel;
import com.jobportal.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stores the notification so the user sees it in their in-app inbox. */
public class InAppNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationService.class);

    private final NotificationRepository repository;

    public InAppNotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {
        repository.save(notification);
        log.info("IN_APP -> user#{}: {}", notification.getRecipientUserId(), notification.getMessage());
    }
}

package com.jobportal.notification;

import com.jobportal.model.Notification;
import com.jobportal.model.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simulated email gateway. Phase 6: replace the log line with JavaMail / an SES client. */
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        log.info("EMAIL  -> user#{}: {}", notification.getRecipientUserId(), notification.getMessage());
    }
}

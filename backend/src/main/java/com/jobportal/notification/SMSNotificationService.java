package com.jobportal.notification;

import com.jobportal.model.Notification;
import com.jobportal.model.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simulated SMS gateway. */
public class SMSNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(SMSNotificationService.class);
    private static final int SMS_MAX_LENGTH = 160;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(Notification notification) {
        String text = notification.getMessage();
        if (text.length() > SMS_MAX_LENGTH) {
            text = text.substring(0, SMS_MAX_LENGTH - 3) + "...";
        }
        log.info("SMS    -> user#{}: {}", notification.getRecipientUserId(), text);
    }
}

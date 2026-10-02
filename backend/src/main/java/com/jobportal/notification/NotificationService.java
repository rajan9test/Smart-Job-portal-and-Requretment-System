package com.jobportal.notification;

import com.jobportal.model.Notification;
import com.jobportal.model.NotificationChannel;

/**
 * One delivery channel. Callers depend on this interface, never on a concrete channel
 * (dependency inversion), so adding PushNotificationService touches only the factory.
 */
public interface NotificationService {

    NotificationChannel channel();

    void send(Notification notification);
}

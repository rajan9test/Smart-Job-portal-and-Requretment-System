package com.jobportal.notification;

import com.jobportal.model.NotificationChannel;
import com.jobportal.repository.NotificationRepository;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory pattern: maps a channel to the service that delivers on it. Instances are stateless,
 * so each is created once and cached in an EnumMap (array-backed, faster than a HashMap for enum keys).
 */
public class NotificationFactory {

    private final Map<NotificationChannel, NotificationService> services = new EnumMap<>(NotificationChannel.class);

    public NotificationFactory(NotificationRepository notificationRepository) {
        for (NotificationChannel channel : NotificationChannel.values()) {
            services.put(channel, switch (channel) {
                case EMAIL -> new EmailNotificationService();
                case SMS -> new SMSNotificationService();
                case IN_APP -> new InAppNotificationService(notificationRepository);
            });
        }
    }

    public NotificationService create(NotificationChannel channel) {
        return services.get(channel);
    }
}

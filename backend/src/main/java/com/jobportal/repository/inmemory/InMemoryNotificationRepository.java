package com.jobportal.repository.inmemory;

import com.jobportal.model.Notification;
import com.jobportal.repository.NotificationRepository;

import java.util.List;

public class InMemoryNotificationRepository extends InMemoryRepository<Notification> implements NotificationRepository {

    @Override
    public List<Notification> findByRecipientUserId(Long userId) {
        return findWhere(n -> n.getRecipientUserId().equals(userId));
    }
}

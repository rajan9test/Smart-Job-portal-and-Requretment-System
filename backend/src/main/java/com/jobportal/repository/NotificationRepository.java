package com.jobportal.repository;

import com.jobportal.model.Notification;

import java.util.List;

public interface NotificationRepository extends Repository<Notification, Long> {

    List<Notification> findByRecipientUserId(Long userId);
}

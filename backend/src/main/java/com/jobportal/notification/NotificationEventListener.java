package com.jobportal.notification;

import com.jobportal.event.PortalEvent;
import com.jobportal.event.PortalEvent.ApplicationStatusChanged;
import com.jobportal.event.PortalEvent.ApplicationSubmitted;
import com.jobportal.event.PortalEvent.InterviewCancelled;
import com.jobportal.event.PortalEvent.InterviewRescheduled;
import com.jobportal.event.PortalEvent.InterviewScheduled;
import com.jobportal.event.PortalEventListener;
import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.Notification;
import com.jobportal.model.NotificationChannel;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Bridges domain events to notifications: decides who hears about what, then hands the
 * messages to the dispatcher queue.
 */
public class NotificationEventListener implements PortalEventListener {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final NotificationDispatcher dispatcher;
    private final List<NotificationChannel> channels;
    private final Clock clock;

    public NotificationEventListener(NotificationDispatcher dispatcher, List<NotificationChannel> channels, Clock clock) {
        this.dispatcher = dispatcher;
        this.channels = List.copyOf(channels);
        this.clock = clock;
    }

    @Override
    public void onEvent(PortalEvent event) {
        // Pattern matching switch over a sealed interface: exhaustive, no default needed.
        switch (event) {
            case ApplicationSubmitted e -> notify(e.recruiterId(),
                    "%s applied for %s".formatted(e.candidateName(), e.jobTitle()));
            case ApplicationStatusChanged e when e.to() == ApplicationStatus.WITHDRAWN -> notify(e.recruiterId(),
                    "A candidate withdrew their application for %s".formatted(e.jobTitle()));
            case ApplicationStatusChanged e -> notify(e.candidateId(),
                    "Your application for %s is now %s".formatted(e.jobTitle(), e.to()));
            case InterviewScheduled e -> notify(e.candidateId(),
                    "Interview for %s scheduled on %s".formatted(e.jobTitle(), e.slot().start().format(WHEN)));
            case InterviewRescheduled e -> notify(e.candidateId(),
                    "Interview for %s moved to %s".formatted(e.jobTitle(), e.newSlot().start().format(WHEN)));
            case InterviewCancelled e -> notify(e.candidateId(),
                    "Interview for %s was cancelled: %s".formatted(e.jobTitle(), e.reason()));
        }
    }

    private void notify(Long userId, String message) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (NotificationChannel channel : channels) {
            dispatcher.submit(new Notification(userId, message, channel, now));
        }
    }
}

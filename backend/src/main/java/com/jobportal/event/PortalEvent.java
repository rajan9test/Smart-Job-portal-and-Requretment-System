package com.jobportal.event;

import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.TimeSlot;

/**
 * Domain events published by services when something worth reacting to happens.
 * Sealed: the compiler knows every subtype, so a switch over PortalEvent needs no default branch
 * and fails to compile if you add a new event and forget to handle it.
 */
public sealed interface PortalEvent {

    record ApplicationSubmitted(Long applicationId, Long candidateId, String candidateName,
                                Long recruiterId, String jobTitle) implements PortalEvent {
    }

    record ApplicationStatusChanged(Long applicationId, Long candidateId, Long recruiterId, String jobTitle,
                                    ApplicationStatus from, ApplicationStatus to) implements PortalEvent {
    }

    record InterviewScheduled(Long interviewId, Long candidateId, String jobTitle, TimeSlot slot)
            implements PortalEvent {
    }

    record InterviewRescheduled(Long interviewId, Long candidateId, String jobTitle, TimeSlot newSlot)
            implements PortalEvent {
    }

    record InterviewCancelled(Long interviewId, Long candidateId, String jobTitle, String reason)
            implements PortalEvent {
    }
}

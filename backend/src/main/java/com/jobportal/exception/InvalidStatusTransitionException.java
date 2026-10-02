package com.jobportal.exception;

import com.jobportal.model.ApplicationStatus;

public class InvalidStatusTransitionException extends JobPortalException {
    public InvalidStatusTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super("Cannot move application from " + from + " to " + to);
    }
}

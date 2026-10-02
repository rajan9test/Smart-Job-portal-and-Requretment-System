package com.jobportal.exception;

/**
 * Root of every business exception in the portal. Unchecked, so services stay readable;
 * in Phase 3 a single @ControllerAdvice can map each subtype to an HTTP status.
 */
public abstract class JobPortalException extends RuntimeException {
    protected JobPortalException(String message) {
        super(message);
    }
}

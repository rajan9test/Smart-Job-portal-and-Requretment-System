package com.jobportal.exception;

public class DuplicateEmailException extends JobPortalException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}

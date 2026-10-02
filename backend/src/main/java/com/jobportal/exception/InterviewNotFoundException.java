package com.jobportal.exception;

public class InterviewNotFoundException extends NotFoundException {
    public InterviewNotFoundException(Object id) {
        super("Interview", id);
    }
}

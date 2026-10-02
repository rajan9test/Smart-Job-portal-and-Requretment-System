package com.jobportal.exception;

public class ApplicationNotFoundException extends NotFoundException {
    public ApplicationNotFoundException(Object id) {
        super("Application", id);
    }
}

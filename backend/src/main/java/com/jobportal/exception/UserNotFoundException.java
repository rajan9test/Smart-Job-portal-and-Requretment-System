package com.jobportal.exception;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(Object id) {
        super("User", id);
    }
}

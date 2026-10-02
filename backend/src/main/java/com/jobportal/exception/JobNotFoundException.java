package com.jobportal.exception;

public class JobNotFoundException extends NotFoundException {
    public JobNotFoundException(Object id) {
        super("Job", id);
    }
}

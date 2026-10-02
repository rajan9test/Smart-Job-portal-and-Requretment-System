package com.jobportal.exception;

public class CompanyNotFoundException extends NotFoundException {
    public CompanyNotFoundException(Object id) {
        super("Company", id);
    }
}

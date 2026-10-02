package com.jobportal.exception;

/** Base for "entity does not exist" errors (maps to HTTP 404 later). */
public abstract class NotFoundException extends JobPortalException {
    protected NotFoundException(String entity, Object id) {
        super(entity + " not found: " + id);
    }
}

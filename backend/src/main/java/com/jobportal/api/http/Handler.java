package com.jobportal.api.http;

/** One endpoint. Return a body object (200), a {@link Response}, or null (204). */
@FunctionalInterface
public interface Handler {
    Object handle(Request request) throws Exception;
}

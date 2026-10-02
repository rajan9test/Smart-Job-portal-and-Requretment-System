package com.jobportal.api.http;

/** An error raised by the HTTP layer itself, carrying the status code to return. */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public static ApiException unauthenticated(String message) {
        return new ApiException(401, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(403, message);
    }
}

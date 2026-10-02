package com.jobportal.api.http;

/**
 * Lets a handler choose the status code. Handlers that return any other object get 200 OK
 * with that object as the JSON body.
 */
public record Response(int status, Object body) {

    public static Response created(Object body) {
        return new Response(201, body);
    }

    public static Response noContent() {
        return new Response(204, null);
    }
}

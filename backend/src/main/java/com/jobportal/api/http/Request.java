package com.jobportal.api.http;

import com.fasterxml.jackson.core.JacksonException;
import com.jobportal.exception.UnauthorizedException;
import com.jobportal.model.Admin;
import com.jobportal.model.Candidate;
import com.jobportal.model.Recruiter;
import com.jobportal.model.User;
import com.jobportal.service.AuthService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps one HTTP exchange with typed accessors: path variables, query parameters, JSON body,
 * and the authenticated user.
 */
public class Request {

    private final HttpExchange exchange;
    private final Map<String, String> pathParams;
    private final Map<String, String> queryParams;
    private final AuthService auth;
    private User user;

    Request(HttpExchange exchange, Map<String, String> pathParams, AuthService auth) {
        this.exchange = exchange;
        this.pathParams = pathParams;
        this.queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
        this.auth = auth;
    }

    // ---------- path & query ----------

    public long pathLong(String name) {
        String value = pathParams.get(name);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Path variable '" + name + "' must be a number, got: " + value);
        }
    }

    /** Query parameter, or null if absent or blank. */
    public String query(String name) {
        String value = queryParams.get(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String query(String name, String defaultValue) {
        String value = query(name);
        return value == null ? defaultValue : value;
    }

    public Integer queryInt(String name) {
        String value = query(name);
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Query parameter '" + name + "' must be a whole number");
        }
    }

    public Long queryLong(String name) {
        String value = query(name);
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Query parameter '" + name + "' must be a number");
        }
    }

    // ---------- body ----------

    public <T> T body(Class<T> type) {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) {
                throw ApiException.badRequest("Request body is required");
            }
            return Json.mapper().readValue(bytes, type);
        } catch (JacksonException e) {
            throw ApiException.badRequest("Invalid JSON: " + e.getOriginalMessage());
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read request body");
        }
    }

    // ---------- authentication & roles ----------

    /** Bearer token from the Authorization header, or null. */
    public String token() {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring("Bearer ".length()).trim();
    }

    /** The logged-in user; 401 if there is no valid session. */
    public User user() {
        if (user == null) {
            String token = token();
            if (token == null) throw ApiException.unauthenticated("Login required");
            try {
                user = auth.authenticate(token);
            } catch (UnauthorizedException e) {
                throw ApiException.unauthenticated(e.getMessage());
            }
        }
        return user;
    }

    public Candidate candidate() {
        return requireType(Candidate.class, "CANDIDATE");
    }

    public Recruiter recruiter() {
        return requireType(Recruiter.class, "RECRUITER");
    }

    public Admin admin() {
        return requireType(Admin.class, "ADMIN");
    }

    private <T extends User> T requireType(Class<T> type, String roleName) {
        User current = user();
        if (!type.isInstance(current)) {
            throw ApiException.forbidden("This action requires role " + roleName);
        }
        return type.cast(current);
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> params = new HashMap<>();
        if (raw == null || raw.isEmpty()) return params;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }
}

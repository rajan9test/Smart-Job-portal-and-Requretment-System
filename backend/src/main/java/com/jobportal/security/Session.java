package com.jobportal.security;

import com.jobportal.model.Role;

import java.time.LocalDateTime;

/**
 * A logged-in session identified by an opaque random token. Phase 4 replaces this with a signed JWT
 * carrying the same claims (subject = userId, role, expiry).
 */
public record Session(String token, Long userId, Role role, LocalDateTime expiresAt) {

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }
}

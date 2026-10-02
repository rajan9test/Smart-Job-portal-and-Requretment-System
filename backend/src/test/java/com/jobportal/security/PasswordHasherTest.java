package com.jobportal.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(1_000);

    @Test
    void matchesOriginalPasswordOnly() {
        String stored = hasher.hash("correct horse battery staple");
        assertTrue(hasher.matches("correct horse battery staple", stored));
        assertFalse(hasher.matches("Correct horse battery staple", stored));
    }

    @Test
    void sameInputGivesDifferentHashesBecauseOfSalt() {
        assertNotEquals(hasher.hash("password123"), hasher.hash("password123"));
    }

    @Test
    void hashesFromOtherIterationCountsStillVerify() {
        String oldHash = new PasswordHasher(500).hash("password123");
        assertTrue(hasher.matches("password123", oldHash));
    }

    @Test
    void malformedStoredValueDoesNotMatch() {
        assertFalse(hasher.matches("x", "not-a-hash"));
    }
}

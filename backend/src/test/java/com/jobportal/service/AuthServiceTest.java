package com.jobportal.service;

import com.jobportal.exception.CompanyNotFoundException;
import com.jobportal.exception.DuplicateEmailException;
import com.jobportal.exception.InvalidCredentialsException;
import com.jobportal.exception.UnauthorizedException;
import com.jobportal.exception.UserBlockedException;
import com.jobportal.model.Candidate;
import com.jobportal.model.Company;
import com.jobportal.model.Role;
import com.jobportal.repository.inmemory.InMemoryCompanyRepository;
import com.jobportal.repository.inmemory.InMemoryUserRepository;
import com.jobportal.security.PasswordHasher;
import com.jobportal.security.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    /** A clock tests can move forward, to check session expiry without sleeping. */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-02T09:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private InMemoryUserRepository users;
    private InMemoryCompanyRepository companies;
    private MutableClock clock;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        companies = new InMemoryCompanyRepository();
        clock = new MutableClock();
        // Low iteration count keeps tests fast; production uses the value from application.properties.
        auth = new AuthService(users, companies, new PasswordHasher(1_000), clock, Duration.ofMinutes(30), 8);
    }

    @Test
    void registerHashesPasswordAndLoginWorks() {
        Candidate c = auth.registerCandidate("Rajan", "Rajan@Mail.dev", null, "s3cret-pass");

        assertNotEquals("s3cret-pass", c.getPasswordHash());
        Session session = auth.login("rajan@mail.dev", "s3cret-pass"); // email is case-insensitive
        assertEquals(Role.CANDIDATE, session.role());
        assertEquals(c, auth.authenticate(session.token()));
    }

    @Test
    void wrongPasswordAndUnknownEmailFailTheSameWay() {
        auth.registerCandidate("Rajan", "rajan@mail.dev", null, "s3cret-pass");

        var wrongPassword = assertThrows(InvalidCredentialsException.class, () -> auth.login("rajan@mail.dev", "nope-nope"));
        var unknownEmail = assertThrows(InvalidCredentialsException.class, () -> auth.login("ghost@mail.dev", "s3cret-pass"));
        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

    @Test
    void duplicateEmailRejected() {
        auth.registerCandidate("Rajan", "rajan@mail.dev", null, "s3cret-pass");
        assertThrows(DuplicateEmailException.class,
                () -> auth.registerCandidate("Other", "RAJAN@mail.dev", null, "s3cret-pass"));
    }

    @Test
    void shortPasswordRejected() {
        assertThrows(IllegalArgumentException.class, () -> auth.registerCandidate("R", "r@mail.dev", null, "short"));
    }

    @Test
    void recruiterNeedsExistingCompany() {
        assertThrows(CompanyNotFoundException.class,
                () -> auth.registerRecruiter("Amit", "amit@acme.dev", null, "s3cret-pass", 42L));
        Company acme = companies.save(new Company("Acme", "Pune", null));
        assertEquals(acme.getId(), auth.registerRecruiter("Amit", "amit@acme.dev", null, "s3cret-pass", acme.getId()).getCompanyId());
    }

    @Test
    void blockedUserCannotLoginAndExistingSessionStops() {
        Candidate c = auth.registerCandidate("Rajan", "rajan@mail.dev", null, "s3cret-pass");
        Session session = auth.login("rajan@mail.dev", "s3cret-pass");

        c.setBlocked(true);

        assertThrows(UserBlockedException.class, () -> auth.authenticate(session.token()));
        assertThrows(UserBlockedException.class, () -> auth.login("rajan@mail.dev", "s3cret-pass"));
    }

    @Test
    void sessionExpires() {
        auth.registerCandidate("Rajan", "rajan@mail.dev", null, "s3cret-pass");
        Session session = auth.login("rajan@mail.dev", "s3cret-pass");

        clock.advance(Duration.ofMinutes(31));

        assertThrows(UnauthorizedException.class, () -> auth.authenticate(session.token()));
    }

    @Test
    void logoutInvalidatesToken() {
        auth.registerCandidate("Rajan", "rajan@mail.dev", null, "s3cret-pass");
        Session session = auth.login("rajan@mail.dev", "s3cret-pass");
        auth.logout(session.token());
        assertThrows(UnauthorizedException.class, () -> auth.authenticate(session.token()));
    }
}

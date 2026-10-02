package com.jobportal.service;

import com.jobportal.exception.CompanyNotFoundException;
import com.jobportal.exception.DuplicateEmailException;
import com.jobportal.exception.InvalidCredentialsException;
import com.jobportal.exception.UnauthorizedException;
import com.jobportal.exception.UserBlockedException;
import com.jobportal.model.Admin;
import com.jobportal.model.Candidate;
import com.jobportal.model.Recruiter;
import com.jobportal.model.User;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.PasswordHasher;
import com.jobportal.security.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Register -> hash password -> login -> session token -> authenticate each request.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final CompanyRepository companies;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final Duration sessionTtl;
    private final int minPasswordLength;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(UserRepository users, CompanyRepository companies, PasswordHasher hasher,
                       Clock clock, Duration sessionTtl, int minPasswordLength) {
        this.users = users;
        this.companies = companies;
        this.hasher = hasher;
        this.clock = clock;
        this.sessionTtl = sessionTtl;
        this.minPasswordLength = minPasswordLength;
    }

    public Candidate registerCandidate(String name, String email, String phone, String rawPassword) {
        String hash = prepareRegistration(email, rawPassword);
        Candidate candidate = (Candidate) users.save(new Candidate(name, email, phone, hash));
        log.info("Registered candidate {}", candidate.getId());
        return candidate;
    }

    public Recruiter registerRecruiter(String name, String email, String phone, String rawPassword, Long companyId) {
        if (!companies.existsById(companyId)) {
            throw new CompanyNotFoundException(companyId);
        }
        String hash = prepareRegistration(email, rawPassword);
        Recruiter recruiter = (Recruiter) users.save(new Recruiter(name, email, phone, hash, companyId));
        log.info("Registered recruiter {} for company {}", recruiter.getId(), companyId);
        return recruiter;
    }

    /** Admins are not self-registered; this is used for seeding / by other admins. */
    public Admin createAdmin(String name, String email, String rawPassword) {
        String hash = prepareRegistration(email, rawPassword);
        return (Admin) users.save(new Admin(name, email, null, hash));
    }

    private String prepareRegistration(String email, String rawPassword) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (rawPassword == null || rawPassword.length() < minPasswordLength) {
            throw new IllegalArgumentException("Password must be at least " + minPasswordLength + " characters");
        }
        if (users.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException("Email already registered: " + email);
        }
        return hasher.hash(rawPassword);
    }

    public Session login(String email, String rawPassword) {
        // Same message for "no such user" and "wrong password": don't reveal which emails exist.
        User user = users.findByEmail(email)
                .filter(u -> hasher.matches(rawPassword, u.getPasswordHash()))
                .orElseThrow(() -> {
                    log.warn("Failed login attempt for {}", email);
                    return new InvalidCredentialsException("Invalid email or password");
                });
        if (user.isBlocked()) {
            throw new UserBlockedException("Account is blocked");
        }
        Session session = new Session(newToken(), user.getId(), user.getRole(), LocalDateTime.now(clock).plus(sessionTtl));
        sessions.put(session.token(), session);
        log.info("User {} logged in as {}", user.getId(), user.getRole());
        return session;
    }

    public void logout(String token) {
        Session removed = sessions.remove(token);
        if (removed != null) {
            log.info("User {} logged out", removed.userId());
        }
    }

    /** Resolves a token to its user, rejecting unknown, expired and blocked sessions. */
    public User authenticate(String token) {
        Session session = Optional.ofNullable(token).map(sessions::get)
                .orElseThrow(() -> new UnauthorizedException("Not logged in"));
        if (session.isExpired(LocalDateTime.now(clock))) {
            sessions.remove(token);
            throw new UnauthorizedException("Session expired");
        }
        User user = users.findById(session.userId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        if (user.isBlocked()) {
            sessions.remove(token);
            throw new UserBlockedException("Account is blocked");
        }
        return user;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

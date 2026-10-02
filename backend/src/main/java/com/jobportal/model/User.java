package com.jobportal.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base type for every account on the platform.
 * Encapsulation: fields are private, the role is fixed at construction and cannot change.
 * Abstraction: each subtype decides what its dashboard looks like.
 */
public abstract class User implements Identifiable {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String passwordHash;
    private final Role role;
    private boolean blocked;
    private final LocalDateTime createdAt;

    protected User(String name, String email, String phone, String passwordHash, Role role) {
        this.name = Objects.requireNonNull(name, "name");
        this.email = Objects.requireNonNull(email, "email").trim().toLowerCase();
        this.phone = phone;
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
        this.createdAt = LocalDateTime.now();
    }

    /** Polymorphism: Candidate, Recruiter and Admin each render a different dashboard. */
    public abstract String showDashboard();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public Role getRole() {
        return role;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "%s{id=%d, name='%s', email='%s'}".formatted(getClass().getSimpleName(), id, name, email);
    }
}

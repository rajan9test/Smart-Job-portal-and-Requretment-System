package com.jobportal.model;

public enum Role {
    CANDIDATE,
    RECRUITER,
    ADMIN;

    /** Spring Security style authority name, e.g. ROLE_RECRUITER. Useful in Phase 4. */
    public String authority() {
        return "ROLE_" + name();
    }
}

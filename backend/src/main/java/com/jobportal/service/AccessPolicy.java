package com.jobportal.service;

import com.jobportal.exception.UnauthorizedException;
import com.jobportal.model.Job;
import com.jobportal.model.Recruiter;
import com.jobportal.model.Role;
import com.jobportal.model.User;

import java.util.Arrays;

/**
 * Central place for "is this user allowed to do that?" decisions, so the rules aren't scattered
 * across services. In Phase 4 this becomes @PreAuthorize expressions / a Spring Security bean.
 */
public class AccessPolicy {

    /** Role-based check, e.g. only RECRUITERs may post jobs. */
    public void requireRole(User user, Role... allowed) {
        if (user == null || Arrays.stream(allowed).noneMatch(r -> r == user.getRole())) {
            throw new UnauthorizedException("Requires role " + Arrays.toString(allowed));
        }
    }

    /**
     * Ownership check: may this recruiter edit / close / delete this job and manage its applicants?
     *
     * TODO(#7): implement.
     *   Decide the rule yourself and write it down in this comment. Things to consider:
     *   - Should ANY recruiter from the job's company be allowed, or only the one who posted it?
     *     (Real portals usually allow the whole hiring team, i.e. same companyId.)
     *   - A recruiter with no company (companyId == null) must never pass.
     *   - Beware Long comparisons: {@code Long a == Long b} compares references, and only works
     *     by accident for small values (-128..127) because of the Integer/Long cache. Try it!
     *   JobServiceTest has the test "recruiter from another company cannot modify job".
     */
    public boolean canModifyJob(Recruiter recruiter, Job job) {
        throw new UnsupportedOperationException("TODO(#7): AccessPolicy.canModifyJob");
    }

    public void requireCanModifyJob(Recruiter recruiter, Job job) {
        if (!canModifyJob(recruiter, job)) {
            throw new UnauthorizedException("Recruiter " + recruiter.getId() + " cannot manage job " + job.getId());
        }
    }
}

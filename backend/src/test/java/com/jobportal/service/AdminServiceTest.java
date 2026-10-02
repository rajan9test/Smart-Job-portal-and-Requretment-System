package com.jobportal.service;

import com.jobportal.exception.UnauthorizedException;
import com.jobportal.model.Admin;
import com.jobportal.model.Application;
import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.Candidate;
import com.jobportal.model.Job;
import com.jobportal.model.Recruiter;
import com.jobportal.model.Role;
import com.jobportal.repository.inmemory.InMemoryApplicationRepository;
import com.jobportal.repository.inmemory.InMemoryJobRepository;
import com.jobportal.repository.inmemory.InMemoryUserRepository;
import com.jobportal.service.dto.PlatformStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static com.jobportal.TestData.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminServiceTest {

    private InMemoryUserRepository users;
    private InMemoryJobRepository jobs;
    private InMemoryApplicationRepository applications;
    private AdminService service;
    private Admin admin;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        jobs = new InMemoryJobRepository();
        applications = new InMemoryApplicationRepository();
        service = new AdminService(users, jobs, applications);
        admin = (Admin) users.save(new Admin("Root", "root@portal.dev", null, "hash"));
    }

    @Test
    void blockAndUnblock() {
        Candidate c = (Candidate) users.save(new Candidate("C", "c@mail.dev", null, "hash"));
        service.blockUser(admin, c.getId());
        assertTrue(users.findById(c.getId()).orElseThrow().isBlocked());
        service.unblockUser(admin, c.getId());
        assertTrue(!users.findById(c.getId()).orElseThrow().isBlocked());
    }

    @Test
    void adminCannotBlockThemselves() {
        assertThrows(UnauthorizedException.class, () -> service.blockUser(admin, admin.getId()));
    }

    @Test
    void statisticsOnEmptyPlatform() {
        PlatformStats stats = service.getStatistics(admin); // TODO(#11)

        assertEquals(1, stats.totalUsers());
        assertEquals(Map.of(Role.ADMIN, 1L, Role.CANDIDATE, 0L, Role.RECRUITER, 0L), stats.usersByRole());
        assertEquals(0, stats.totalApplications());
        assertEquals(Optional.empty(), stats.mostAppliedJobTitle());
    }

    @Test
    void statistics() {
        Candidate c1 = (Candidate) users.save(new Candidate("C1", "c1@mail.dev", null, "hash"));
        Candidate c2 = (Candidate) users.save(new Candidate("C2", "c2@mail.dev", null, "hash"));
        Candidate c3 = (Candidate) users.save(new Candidate("C3", "c3@mail.dev", null, "hash"));
        Recruiter r = (Recruiter) users.save(new Recruiter("R", "r@corp.dev", null, "hash", 1L));

        Job popular = jobs.save(new Job("Popular Job", 1L, r.getId()));
        Job quiet = jobs.save(new Job("Quiet Job", 1L, r.getId()));
        Job closed = jobs.save(new Job("Closed Job", 1L, r.getId()));
        closed.close();

        applications.save(new Application(popular.getId(), c1.getId(), NOW));
        applications.save(new Application(popular.getId(), c2.getId(), NOW));
        Application rejected = new Application(popular.getId(), c3.getId(), NOW);
        rejected.changeStatus(ApplicationStatus.REJECTED, NOW); // needs TODO(#2)
        applications.save(rejected);
        applications.save(new Application(quiet.getId(), c1.getId(), NOW));

        PlatformStats stats = service.getStatistics(admin); // TODO(#11)

        assertEquals(5, stats.totalUsers());
        assertEquals(3L, stats.usersByRole().get(Role.CANDIDATE));
        assertEquals(1L, stats.usersByRole().get(Role.RECRUITER));
        assertEquals(1L, stats.usersByRole().get(Role.ADMIN));
        assertEquals(2, stats.activeJobs());
        assertEquals(3, stats.totalJobs());
        assertEquals(4, stats.totalApplications());
        assertEquals(3L, stats.applicationsByStatus().get(ApplicationStatus.APPLIED));
        assertEquals(1L, stats.applicationsByStatus().get(ApplicationStatus.REJECTED));
        assertEquals(Optional.of("Popular Job"), stats.mostAppliedJobTitle());
    }
}

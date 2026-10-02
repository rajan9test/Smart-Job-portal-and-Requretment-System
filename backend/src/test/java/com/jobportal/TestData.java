package com.jobportal;

import com.jobportal.model.Candidate;
import com.jobportal.model.Job;
import com.jobportal.model.JobType;
import com.jobportal.model.Recruiter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

/** Small factory for test objects so each test only spells out the fields it cares about. */
public final class TestData {

    /** All tests run "now" = 2 Oct 2026, 09:00 UTC, so date logic is deterministic. */
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T09:00:00Z"), ZoneOffset.UTC);
    public static final LocalDate TODAY = LocalDate.now(CLOCK);
    public static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private TestData() {
    }

    public static Candidate candidate(long id, String name, String... skills) {
        Candidate c = new Candidate(name, name.toLowerCase() + "@mail.dev", null, "hash");
        c.setId(id);
        c.setSkills(Set.of(skills));
        return c;
    }

    public static Recruiter recruiter(long id, Long companyId) {
        Recruiter r = new Recruiter("Recruiter" + id, "recruiter" + id + "@corp.dev", null, "hash", companyId);
        r.setId(id);
        return r;
    }

    /** An OPEN full-time job in Pune, 1-3 yrs, 5-10 LPA, deadline in 30 days. */
    public static Job job(long id, long companyId, long recruiterId, String title, String... skills) {
        Job job = new Job(title, companyId, recruiterId);
        job.setId(id);
        job.setRequiredSkills(Set.of(skills));
        job.setExperienceRange(1, 3);
        job.setSalaryRange(500_000, 1_000_000);
        job.setLocation("Pune");
        job.setJobType(JobType.FULL_TIME);
        job.setOpenings(1);
        job.setDeadline(TODAY.plusDays(30));
        return job;
    }
}

package com.jobportal.service;

import com.jobportal.exception.UnauthorizedException;
import com.jobportal.model.Company;
import com.jobportal.model.Job;
import com.jobportal.model.JobStatus;
import com.jobportal.model.JobType;
import com.jobportal.model.Recruiter;
import com.jobportal.repository.inmemory.InMemoryCompanyRepository;
import com.jobportal.repository.inmemory.InMemoryJobRepository;
import com.jobportal.service.dto.JobRequest;
import com.jobportal.service.dto.JobSearchCriteria;
import com.jobportal.service.dto.Page;
import com.jobportal.service.dto.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.jobportal.TestData.TODAY;
import static com.jobportal.TestData.recruiter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses real in-memory repositories: these are fast and make the tests read like real usage. */
class JobServiceTest {

    private InMemoryJobRepository jobs;
    private InMemoryCompanyRepository companies;
    private JobService service;
    private Recruiter acmeRecruiter;
    private Recruiter acmeColleague;
    private Recruiter globexRecruiter;

    @BeforeEach
    void setUp() {
        jobs = new InMemoryJobRepository();
        companies = new InMemoryCompanyRepository();
        service = new JobService(jobs, companies, new AccessPolicy());
        Company acme = companies.save(new Company("Acme Tech", "Pune", null));
        Company globex = companies.save(new Company("Globex", "Bengaluru", null));
        acmeRecruiter = recruiter(1000, acme.getId());
        acmeColleague = recruiter(1001, acme.getId());
        globexRecruiter = recruiter(2000, globex.getId());
    }

    private static JobRequest request(String title, String location, int minExp, int maxExp,
                                      long minSal, long maxSal, JobType type, String... skills) {
        return new JobRequest(title, "desc", Set.of(skills), minExp, maxExp, minSal, maxSal,
                location, type, 1, TODAY.plusDays(30));
    }

    @Nested
    @DisplayName("Ownership - TODO(#7)")
    class Ownership {
        private Job job;

        @BeforeEach
        void createJob() {
            job = service.createJob(acmeRecruiter,
                    request("Java Dev", "Pune", 1, 3, 500_000, 1_000_000, JobType.FULL_TIME, "Java"));
        }

        @Test
        void ownerCanCloseAndReopen() {
            assertEquals(JobStatus.CLOSED, service.closeJob(acmeRecruiter, job.getId()).getStatus());
            assertEquals(JobStatus.OPEN, service.reopenJob(acmeRecruiter, job.getId()).getStatus());
        }

        @Test
        @DisplayName("Unauthorized recruiter cannot modify another company's job")
        void recruiterFromAnotherCompanyCannotModifyJob() {
            JobRequest update = request("Hijacked", "Pune", 0, 1, 0, 1, JobType.FULL_TIME);
            assertThrows(UnauthorizedException.class, () -> service.updateJob(globexRecruiter, job.getId(), update));
            assertThrows(UnauthorizedException.class, () -> service.closeJob(globexRecruiter, job.getId()));
            assertThrows(UnauthorizedException.class, () -> service.deleteJob(globexRecruiter, job.getId()));
            assertEquals("Java Dev", service.getJob(job.getId()).getTitle());
        }

        @Test
        void recruiterWithoutCompanyCannotModifyJob() {
            assertThrows(UnauthorizedException.class, () -> service.closeJob(recruiter(3000, null), job.getId()));
        }

        @Test
        void largeIdsAreComparedByValue() {
            // Ids above 127 fall outside the Long cache, so each literal below autoboxes to a DIFFERENT
            // Long object. Comparing them with == returns false; equals() returns true.
            Recruiter poster = recruiter(5000, 1_000_000L);
            Job bigJob = new Job("Big", 1_000_000L, 5000L);
            assertTrue(new AccessPolicy().canModifyJob(poster, bigJob));
        }
    }

    @Nested
    @DisplayName("Search - TODO(#10)")
    class Search {

        @BeforeEach
        void seed() {
            service.createJob(acmeRecruiter, request("Java Backend Developer", "Pune", 1, 3, 500_000, 1_000_000, JobType.FULL_TIME, "Java", "Spring Boot", "SQL"));
            service.createJob(acmeColleague, request("Senior Java Engineer", "Pune", 5, 8, 2_000_000, 3_000_000, JobType.FULL_TIME, "Java", "AWS"));
            service.createJob(globexRecruiter, request("React Developer", "Bengaluru", 0, 2, 400_000, 700_000, JobType.FULL_TIME, "React", "TypeScript"));
            service.createJob(globexRecruiter, request("Java Intern", "pune", 0, 0, 100_000, 200_000, JobType.INTERNSHIP, "Java"));
            Job closed = service.createJob(globexRecruiter, request("Java Contractor", "Pune", 2, 6, 900_000, 1_500_000, JobType.CONTRACT, "Java", "SQL"));
            closed.close();
        }

        private List<String> titles(JobSearchCriteria criteria) {
            return service.search(criteria, PageRequest.of(0, 50, "title,asc")).content().stream()
                    .map(Job::getTitle).toList();
        }

        @Test
        void noCriteriaReturnsAllOpenJobs() {
            assertEquals(4, service.search(JobSearchCriteria.builder().build(), PageRequest.of(0, 50)).totalElements());
        }

        @Test
        void includeClosed() {
            assertEquals(5, service.search(JobSearchCriteria.builder().includeClosed(true).build(), PageRequest.of(0, 50)).totalElements());
        }

        @Test
        void byTitleSubstringIgnoringCase() {
            assertEquals(List.of("Java Backend Developer", "Java Intern", "Senior Java Engineer"),
                    titles(JobSearchCriteria.builder().title("jAvA").build()));
        }

        @Test
        void byLocationIgnoringCase() {
            assertEquals(List.of("Java Backend Developer", "Java Intern", "Senior Java Engineer"),
                    titles(JobSearchCriteria.builder().location("PUNE").build()));
        }

        @Test
        void byCompanyName() {
            assertEquals(List.of("Java Intern", "React Developer"),
                    titles(JobSearchCriteria.builder().companyName("glob").build()));
        }

        @Test
        void byExperienceInsideJobRange() {
            assertEquals(List.of("Java Backend Developer", "React Developer"),
                    titles(JobSearchCriteria.builder().experienceYears(2).build()));
        }

        @Test
        void bySalaryRangeOverlap() {
            // 6L-12L overlaps Backend (5-10L) and React (4-7L), not Senior (20-30L) or Intern (1-2L)
            assertEquals(List.of("Java Backend Developer", "React Developer"),
                    titles(JobSearchCriteria.builder().minSalary(600_000).maxSalary(1_200_000).build()));
        }

        @Test
        void byMinSalaryOnly() {
            assertEquals(List.of("Senior Java Engineer"),
                    titles(JobSearchCriteria.builder().minSalary(1_500_000).build()));
        }

        @Test
        void bySkillsRequiresAllOfThem() {
            assertEquals(List.of("Java Backend Developer"),
                    titles(JobSearchCriteria.builder().skills("java", "SQL").build()));
        }

        @Test
        void byJobType() {
            assertEquals(List.of("Java Intern"), titles(JobSearchCriteria.builder().jobType(JobType.INTERNSHIP).build()));
        }

        @Test
        void combinedCriteria() {
            assertEquals(List.of("Java Backend Developer"), titles(JobSearchCriteria.builder()
                    .title("java").location("pune").experienceYears(2).minSalary(500_000).maxSalary(1_000_000)
                    .skills("Spring Boot").build()));
        }

        @Test
        void sortsBySalaryDescending() {
            List<String> titles = service.search(JobSearchCriteria.builder().build(), PageRequest.of(0, 50, "salary,desc"))
                    .content().stream().map(Job::getTitle).toList();
            assertEquals(List.of("Senior Java Engineer", "Java Backend Developer", "React Developer", "Java Intern"), titles);
        }

        @Test
        void paginates() {
            Page<Job> first = service.search(JobSearchCriteria.builder().build(), PageRequest.of(0, 3, "title"));
            Page<Job> second = service.search(JobSearchCriteria.builder().build(), PageRequest.of(1, 3, "title"));

            assertEquals(4, first.totalElements());
            assertEquals(2, first.totalPages());
            assertEquals(3, first.content().size());
            assertTrue(first.hasNext());
            assertEquals(List.of("Senior Java Engineer"), second.content().stream().map(Job::getTitle).toList());
            assertFalse(second.hasNext());
        }

        @Test
        void pageBeyondTheEndIsEmpty() {
            Page<Job> page = service.search(JobSearchCriteria.builder().build(), PageRequest.of(5, 10));
            assertTrue(page.content().isEmpty());
            assertEquals(4, page.totalElements());
        }
    }
}

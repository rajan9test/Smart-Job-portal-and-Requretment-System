package com.jobportal;

import com.jobportal.model.Application;
import com.jobportal.model.Candidate;
import com.jobportal.model.Company;
import com.jobportal.model.Job;
import com.jobportal.model.JobType;
import com.jobportal.model.Recruiter;
import com.jobportal.service.dto.JobRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Sample accounts and jobs so the UI has something to show on first start.
 * Storage is in-memory, so this runs on every server start and everything resets on restart.
 */
public final class DemoData {

    private static final Logger log = LoggerFactory.getLogger(DemoData.class);

    /** Every demo account uses this password. */
    public static final String PASSWORD = "password123";

    private DemoData() {
    }

    public static void seed(PortalApplication app, Clock clock) {
        LocalDate today = LocalDate.now(clock);

        Company acme = app.companies.save(new Company("Acme Tech", "Pune", "https://acme.example"));
        Company globex = app.companies.save(new Company("Globex", "Bengaluru", "https://globex.example"));
        app.companies.save(new Company("Initech", "Hyderabad", "https://initech.example"));

        app.authService.createAdmin("Platform Admin", "admin@portal.dev", PASSWORD);
        Recruiter amit = app.authService.registerRecruiter("Amit Sharma", "amit@acme.dev", "9000000001", PASSWORD, acme.getId());
        Recruiter sara = app.authService.registerRecruiter("Sara Khan", "sara@globex.dev", "9000000002", PASSWORD, globex.getId());

        Candidate rajan = candidate(app, "Rajan Gupta", "rajan@mail.dev", 2, 800_000, "Pune",
                "B.E. Computer Engineering", "Java", "Spring Boot", "SQL", "React");
        Candidate priya = candidate(app, "Priya Nair", "priya@mail.dev", 4, 1_200_000, "Pune",
                "M.Tech Software Systems", "Java", "Spring Boot", "SQL", "Docker", "AWS");
        candidate(app, "Karan Mehta", "karan@mail.dev", 1, 500_000, "Bengaluru",
                "B.Sc Computer Science", "Python", "SQL", "React");

        Job backend = app.jobService.createJob(amit, new JobRequest("Java Backend Developer",
                "Build and maintain REST APIs with Spring Boot and PostgreSQL.",
                Set.of("Java", "Spring Boot", "SQL", "Docker", "AWS"), 1, 3, 500_000, 1_000_000,
                "Pune", JobType.FULL_TIME, 2, today.plusDays(30)));
        app.jobService.createJob(amit, new JobRequest("Senior Java Engineer",
                "Lead the design of our matching platform.",
                Set.of("Java", "AWS", "Kafka", "System Design"), 5, 8, 2_000_000, 3_000_000,
                "Pune", JobType.FULL_TIME, 1, today.plusDays(45)));
        app.jobService.createJob(amit, new JobRequest("Java Intern",
                "Six-month internship on the backend team.",
                Set.of("Java", "SQL"), 0, 1, 180_000, 300_000,
                "Pune", JobType.INTERNSHIP, 3, today.plusDays(15)));
        app.jobService.createJob(sara, new JobRequest("React Developer",
                "Own the candidate-facing web app.",
                Set.of("React", "TypeScript", "CSS"), 0, 2, 400_000, 800_000,
                "Bengaluru", JobType.FULL_TIME, 2, today.plusDays(20)));
        app.jobService.createJob(sara, new JobRequest("Data Engineer (Contract)",
                "Six-month contract building ETL pipelines.",
                Set.of("Python", "SQL", "Airflow"), 2, 5, 900_000, 1_500_000,
                "Bengaluru", JobType.CONTRACT, 1, today.plusDays(10)));

        // Seed two applications directly through the repository so recruiter screens aren't empty
        // before ApplicationService.apply (TODO #8) is implemented. Real applications go through apply().
        LocalDateTime now = LocalDateTime.now(clock);
        app.applications.save(new Application(backend.getId(), rajan.getId(), now.minusDays(2)));
        app.applications.save(new Application(backend.getId(), priya.getId(), now.minusDays(1)));

        log.info("Demo data loaded: {} users, {} companies, {} jobs (password for every account: {})",
                app.users.count(), app.companies.count(), app.jobs.count(), PASSWORD);
    }

    private static Candidate candidate(PortalApplication app, String name, String email, int years, long salary,
                                       String location, String education, String... skills) {
        Candidate c = app.authService.registerCandidate(name, email, null, PASSWORD);
        c.setSkills(Set.of(skills));
        c.setExperienceYears(years);
        c.setExpectedSalary(salary);
        c.setLocation(location);
        c.setEducation(education);
        return c;
    }
}

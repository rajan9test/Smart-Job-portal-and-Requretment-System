package com.jobportal;

import com.jobportal.exception.JobPortalException;
import com.jobportal.matching.ExperienceMatchingStrategy;
import com.jobportal.matching.MatchingStrategy;
import com.jobportal.matching.SalaryMatchingStrategy;
import com.jobportal.matching.SkillMatchingStrategy;
import com.jobportal.matching.WeightedMatchingStrategy;
import com.jobportal.model.Admin;
import com.jobportal.model.Application;
import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.Candidate;
import com.jobportal.model.Company;
import com.jobportal.model.InterviewType;
import com.jobportal.model.Job;
import com.jobportal.model.JobType;
import com.jobportal.model.Recruiter;
import com.jobportal.security.Session;
import com.jobportal.service.dto.JobRequest;
import com.jobportal.service.dto.JobSearchCriteria;
import com.jobportal.service.dto.PageRequest;
import com.jobportal.service.dto.ScheduleInterviewRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * End-to-end console walkthrough. Steps that hit an unimplemented TODO are reported and skipped,
 * so run this after each exercise to watch more of the flow come alive:
 *
 * <pre>./mvnw -q compile exec:java -Dexec.mainClass=com.jobportal.Main</pre>
 */
public class Main {

    private final PortalApplication app = new PortalApplication(Clock.systemDefaultZone());
    private int todosRemaining;

    private Recruiter amit;
    private Candidate rajan;
    private Admin admin;
    private Job javaJob;
    private final List<Candidate> candidates = new ArrayList<>();
    private final List<Application> applications = new ArrayList<>();

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        app.start();
        try (app) {
            step("Seed users and company", this::seed);
            step("Recruiter logs in", () -> {
                Session session = app.authService.login("amit@acme.dev", "recruiter-pass");
                System.out.println("  token issued, authenticated as " + app.authService.authenticate(session.token()));
            });
            step("Recruiter posts a job", this::postJob);
            step("Candidate searches for jobs", () -> {
                var criteria = JobSearchCriteria.builder().title("java").location("Pune").experienceYears(2).build();
                var page = app.jobService.search(criteria, PageRequest.of(0, 10, "salary,desc"));
                System.out.println("  found " + page.totalElements() + ": " + page.content());
            });
            step("Candidates apply", () -> {
                for (Candidate c : candidates) {
                    applications.add(app.applicationService.apply(c, javaJob.getId()));
                }
                System.out.println("  " + applications.size() + " applications submitted");
            });
            step("Candidate cannot apply twice", () -> {
                try {
                    app.applicationService.apply(rajan, javaJob.getId());
                    System.out.println("  BUG: duplicate application was accepted");
                } catch (JobPortalException expected) {
                    System.out.println("  rejected as expected: " + expected.getMessage());
                }
            });
            step("Rank applicants (weighted: skills 60%, experience 30%, salary 10%)", () -> {
                MatchingStrategy strategy = new WeightedMatchingStrategy()
                        .with(new SkillMatchingStrategy(), 0.6)
                        .with(new ExperienceMatchingStrategy(), 0.3)
                        .with(new SalaryMatchingStrategy(), 0.1);
                app.matchingService.rankApplicants(amit, javaJob.getId(), strategy)
                        .forEach(r -> System.out.println("  " + r));
            });
            step("Recruiter shortlists Rajan", () -> {
                Application a = applicationOf(rajan);
                app.applicationService.updateStatus(amit, a.getId(), ApplicationStatus.SHORTLISTED);
                System.out.println("  " + a);
            });
            step("Schedule a technical interview", () -> {
                LocalDateTime tomorrow11 = LocalDate.now().plusDays(1).atTime(11, 0);
                var interview = app.interviewService.schedule(amit, new ScheduleInterviewRequest(
                        applicationOf(rajan).getId(), "interviewer@acme.dev", tomorrow11, Duration.ofHours(1),
                        InterviewType.TECHNICAL));
                System.out.println("  " + interview);
            });
            step("Overlapping interview is rejected", () -> {
                LocalDateTime overlapping = LocalDate.now().plusDays(1).atTime(11, 30);
                try {
                    app.interviewService.schedule(amit, new ScheduleInterviewRequest(
                            applicationOf(rajan).getId(), "interviewer@acme.dev", overlapping, Duration.ofHours(1),
                            InterviewType.HR));
                    System.out.println("  BUG: overlapping interview was accepted");
                } catch (JobPortalException expected) {
                    System.out.println("  rejected as expected: " + expected.getMessage());
                }
            });
            step("Admin views platform statistics", () ->
                    System.out.println("  " + app.adminService.getStatistics(admin)));
        }
        // close() drained the notification queue, so the in-app inbox is complete now.
        if (rajan != null) {
            System.out.println("\nRajan's in-app inbox:");
            app.notifications.findByRecipientUserId(rajan.getId())
                    .forEach(n -> System.out.println("  - " + n.getMessage()));
        }
        System.out.printf("%nDemo finished. Steps blocked on TODOs: %d%n", todosRemaining);
    }

    private void seed() {
        Company acme = app.companies.save(new Company("Acme Tech", "Pune", "https://acme.example"));
        amit = app.authService.registerRecruiter("Amit", "amit@acme.dev", "9000000001", "recruiter-pass", acme.getId());
        admin = app.authService.createAdmin("Root Admin", "admin@portal.dev", "admin-pass");

        rajan = candidate("Rajan", "rajan@mail.dev", 2, 800_000, "Java", "Spring Boot", "SQL", "React");
        candidate("Priya", "priya@mail.dev", 4, 1_200_000, "Java", "Spring Boot", "SQL", "Docker", "AWS");
        candidate("Karan", "karan@mail.dev", 1, 500_000, "Python", "SQL");
        System.out.println("  company, recruiter, admin and " + candidates.size() + " candidates created");
    }

    private Candidate candidate(String name, String email, int years, long salary, String... skills) {
        Candidate c = app.authService.registerCandidate(name, email, null, "candidate-pass");
        c.setSkills(Set.of(skills));
        c.setExperienceYears(years);
        c.setExpectedSalary(salary);
        c.setLocation("Pune");
        candidates.add(c);
        return c;
    }

    private void postJob() {
        javaJob = app.jobService.createJob(amit, new JobRequest(
                "Java Backend Developer", "Build REST APIs with Spring Boot and PostgreSQL",
                Set.of("Java", "Spring Boot", "SQL", "Docker", "AWS"),
                1, 3, 500_000, 1_000_000, "Pune", JobType.FULL_TIME, 2, LocalDate.now().plusDays(30)));
        System.out.println("  " + javaJob);
    }

    private Application applicationOf(Candidate c) {
        return app.applications.findByJobIdAndCandidateId(javaJob.getId(), c.getId())
                .orElseThrow(() -> new IllegalStateException("no application for " + c.getName()
                        + " (is the 'Candidates apply' step still a TODO?)"));
    }

    private void step(String title, Runnable action) {
        System.out.println("\n== " + title);
        try {
            action.run();
        } catch (UnsupportedOperationException e) {
            todosRemaining++;
            System.out.println("  [TODO] " + e.getMessage());
        } catch (JobPortalException | IllegalStateException e) {
            System.out.println("  [ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}

package com.jobportal.api.dto;

import com.jobportal.matching.MatchResult;
import com.jobportal.model.Application;
import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.Candidate;
import com.jobportal.model.Company;
import com.jobportal.model.Interview;
import com.jobportal.model.InterviewFeedback;
import com.jobportal.model.InterviewStatus;
import com.jobportal.model.InterviewType;
import com.jobportal.model.Job;
import com.jobportal.model.JobStatus;
import com.jobportal.model.JobType;
import com.jobportal.model.Notification;
import com.jobportal.model.Recruiter;
import com.jobportal.model.Role;
import com.jobportal.model.User;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.Session;
import com.jobportal.service.dto.Page;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * JSON response shapes, and the mapping from domain objects to them.
 * Domain objects are never serialized directly: that would leak password hashes and couple the
 * API contract to internal field names.
 */
public final class Views {

    public record UserView(Long id, String name, String email, String phone, Role role, boolean blocked,
                           LocalDateTime createdAt,
                           // candidate-only
                           Set<String> skills, Integer experienceYears, String education, Long expectedSalary,
                           String location,
                           // recruiter-only
                           Long companyId, String companyName) {
    }

    public record LoginView(String token, LocalDateTime expiresAt, UserView user) {
    }

    public record CompanyView(Long id, String name, String location, String website) {
    }

    public record JobView(Long id, String title, String description, Long companyId, String companyName,
                          Long recruiterId, Set<String> requiredSkills, int minExperience, int maxExperience,
                          long minSalary, long maxSalary, String location, JobType jobType, int openings,
                          LocalDate deadline, JobStatus status, boolean acceptingApplications,
                          LocalDateTime createdAt) {
    }

    public record StatusChangeView(ApplicationStatus from, ApplicationStatus to, LocalDateTime at) {
    }

    public record ApplicationView(Long id, Long jobId, String jobTitle, String companyName, UserView candidate,
                                  ApplicationStatus status, LocalDateTime appliedAt, LocalDateTime updatedAt,
                                  List<StatusChangeView> history) {
    }

    public record FeedbackView(int rating, String comments, boolean recommended, LocalDateTime submittedAt) {
    }

    public record InterviewView(Long id, Long applicationId, Long candidateId, String candidateName, Long jobId,
                                String jobTitle, String interviewerEmail, LocalDateTime start, LocalDateTime end,
                                InterviewType type, InterviewStatus status, FeedbackView feedback) {
    }

    public record NotificationView(Long id, String message, LocalDateTime createdAt, boolean read) {
    }

    public record MatchView(UserView candidate, double score) {
    }

    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }

    // ---------- mapping ----------

    private final UserRepository users;
    private final JobRepository jobs;
    private final CompanyRepository companies;
    private final Clock clock;

    public Views(UserRepository users, JobRepository jobs, CompanyRepository companies, Clock clock) {
        this.users = users;
        this.jobs = jobs;
        this.companies = companies;
        this.clock = clock;
    }

    public UserView user(User u) {
        Set<String> skills = null;
        Integer experience = null;
        String education = null;
        Long salary = null;
        String location = null;
        Long companyId = null;
        String companyName = null;
        if (u instanceof Candidate c) {
            skills = new TreeSet<>(c.getSkills()); // sorted for stable display
            experience = c.getExperienceYears();
            education = c.getEducation();
            salary = c.getExpectedSalary();
            location = c.getLocation();
        } else if (u instanceof Recruiter r) {
            companyId = r.getCompanyId();
            companyName = companyName(r.getCompanyId());
        }
        return new UserView(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.isBlocked(),
                u.getCreatedAt(), skills, experience, education, salary, location, companyId, companyName);
    }

    public LoginView login(Session session, User u) {
        return new LoginView(session.token(), session.expiresAt(), user(u));
    }

    public CompanyView company(Company c) {
        return new CompanyView(c.getId(), c.getName(), c.getLocation(), c.getWebsite());
    }

    public JobView job(Job j) {
        return new JobView(j.getId(), j.getTitle(), j.getDescription(), j.getCompanyId(), companyName(j.getCompanyId()),
                j.getRecruiterId(), new TreeSet<>(j.getRequiredSkills()), j.getMinExperience(), j.getMaxExperience(),
                j.getMinSalary(), j.getMaxSalary(), j.getLocation(), j.getJobType(), j.getOpenings(), j.getDeadline(),
                j.getStatus(), j.isAcceptingApplications(LocalDate.now(clock)), j.getCreatedAt());
    }

    public ApplicationView application(Application a) {
        Job job = jobs.findById(a.getJobId()).orElse(null);
        UserView candidate = users.findById(a.getCandidateId()).map(this::user).orElse(null);
        List<StatusChangeView> history = a.getHistory().stream()
                .map(h -> new StatusChangeView(h.from(), h.to(), h.at()))
                .toList();
        return new ApplicationView(a.getId(), a.getJobId(), job == null ? "(deleted job)" : job.getTitle(),
                job == null ? null : companyName(job.getCompanyId()), candidate, a.getStatus(),
                a.getAppliedAt(), a.getUpdatedAt(), history);
    }

    public InterviewView interview(Interview i) {
        String candidateName = users.findById(i.getCandidateId()).map(User::getName).orElse("(deleted user)");
        String jobTitle = jobs.findById(i.getJobId()).map(Job::getTitle).orElse("(deleted job)");
        InterviewFeedback f = i.getFeedback();
        FeedbackView feedback = f == null ? null
                : new FeedbackView(f.rating(), f.comments(), f.recommended(), f.submittedAt());
        return new InterviewView(i.getId(), i.getApplicationId(), i.getCandidateId(), candidateName, i.getJobId(),
                jobTitle, i.getInterviewerEmail(), i.getSlot().start(), i.getSlot().end(), i.getType(),
                i.getStatus(), feedback);
    }

    public NotificationView notification(Notification n) {
        return new NotificationView(n.getId(), n.getMessage(), n.getCreatedAt(), n.isRead());
    }

    public MatchView match(MatchResult r) {
        return new MatchView(user(r.candidate()), Math.round(r.score() * 10) / 10.0);
    }

    public PageView<JobView> jobPage(Page<Job> page) {
        return new PageView<>(page.content().stream().map(this::job).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    private String companyName(Long companyId) {
        return companyId == null ? null : companies.findById(companyId).map(Company::getName).orElse(null);
    }
}

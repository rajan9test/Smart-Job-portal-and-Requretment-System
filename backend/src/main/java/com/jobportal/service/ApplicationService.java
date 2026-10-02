package com.jobportal.service;

import com.jobportal.event.EventPublisher;
import com.jobportal.event.PortalEvent;
import com.jobportal.exception.ApplicationNotFoundException;
import com.jobportal.exception.JobNotFoundException;
import com.jobportal.exception.UnauthorizedException;
import com.jobportal.model.Application;
import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.Candidate;
import com.jobportal.model.Job;
import com.jobportal.model.Recruiter;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applications;
    private final JobRepository jobs;
    private final AccessPolicy access;
    private final EventPublisher events;
    private final Clock clock;

    public ApplicationService(ApplicationRepository applications, JobRepository jobs, AccessPolicy access,
                              EventPublisher events, Clock clock) {
        this.applications = applications;
        this.jobs = jobs;
        this.access = access;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Candidate applies for a job.
     *
     * TODO(#8): implement. In order:
     *   1. Load the job or throw JobNotFoundException.
     *   2. If the job is not accepting applications (closed OR past deadline, see
     *      Job.isAcceptingApplications with LocalDate.now(clock)) throw JobClosedException.
     *   3. If this candidate already has an application for this job, throw AlreadyAppliedException.
     *      Design decision for you: may a candidate who WITHDREW apply again? The tests only cover an
     *      active application; pick a rule, document it here, and add a test for it.
     *   4. Save a new Application (appliedAt = LocalDateTime.now(clock)).
     *   5. Publish PortalEvent.ApplicationSubmitted so the recruiter (job.getRecruiterId()) is notified.
     *   6. log.info the event, then return the saved application.
     *
     *   Thinking question: two threads call apply() for the same candidate+job at the same moment.
     *   Both pass step 3 and both save. How would you prevent it here? How does a UNIQUE(job_id,
     *   candidate_id) constraint solve it in Phase 2?
     */
    public Application apply(Candidate candidate, Long jobId) {
        throw new UnsupportedOperationException("TODO(#8): ApplicationService.apply");
    }

    /** Candidate withdraws their own application. */
    public Application withdraw(Candidate candidate, Long applicationId) {
        Application application = getApplication(applicationId);
        if (!application.getCandidateId().equals(candidate.getId())) {
            throw new UnauthorizedException("Not your application");
        }
        return transition(application, ApplicationStatus.WITHDRAWN);
    }

    /** Recruiter moves an applicant through the pipeline (shortlist, reject, select...). */
    public Application updateStatus(Recruiter recruiter, Long applicationId, ApplicationStatus newStatus) {
        Application application = getApplication(applicationId);
        access.requireCanModifyJob(recruiter, loadJob(application.getJobId()));
        if (newStatus == ApplicationStatus.WITHDRAWN) {
            throw new UnauthorizedException("Only the candidate can withdraw an application");
        }
        return transition(application, newStatus);
    }

    /** Application history, newest first. */
    public List<Application> getApplicationsForCandidate(Candidate candidate) {
        return applications.findByCandidateId(candidate.getId()).stream()
                .sorted(Comparator.comparing(Application::getAppliedAt).reversed())
                .toList();
    }

    public List<Application> getApplicantsForJob(Recruiter recruiter, Long jobId) {
        access.requireCanModifyJob(recruiter, loadJob(jobId));
        return applications.findByJobId(jobId).stream()
                .sorted(Comparator.comparing(Application::getAppliedAt))
                .toList();
    }

    public Application getApplication(Long applicationId) {
        return applications.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    private Application transition(Application application, ApplicationStatus newStatus) {
        ApplicationStatus previous = application.changeStatus(newStatus, LocalDateTime.now(clock));
        applications.save(application);
        Job job = loadJob(application.getJobId());
        log.info("Application {} moved {} -> {}", application.getId(), previous, newStatus);
        events.publish(new PortalEvent.ApplicationStatusChanged(application.getId(), application.getCandidateId(),
                job.getRecruiterId(), job.getTitle(), previous, newStatus));
        return application;
    }

    private Job loadJob(Long jobId) {
        return jobs.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    }
}

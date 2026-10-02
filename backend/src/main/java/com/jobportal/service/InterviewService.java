package com.jobportal.service;

import com.jobportal.event.EventPublisher;
import com.jobportal.event.PortalEvent;
import com.jobportal.exception.ApplicationNotFoundException;
import com.jobportal.exception.InterviewNotFoundException;
import com.jobportal.exception.InvalidStatusTransitionException;
import com.jobportal.exception.JobNotFoundException;
import com.jobportal.model.Application;
import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.Candidate;
import com.jobportal.model.Interview;
import com.jobportal.model.InterviewFeedback;
import com.jobportal.model.InterviewStatus;
import com.jobportal.model.Job;
import com.jobportal.model.Recruiter;
import com.jobportal.model.TimeSlot;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.InterviewRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.service.dto.ScheduleInterviewRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewRepository interviews;
    private final ApplicationRepository applications;
    private final JobRepository jobs;
    private final AccessPolicy access;
    private final EventPublisher events;
    private final Clock clock;

    public InterviewService(InterviewRepository interviews, ApplicationRepository applications, JobRepository jobs,
                            AccessPolicy access, EventPublisher events, Clock clock) {
        this.interviews = interviews;
        this.applications = applications;
        this.jobs = jobs;
        this.access = access;
        this.events = events;
        this.clock = clock;
    }

    public Interview schedule(Recruiter recruiter, ScheduleInterviewRequest request) {
        Application application = applications.findById(request.applicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(request.applicationId()));
        Job job = loadJob(application.getJobId());
        access.requireCanModifyJob(recruiter, job);

        TimeSlot slot = TimeSlot.of(request.start(), request.duration());
        requireFuture(slot);
        ensureNoConflict(request.interviewerEmail(), application.getCandidateId(), slot, null);

        // First interview moves SHORTLISTED -> INTERVIEW; later rounds keep it at INTERVIEW.
        if (application.getStatus() == ApplicationStatus.SHORTLISTED) {
            application.changeStatus(ApplicationStatus.INTERVIEW, LocalDateTime.now(clock));
            applications.save(application);
        } else if (application.getStatus() != ApplicationStatus.INTERVIEW) {
            throw new InvalidStatusTransitionException(application.getStatus(), ApplicationStatus.INTERVIEW);
        }

        Interview interview = interviews.save(new Interview(application.getId(), application.getCandidateId(),
                job.getId(), request.interviewerEmail(), slot, request.type()));
        log.info("Scheduled {} interview {} for application {} at {}",
                request.type(), interview.getId(), application.getId(), slot.start());
        events.publish(new PortalEvent.InterviewScheduled(interview.getId(), interview.getCandidateId(), job.getTitle(), slot));
        return interview;
    }

    public Interview reschedule(Recruiter recruiter, Long interviewId, LocalDateTime newStart, Duration duration) {
        Interview interview = loadActive(recruiter, interviewId);
        TimeSlot slot = TimeSlot.of(newStart, duration);
        requireFuture(slot);
        ensureNoConflict(interview.getInterviewerEmail(), interview.getCandidateId(), slot, interview.getId());
        interview.setSlot(slot);
        interviews.save(interview);
        log.info("Interview {} rescheduled to {}", interviewId, slot.start());
        events.publish(new PortalEvent.InterviewRescheduled(interviewId, interview.getCandidateId(),
                loadJob(interview.getJobId()).getTitle(), slot));
        return interview;
    }

    public Interview cancel(Recruiter recruiter, Long interviewId, String reason) {
        Interview interview = loadActive(recruiter, interviewId);
        interview.setStatus(InterviewStatus.CANCELLED);
        interviews.save(interview);
        log.info("Interview {} cancelled: {}", interviewId, reason);
        events.publish(new PortalEvent.InterviewCancelled(interviewId, interview.getCandidateId(),
                loadJob(interview.getJobId()).getTitle(), reason));
        return interview;
    }

    public Interview complete(Recruiter recruiter, Long interviewId) {
        Interview interview = loadActive(recruiter, interviewId);
        interview.setStatus(InterviewStatus.COMPLETED);
        log.info("Interview {} completed", interviewId);
        return interviews.save(interview);
    }

    public Interview submitFeedback(Recruiter recruiter, Long interviewId, int rating, String comments, boolean recommended) {
        Interview interview = load(interviewId);
        access.requireCanModifyJob(recruiter, loadJob(interview.getJobId()));
        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new IllegalStateException("Feedback can only be given for a completed interview");
        }
        if (interview.getFeedback() != null) {
            throw new IllegalStateException("Feedback already submitted for interview " + interviewId);
        }
        interview.setFeedback(new InterviewFeedback(rating, comments, recommended, LocalDateTime.now(clock)));
        log.info("Feedback recorded for interview {} (rating {})", interviewId, rating);
        return interviews.save(interview);
    }

    /** A candidate's interviews, soonest first. */
    public List<Interview> getInterviewsForCandidate(Candidate candidate) {
        return interviews.findByCandidateId(candidate.getId()).stream()
                .sorted(Comparator.comparing((Interview i) -> i.getSlot().start()))
                .toList();
    }

    /** Every interview for jobs of the recruiter's company, soonest first. */
    public List<Interview> getInterviewsForRecruiter(Recruiter recruiter) {
        Set<Long> companyJobIds = jobs.findByCompanyId(recruiter.getCompanyId()).stream()
                .map(Job::getId)
                .collect(Collectors.toSet());
        return interviews.findAll().stream()
                .filter(i -> companyJobIds.contains(i.getJobId()))
                .sorted(Comparator.comparing((Interview i) -> i.getSlot().start()))
                .toList();
    }

    /**
     * Rejects a slot that clashes with another active interview.
     *
     * TODO(#9): implement.
     *   - Throw InterviewConflictException if the INTERVIEWER has another SCHEDULED interview whose slot
     *     overlaps {@code slot} (use TimeSlot.overlaps from TODO #1).
     *   - Apply the same check to the CANDIDATE: they can't be in two interviews at once either.
     *   - Ignore cancelled/completed interviews (Interview.isActive()).
     *   - Ignore the interview with id {@code excludeInterviewId}: when rescheduling, an interview must
     *     not conflict with its own old slot. It is null when scheduling a new one.
     *   - Log the conflict at ERROR before throwing, and put the clashing slot in the message.
     *   Repository methods you'll want: findByInterviewerEmail, findByCandidateId.
     *
     *   Stretch: this is O(n) per call. If an interviewer had 10,000 interviews, how would a
     *   TreeMap<LocalDateTime, Interview> keyed by start time let you check only the neighbours
     *   (floorEntry / ceilingEntry)?
     */
    void ensureNoConflict(String interviewerEmail, Long candidateId, TimeSlot slot, Long excludeInterviewId) {
        throw new UnsupportedOperationException("TODO(#9): InterviewService.ensureNoConflict");
    }

    private void requireFuture(TimeSlot slot) {
        if (!slot.start().isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Interview must be scheduled in the future");
        }
    }

    private Interview load(Long interviewId) {
        return interviews.findById(interviewId).orElseThrow(() -> new InterviewNotFoundException(interviewId));
    }

    private Interview loadActive(Recruiter recruiter, Long interviewId) {
        Interview interview = load(interviewId);
        access.requireCanModifyJob(recruiter, loadJob(interview.getJobId()));
        if (!interview.isActive()) {
            throw new IllegalStateException("Interview " + interviewId + " is " + interview.getStatus());
        }
        return interview;
    }

    private Job loadJob(Long jobId) {
        return jobs.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    }
}

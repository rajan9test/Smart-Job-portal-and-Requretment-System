package com.jobportal.model;

import java.util.Objects;

public class Interview implements Identifiable {

    private Long id;
    private final Long applicationId;
    private final Long candidateId;
    private final Long jobId;
    /** Interviewers are not necessarily platform users, so they are identified by email. */
    private final String interviewerEmail;
    private TimeSlot slot;
    private final InterviewType type;
    private InterviewStatus status = InterviewStatus.SCHEDULED;
    private InterviewFeedback feedback;

    public Interview(Long applicationId, Long candidateId, Long jobId,
                     String interviewerEmail, TimeSlot slot, InterviewType type) {
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.candidateId = Objects.requireNonNull(candidateId, "candidateId");
        this.jobId = Objects.requireNonNull(jobId, "jobId");
        this.interviewerEmail = Objects.requireNonNull(interviewerEmail, "interviewerEmail").trim().toLowerCase();
        this.slot = Objects.requireNonNull(slot, "slot");
        this.type = Objects.requireNonNull(type, "type");
    }

    public boolean isActive() {
        return status == InterviewStatus.SCHEDULED;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getInterviewerEmail() {
        return interviewerEmail;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public void setSlot(TimeSlot slot) {
        this.slot = Objects.requireNonNull(slot, "slot");
    }

    public InterviewType getType() {
        return type;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public InterviewFeedback getFeedback() {
        return feedback;
    }

    public void setFeedback(InterviewFeedback feedback) {
        this.feedback = feedback;
    }

    @Override
    public String toString() {
        return "Interview{id=%d, candidateId=%d, interviewer=%s, slot=%s, type=%s, status=%s}"
                .formatted(id, candidateId, interviewerEmail, slot, type, status);
    }
}

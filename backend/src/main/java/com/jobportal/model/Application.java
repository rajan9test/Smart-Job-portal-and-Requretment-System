package com.jobportal.model;

import com.jobportal.exception.InvalidStatusTransitionException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Application implements Identifiable {

    /** One entry in the audit trail of an application. */
    public record StatusChange(ApplicationStatus from, ApplicationStatus to, LocalDateTime at) {
    }

    private Long id;
    private final Long jobId;
    private final Long candidateId;
    private ApplicationStatus status = ApplicationStatus.APPLIED;
    private final LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private final List<StatusChange> history = new ArrayList<>();

    public Application(Long jobId, Long candidateId, LocalDateTime appliedAt) {
        this.jobId = Objects.requireNonNull(jobId, "jobId");
        this.candidateId = Objects.requireNonNull(candidateId, "candidateId");
        this.appliedAt = Objects.requireNonNull(appliedAt, "appliedAt");
        this.updatedAt = appliedAt;
    }

    /**
     * Moves the application to a new status, enforcing the state machine in {@link ApplicationStatus}.
     *
     * @return the previous status
     */
    public ApplicationStatus changeStatus(ApplicationStatus next, LocalDateTime at) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(status, next);
        }
        ApplicationStatus previous = status;
        status = next;
        updatedAt = at;
        history.add(new StatusChange(previous, next, at));
        return previous;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<StatusChange> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @Override
    public String toString() {
        return "Application{id=%d, jobId=%d, candidateId=%d, status=%s}".formatted(id, jobId, candidateId, status);
    }
}

package com.jobportal.service;

import com.jobportal.matching.MatchResult;
import com.jobportal.matching.MatchingStrategy;
import com.jobportal.model.Application;
import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.Candidate;
import com.jobportal.model.Job;
import com.jobportal.model.Recruiter;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.UserRepository;

import java.util.Collection;
import java.util.List;

public class MatchingService {

    private final JobService jobService;
    private final ApplicationRepository applications;
    private final UserRepository users;
    private final AccessPolicy access;

    public MatchingService(JobService jobService, ApplicationRepository applications, UserRepository users,
                           AccessPolicy access) {
        this.jobService = jobService;
        this.applications = applications;
        this.users = users;
        this.access = access;
    }

    /**
     * Scores every candidate and returns them best-first.
     *
     * TODO(#5): implement with streams.
     *   - map each candidate to new MatchResult(candidate, strategy.score(candidate, job))
     *   - sort by score descending; break ties by candidate name ascending so output is deterministic
     *     (Comparator.comparingDouble(...).reversed().thenComparing(...))
     *   - return an unmodifiable list (Stream.toList() already is)
     */
    public List<MatchResult> rankCandidates(Job job, Collection<Candidate> candidates, MatchingStrategy strategy) {
        throw new UnsupportedOperationException("TODO(#5): MatchingService.rankCandidates");
    }

    /**
     * Returns the best {@code n} candidates, best-first, WITHOUT sorting the whole collection.
     *
     * TODO(#6): implement with a PriorityQueue.
     *   Sorting everything is O(m log m). Keeping a MIN-heap of size n is O(m log n):
     *     for each candidate: offer its MatchResult; if heap.size() > n, poll() (drops the current worst).
     *   At the end the heap holds the top n, but in heap order, so drain it and reverse to get best-first.
     *   Use the same tie-break as rankCandidates so the two methods agree.
     *   Edge cases: n <= 0 -> empty list; n > candidates.size() -> all of them.
     */
    public List<MatchResult> topCandidates(Job job, Collection<Candidate> candidates, int n, MatchingStrategy strategy) {
        throw new UnsupportedOperationException("TODO(#6): MatchingService.topCandidates");
    }

    /** Ranks the active (non-withdrawn, non-rejected) applicants of a job for its recruiter. */
    public List<MatchResult> rankApplicants(Recruiter recruiter, Long jobId, MatchingStrategy strategy) {
        Job job = jobService.getJob(jobId);
        access.requireCanModifyJob(recruiter, job);
        List<Candidate> candidates = applications.findByJobId(jobId).stream()
                .filter(a -> a.getStatus() != ApplicationStatus.WITHDRAWN && a.getStatus() != ApplicationStatus.REJECTED)
                .map(Application::getCandidateId)
                .flatMap(id -> users.findById(id).stream())
                .filter(Candidate.class::isInstance)
                .map(Candidate.class::cast)
                .toList();
        return rankCandidates(job, candidates, strategy);
    }
}

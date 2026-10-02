package com.jobportal.matching;

import com.jobportal.model.Candidate;
import com.jobportal.model.Job;

/**
 * Strategy pattern: each implementation scores how well a candidate fits a job in a different way.
 * The ranking code in MatchingService doesn't care which strategy it is given.
 */
@FunctionalInterface
public interface MatchingStrategy {

    /** @return a score from 0.0 (no fit) to 100.0 (perfect fit) */
    double score(Candidate candidate, Job job);

    default String name() {
        return getClass().getSimpleName();
    }
}

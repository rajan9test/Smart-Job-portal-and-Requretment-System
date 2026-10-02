package com.jobportal.matching;

import com.jobportal.model.Candidate;
import com.jobportal.model.Job;

/**
 * Percentage of the job's required skills that the candidate has.
 *
 * <pre>
 * Required: java, spring boot, sql, docker, aws
 * Candidate: java, spring boot, sql, react
 * Matching = 3, Required = 5  ->  60.0
 * </pre>
 */
public class SkillMatchingStrategy implements MatchingStrategy {

    /**
     * TODO(#3): implement.
     *   - Both skill sets are already normalized to lowercase HashSets (see util.Skills).
     *   - If the job requires no skills, every candidate is a perfect match: return 100.0.
     *   - Extra candidate skills (react above) neither help nor hurt.
     *   - Do NOT mutate the sets returned by the getters (they are unmodifiable anyway).
     *   Try two versions and compare: a stream with filter(...).count(), and new HashSet + retainAll.
     *   Which is O(n)? Which allocates?
     */
    @Override
    public double score(Candidate candidate, Job job) {
        throw new UnsupportedOperationException("TODO(#3): SkillMatchingStrategy.score");
    }
}

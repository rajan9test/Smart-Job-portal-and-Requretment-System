package com.jobportal.matching;

import com.jobportal.model.Candidate;
import com.jobportal.model.Job;

/**
 * Worked example of a strategy. Scores the candidate's years of experience against the job's range:
 * <ul>
 *   <li>inside [min, max]: 100</li>
 *   <li>under-qualified: lose 25 points per missing year, never below 0</li>
 *   <li>over-qualified: lose 10 points per extra year, never below 50 (still employable, maybe a flight risk)</li>
 * </ul>
 */
public class ExperienceMatchingStrategy implements MatchingStrategy {

    static final double PENALTY_PER_MISSING_YEAR = 25.0;
    static final double PENALTY_PER_EXTRA_YEAR = 10.0;
    static final double OVERQUALIFIED_FLOOR = 50.0;

    @Override
    public double score(Candidate candidate, Job job) {
        int years = candidate.getExperienceYears();
        if (years < job.getMinExperience()) {
            int missing = job.getMinExperience() - years;
            return Math.max(0.0, 100.0 - missing * PENALTY_PER_MISSING_YEAR);
        }
        if (years > job.getMaxExperience()) {
            int extra = years - job.getMaxExperience();
            return Math.max(OVERQUALIFIED_FLOOR, 100.0 - extra * PENALTY_PER_EXTRA_YEAR);
        }
        return 100.0;
    }
}

package com.jobportal.matching;

import com.jobportal.model.Candidate;
import com.jobportal.model.Job;

/**
 * How affordable the candidate is for this job.
 */
public class SalaryMatchingStrategy implements MatchingStrategy {

    /**
     * TODO(#4): implement.
     *   - expected salary <= job max salary            -> 100.0
     *     (asking below the range is fine for the employer)
     *   - expected above max: lose 1 point for every 1% above max, never below 0.
     *       max = 10,00,000, expected = 12,00,000  -> 20% above -> 80.0
     *       max = 10,00,000, expected = 25,00,000  -> 150% above -> 0.0
     *   - a job with maxSalary == 0 (not disclosed) -> 100.0
     *   Watch out for integer division: both salaries are longs.
     */
    @Override
    public double score(Candidate candidate, Job job) {
        throw new UnsupportedOperationException("TODO(#4): SalaryMatchingStrategy.score");
    }
}

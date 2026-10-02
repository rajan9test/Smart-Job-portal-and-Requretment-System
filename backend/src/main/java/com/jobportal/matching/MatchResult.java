package com.jobportal.matching;

import com.jobportal.model.Candidate;

public record MatchResult(Candidate candidate, double score) {

    @Override
    public String toString() {
        return "%s -> %.1f%%".formatted(candidate.getName(), score);
    }
}

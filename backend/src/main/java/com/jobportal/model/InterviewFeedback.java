package com.jobportal.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Feedback left by the interviewer once an interview is completed.
 *
 * @param rating      1 (poor) to 5 (excellent)
 * @param recommended whether the interviewer recommends moving forward
 */
public record InterviewFeedback(int rating, String comments, boolean recommended, LocalDateTime submittedAt) {
    public InterviewFeedback {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5, got " + rating);
        }
        Objects.requireNonNull(submittedAt, "submittedAt");
    }
}

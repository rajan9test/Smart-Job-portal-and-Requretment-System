package com.jobportal.model;

/**
 * Lifecycle of a job application.
 *
 * <pre>
 * APPLIED -> SHORTLISTED -> INTERVIEW -> SELECTED
 *    \            \            \
 *     +-----------+------------+--> REJECTED
 *
 * The candidate can WITHDRAW while the application is still in progress.
 * </pre>
 */
public enum ApplicationStatus {
    APPLIED,
    SHORTLISTED,
    INTERVIEW,
    SELECTED,
    REJECTED,
    WITHDRAWN;

    /** Terminal states cannot move anywhere else. */
    public boolean isTerminal() {
        return this == SELECTED || this == REJECTED || this == WITHDRAWN;
    }

    /**
     * Returns true if an application in this status may move to {@code next}.
     *
     * TODO(#2): implement the state machine shown in the class comment.
     *   Rules to encode (see ApplicationStatusTest):
     *   - terminal states (SELECTED, REJECTED, WITHDRAWN) cannot transition anywhere
     *   - staying in the same status is not a transition (return false)
     *   - APPLIED     -> SHORTLISTED, REJECTED, WITHDRAWN
     *   - SHORTLISTED -> INTERVIEW, REJECTED, WITHDRAWN
     *   - INTERVIEW   -> SELECTED, REJECTED, WITHDRAWN
     *   Hint: a switch expression on {@code this} keeps it readable. An EnumMap/EnumSet
     *   table is another approach worth trying.
     */
    public boolean canTransitionTo(ApplicationStatus next) {
        throw new UnsupportedOperationException("TODO(#2): ApplicationStatus.canTransitionTo");
    }
}

package com.jobportal.model;

import com.jobportal.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static com.jobportal.model.ApplicationStatus.APPLIED;
import static com.jobportal.model.ApplicationStatus.INTERVIEW;
import static com.jobportal.model.ApplicationStatus.REJECTED;
import static com.jobportal.model.ApplicationStatus.SELECTED;
import static com.jobportal.model.ApplicationStatus.SHORTLISTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TODO(#2) */
class ApplicationStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed")
    @CsvSource({
            "APPLIED, SHORTLISTED", "APPLIED, REJECTED", "APPLIED, WITHDRAWN",
            "SHORTLISTED, INTERVIEW", "SHORTLISTED, REJECTED", "SHORTLISTED, WITHDRAWN",
            "INTERVIEW, SELECTED", "INTERVIEW, REJECTED", "INTERVIEW, WITHDRAWN",
    })
    void allowedTransitions(ApplicationStatus from, ApplicationStatus to) {
        assertTrue(from.canTransitionTo(to));
    }

    @ParameterizedTest(name = "{0} -> {1} forbidden")
    @CsvSource({
            "APPLIED, INTERVIEW", "APPLIED, SELECTED",          // can't skip steps
            "SHORTLISTED, APPLIED", "INTERVIEW, SHORTLISTED",   // can't go backwards
            "SHORTLISTED, SELECTED",
            "REJECTED, SELECTED", "REJECTED, SHORTLISTED",      // rejected is final
            "SELECTED, REJECTED", "WITHDRAWN, APPLIED",
    })
    void forbiddenTransitions(ApplicationStatus from, ApplicationStatus to) {
        assertFalse(from.canTransitionTo(to));
    }

    @ParameterizedTest
    @EnumSource(ApplicationStatus.class)
    void noStatusTransitionsToItself(ApplicationStatus status) {
        assertFalse(status.canTransitionTo(status));
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"SELECTED", "REJECTED", "WITHDRAWN"})
    void terminalStatesGoNowhere(ApplicationStatus terminal) {
        for (ApplicationStatus next : ApplicationStatus.values()) {
            assertFalse(terminal.canTransitionTo(next), terminal + " -> " + next);
        }
    }

    @Test
    @DisplayName("Rejected candidate cannot be selected")
    void rejectedCandidateCannotBeSelected() {
        Application application = new Application(1L, 2L, LocalDateTime.now());
        application.changeStatus(REJECTED, LocalDateTime.now());

        assertThrows(InvalidStatusTransitionException.class,
                () -> application.changeStatus(SELECTED, LocalDateTime.now()));
        assertEquals(REJECTED, application.getStatus());
    }

    @Test
    void happyPathRecordsHistory() {
        Application application = new Application(1L, 2L, LocalDateTime.now());
        application.changeStatus(SHORTLISTED, LocalDateTime.now());
        application.changeStatus(INTERVIEW, LocalDateTime.now());
        application.changeStatus(SELECTED, LocalDateTime.now());

        assertEquals(SELECTED, application.getStatus());
        assertEquals(3, application.getHistory().size());
        assertEquals(APPLIED, application.getHistory().getFirst().from());
    }
}

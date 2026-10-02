package com.jobportal.service;

import com.jobportal.TestData;
import com.jobportal.event.EventPublisher;
import com.jobportal.exception.InterviewConflictException;
import com.jobportal.model.Application;
import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.Interview;
import com.jobportal.model.InterviewStatus;
import com.jobportal.model.InterviewType;
import com.jobportal.model.Job;
import com.jobportal.model.Recruiter;
import com.jobportal.repository.inmemory.InMemoryApplicationRepository;
import com.jobportal.repository.inmemory.InMemoryInterviewRepository;
import com.jobportal.repository.inmemory.InMemoryJobRepository;
import com.jobportal.service.dto.ScheduleInterviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.jobportal.TestData.NOW;
import static com.jobportal.TestData.job;
import static com.jobportal.TestData.recruiter;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * TODO(#9) conflict detection. Also depends on TODO #1 (TimeSlot.overlaps) and #2 (status transitions),
 * because scheduling moves the application SHORTLISTED -> INTERVIEW.
 */
class InterviewServiceTest {

    private static final LocalDateTime TOMORROW_10AM = NOW.toLocalDate().plusDays(1).atTime(10, 0);
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final String AMIT = "amit@acme.dev";
    private static final String NEHA = "neha@acme.dev";

    private InMemoryApplicationRepository applications;
    private InterviewService service;
    private Recruiter recruiter;
    private Job job;

    @BeforeEach
    void setUp() {
        InMemoryJobRepository jobs = new InMemoryJobRepository();
        applications = new InMemoryApplicationRepository();
        AccessPolicy allowAll = mock(AccessPolicy.class); // a mock's void methods do nothing: every check passes
        service = new InterviewService(new InMemoryInterviewRepository(), applications, jobs, allowAll,
                new EventPublisher(), TestData.CLOCK);
        recruiter = recruiter(1, 1L);
        job = jobs.save(job(1, 1, 1, "Java Developer"));
    }

    private Application shortlisted(long candidateId) {
        Application application = new Application(job.getId(), candidateId, NOW);
        application.changeStatus(ApplicationStatus.SHORTLISTED, NOW);
        return applications.save(application);
    }

    private Interview schedule(Application application, String interviewer, LocalDateTime start) {
        return service.schedule(recruiter, new ScheduleInterviewRequest(
                application.getId(), interviewer, start, ONE_HOUR, InterviewType.TECHNICAL));
    }

    @Test
    void schedulingMovesApplicationToInterview() {
        Application application = shortlisted(10);
        Interview interview = schedule(application, AMIT, TOMORROW_10AM);

        assertEquals(InterviewStatus.SCHEDULED, interview.getStatus());
        assertEquals(ApplicationStatus.INTERVIEW, application.getStatus());
    }

    @Test
    @DisplayName("Interview cannot overlap for the same interviewer")
    void interviewerDoubleBookingIsRejected() {
        schedule(shortlisted(10), AMIT, TOMORROW_10AM);                       // 10:00-11:00

        Application other = shortlisted(11);
        assertThrows(InterviewConflictException.class,
                () -> schedule(other, AMIT, TOMORROW_10AM.plusMinutes(30)));   // 10:30-11:30
    }

    @Test
    void interviewerEmailIsCaseInsensitive() {
        schedule(shortlisted(10), AMIT, TOMORROW_10AM);
        Application other = shortlisted(11);
        assertThrows(InterviewConflictException.class, () -> schedule(other, "AMIT@acme.dev", TOMORROW_10AM));
    }

    @Test
    void candidateDoubleBookingIsRejected() {
        Application application = shortlisted(10);
        schedule(application, AMIT, TOMORROW_10AM);

        // same candidate, different interviewer, overlapping time
        assertThrows(InterviewConflictException.class,
                () -> schedule(application, NEHA, TOMORROW_10AM.plusMinutes(15)));
    }

    @Test
    void backToBackInterviewsAreAllowed() {
        schedule(shortlisted(10), AMIT, TOMORROW_10AM);                       // 10:00-11:00
        assertDoesNotThrow(() -> schedule(shortlisted(11), AMIT, TOMORROW_10AM.plusHours(1))); // 11:00-12:00
    }

    @Test
    void differentInterviewersAtSameTimeAreAllowed() {
        schedule(shortlisted(10), AMIT, TOMORROW_10AM);
        assertDoesNotThrow(() -> schedule(shortlisted(11), NEHA, TOMORROW_10AM));
    }

    @Test
    void cancelledInterviewsFreeTheSlot() {
        Interview first = schedule(shortlisted(10), AMIT, TOMORROW_10AM);
        service.cancel(recruiter, first.getId(), "candidate unavailable");

        assertDoesNotThrow(() -> schedule(shortlisted(11), AMIT, TOMORROW_10AM));
    }

    @Test
    void reschedulingDoesNotConflictWithItself() {
        Interview interview = schedule(shortlisted(10), AMIT, TOMORROW_10AM);

        // 10:00-11:00 -> 10:30-11:30 overlaps only its own old slot
        Interview moved = service.reschedule(recruiter, interview.getId(), TOMORROW_10AM.plusMinutes(30), ONE_HOUR);
        assertEquals(TOMORROW_10AM.plusMinutes(30), moved.getSlot().start());
    }

    @Test
    void reschedulingIntoAnotherInterviewIsRejected() {
        schedule(shortlisted(10), AMIT, TOMORROW_10AM);                       // 10:00-11:00
        Interview second = schedule(shortlisted(11), AMIT, TOMORROW_10AM.plusHours(2)); // 12:00-13:00

        assertThrows(InterviewConflictException.class,
                () -> service.reschedule(recruiter, second.getId(), TOMORROW_10AM.plusMinutes(45), ONE_HOUR));
    }

    @Test
    void cannotScheduleInThePast() {
        Application application = shortlisted(10);
        assertThrows(IllegalArgumentException.class, () -> schedule(application, AMIT, NOW.minusHours(1)));
    }

    @Test
    void feedbackRequiresCompletedInterview() {
        Interview interview = schedule(shortlisted(10), AMIT, TOMORROW_10AM);
        assertThrows(IllegalStateException.class,
                () -> service.submitFeedback(recruiter, interview.getId(), 4, "good", true));

        service.complete(recruiter, interview.getId());
        assertEquals(4, service.submitFeedback(recruiter, interview.getId(), 4, "good", true).getFeedback().rating());
    }
}

package com.jobportal.service;

import com.jobportal.TestData;
import com.jobportal.event.EventPublisher;
import com.jobportal.event.PortalEvent;
import com.jobportal.exception.AlreadyAppliedException;
import com.jobportal.exception.JobClosedException;
import com.jobportal.exception.JobNotFoundException;
import com.jobportal.exception.UnauthorizedException;
import com.jobportal.model.Application;
import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.Candidate;
import com.jobportal.model.Job;
import com.jobportal.model.Recruiter;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.jobportal.TestData.NOW;
import static com.jobportal.TestData.TODAY;
import static com.jobportal.TestData.candidate;
import static com.jobportal.TestData.job;
import static com.jobportal.TestData.recruiter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mockito example: the repositories and event publisher are mocks, so each test controls exactly
 * what the "database" returns and verifies what the service did with it.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applications;
    @Mock
    private JobRepository jobs;
    @Mock
    private AccessPolicy access;
    @Mock
    private EventPublisher events;

    private ApplicationService service;
    private Candidate rajan;
    private Job job;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applications, jobs, access, events, TestData.CLOCK);
        rajan = candidate(10, "Rajan", "Java");
        job = job(100, 1, 50, "Java Developer", "Java");
    }

    // ---------- TODO(#8) apply ----------

    @Test
    void applySavesApplicationAndNotifiesRecruiter() {
        when(jobs.findById(100L)).thenReturn(Optional.of(job));
        when(applications.findByJobIdAndCandidateId(100L, 10L)).thenReturn(Optional.empty());
        when(applications.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        Application result = service.apply(rajan, 100L);

        assertEquals(ApplicationStatus.APPLIED, result.getStatus());
        assertEquals(NOW, result.getAppliedAt());
        ArgumentCaptor<PortalEvent> event = ArgumentCaptor.forClass(PortalEvent.class);
        verify(events).publish(event.capture());
        var submitted = assertInstanceOf(PortalEvent.ApplicationSubmitted.class, event.getValue());
        assertEquals(50L, submitted.recruiterId());
        assertEquals(10L, submitted.candidateId());
    }

    @Test
    void applyToMissingJobFails() {
        when(jobs.findById(999L)).thenReturn(Optional.empty());
        assertThrows(JobNotFoundException.class, () -> service.apply(rajan, 999L));
    }

    @Test
    @DisplayName("Closed job cannot accept applications")
    void closedJobRejectsApplications() {
        job.close();
        when(jobs.findById(100L)).thenReturn(Optional.of(job));

        assertThrows(JobClosedException.class, () -> service.apply(rajan, 100L));
        verify(applications, never()).save(any());
        verify(events, never()).publish(any());
    }

    @Test
    void jobPastDeadlineRejectsApplications() {
        job.setDeadline(TODAY.minusDays(1));
        when(jobs.findById(100L)).thenReturn(Optional.of(job));

        assertThrows(JobClosedException.class, () -> service.apply(rajan, 100L));
    }

    @Test
    void deadlineDayItselfIsStillOpen() {
        job.setDeadline(TODAY);
        when(jobs.findById(100L)).thenReturn(Optional.of(job));
        when(applications.findByJobIdAndCandidateId(100L, 10L)).thenReturn(Optional.empty());
        when(applications.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(ApplicationStatus.APPLIED, service.apply(rajan, 100L).getStatus());
    }

    @Test
    @DisplayName("Candidate cannot apply twice")
    void candidateCannotApplyTwice() {
        when(jobs.findById(100L)).thenReturn(Optional.of(job));
        when(applications.findByJobIdAndCandidateId(100L, 10L))
                .thenReturn(Optional.of(new Application(100L, 10L, NOW.minusDays(1))));

        assertThrows(AlreadyAppliedException.class, () -> service.apply(rajan, 100L));
        verify(applications, never()).save(any());
    }

    // ---------- implemented behaviour ----------

    @Test
    void candidateCannotWithdrawSomeoneElsesApplication() {
        Application othersApplication = new Application(100L, 99L, NOW);
        othersApplication.setId(5L);
        when(applications.findById(5L)).thenReturn(Optional.of(othersApplication));

        assertThrows(UnauthorizedException.class, () -> service.withdraw(rajan, 5L));
    }

    @Test
    void recruiterCannotWithdrawOnCandidatesBehalf() {
        Recruiter recruiter = recruiter(50, 1L);
        Application application = new Application(100L, 10L, NOW);
        application.setId(5L);
        when(applications.findById(5L)).thenReturn(Optional.of(application));
        when(jobs.findById(100L)).thenReturn(Optional.of(job));

        assertThrows(UnauthorizedException.class,
                () -> service.updateStatus(recruiter, 5L, ApplicationStatus.WITHDRAWN));
    }
}

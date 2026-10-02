package com.jobportal;

import com.jobportal.config.ConfigurationManager;
import com.jobportal.event.EventPublisher;
import com.jobportal.model.NotificationChannel;
import com.jobportal.notification.NotificationDispatcher;
import com.jobportal.notification.NotificationEventListener;
import com.jobportal.notification.NotificationFactory;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.repository.InterviewRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.NotificationRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.repository.inmemory.InMemoryApplicationRepository;
import com.jobportal.repository.inmemory.InMemoryCompanyRepository;
import com.jobportal.repository.inmemory.InMemoryInterviewRepository;
import com.jobportal.repository.inmemory.InMemoryJobRepository;
import com.jobportal.repository.inmemory.InMemoryNotificationRepository;
import com.jobportal.repository.inmemory.InMemoryUserRepository;
import com.jobportal.security.PasswordHasher;
import com.jobportal.service.AccessPolicy;
import com.jobportal.service.AdminService;
import com.jobportal.service.ApplicationService;
import com.jobportal.service.AuthService;
import com.jobportal.service.InboxService;
import com.jobportal.service.InterviewService;
import com.jobportal.service.JobService;
import com.jobportal.service.MatchingService;
import com.jobportal.service.ProfileService;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Composition root: builds every object and wires dependencies by hand (constructor injection).
 * This is exactly what Spring's ApplicationContext automates in Phase 3, and writing it once
 * yourself makes @Autowired much less magical.
 */
public class PortalApplication implements AutoCloseable {

    public final UserRepository users = new InMemoryUserRepository();
    public final CompanyRepository companies = new InMemoryCompanyRepository();
    public final JobRepository jobs = new InMemoryJobRepository();
    public final ApplicationRepository applications = new InMemoryApplicationRepository();
    public final InterviewRepository interviews = new InMemoryInterviewRepository();
    public final NotificationRepository notifications = new InMemoryNotificationRepository();

    public final EventPublisher events = new EventPublisher();
    public final NotificationDispatcher dispatcher;

    public final AuthService authService;
    public final JobService jobService;
    public final ApplicationService applicationService;
    public final InterviewService interviewService;
    public final MatchingService matchingService;
    public final AdminService adminService;
    public final ProfileService profileService;
    public final InboxService inboxService;

    public PortalApplication(Clock clock) {
        ConfigurationManager config = ConfigurationManager.getInstance();
        AccessPolicy access = new AccessPolicy();

        dispatcher = new NotificationDispatcher(new NotificationFactory(notifications),
                config.getInt("notification.worker.threads", 2));
        List<NotificationChannel> channels = Arrays.stream(config.getString("notification.channels", "IN_APP").split(","))
                .map(String::trim)
                .map(NotificationChannel::valueOf)
                .toList();
        events.subscribe(new NotificationEventListener(dispatcher, channels, clock));

        authService = new AuthService(users, companies,
                new PasswordHasher(config.getInt("auth.pbkdf2.iterations", 210_000)), clock,
                Duration.ofMinutes(config.getLong("auth.session.ttl.minutes", 60)),
                config.getInt("auth.password.min.length", 8));
        jobService = new JobService(jobs, companies, access);
        applicationService = new ApplicationService(applications, jobs, access, events, clock);
        interviewService = new InterviewService(interviews, applications, jobs, access, events, clock);
        matchingService = new MatchingService(jobService, applications, users, access);
        adminService = new AdminService(users, jobs, applications);
        profileService = new ProfileService(users);
        inboxService = new InboxService(notifications);
    }

    public void start() {
        dispatcher.start();
    }

    @Override
    public void close() {
        dispatcher.close();
    }
}

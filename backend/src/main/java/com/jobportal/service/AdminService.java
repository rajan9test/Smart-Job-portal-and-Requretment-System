package com.jobportal.service;

import com.jobportal.exception.UnauthorizedException;
import com.jobportal.exception.UserNotFoundException;
import com.jobportal.model.Admin;
import com.jobportal.model.Application;
import com.jobportal.model.Job;
import com.jobportal.model.Role;
import com.jobportal.model.User;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.service.dto.PlatformStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository users;
    private final JobRepository jobs;
    private final ApplicationRepository applications;

    public AdminService(UserRepository users, JobRepository jobs, ApplicationRepository applications) {
        this.users = users;
        this.jobs = jobs;
        this.applications = applications;
    }

    public List<User> listUsers(Admin admin) {
        return users.findAll().stream().sorted(Comparator.comparing(User::getId)).toList();
    }

    public List<User> listUsersByRole(Admin admin, Role role) {
        return users.findByRole(role);
    }

    public void blockUser(Admin admin, Long userId) {
        User user = loadOtherUser(admin, userId);
        user.setBlocked(true);
        users.save(user);
        log.warn("Admin {} blocked user {}", admin.getId(), userId);
    }

    public void unblockUser(Admin admin, Long userId) {
        User user = loadOtherUser(admin, userId);
        user.setBlocked(false);
        users.save(user);
        log.info("Admin {} unblocked user {}", admin.getId(), userId);
    }

    public void deleteUser(Admin admin, Long userId) {
        loadOtherUser(admin, userId);
        users.deleteById(userId);
        log.warn("Admin {} deleted user {}", admin.getId(), userId);
    }

    public List<Job> listJobs(Admin admin) {
        return jobs.findAll();
    }

    public List<Application> listApplications(Admin admin) {
        return applications.findAll();
    }

    /**
     * TODO(#11): implement using Collectors.
     *   - usersByRole: groupingBy(User::getRole, counting()). Make sure every Role appears, even with 0
     *     (hint: start from an EnumMap pre-filled with 0L, or use Collectors.toMap with a merge function).
     *   - activeJobs: count of OPEN jobs; totalJobs: all jobs
     *   - applicationsByStatus: groupingBy(Application::getStatus, counting())
     *   - mostAppliedJobTitle: group applications by jobId -> counting, take the max entry
     *     (Map.Entry.comparingByValue()), then look up that job's title. Empty Optional if no applications.
     *   Practice: try writing totalApplications with reduce() instead of count(), just to see it.
     */
    public PlatformStats getStatistics(Admin admin) {
        throw new UnsupportedOperationException("TODO(#11): AdminService.getStatistics");
    }

    private User loadOtherUser(Admin admin, Long userId) {
        if (admin.getId().equals(userId)) {
            throw new UnauthorizedException("Admins cannot block or delete themselves");
        }
        return users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}

package com.jobportal.service.dto;

import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.Role;

import java.util.Map;
import java.util.Optional;

/**
 * Admin dashboard numbers.
 *
 * @param usersByRole          every Role present as a key (0 if none)
 * @param activeJobs           jobs with status OPEN
 * @param applicationsByStatus only statuses that occur need to be present
 * @param mostAppliedJobTitle  title of the job with the most applications, empty if there are none
 */
public record PlatformStats(long totalUsers, Map<Role, Long> usersByRole, long activeJobs, long totalJobs,
                            long totalApplications, Map<ApplicationStatus, Long> applicationsByStatus,
                            Optional<String> mostAppliedJobTitle) {
}

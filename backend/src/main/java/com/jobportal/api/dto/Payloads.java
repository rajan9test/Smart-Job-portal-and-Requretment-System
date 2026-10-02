package com.jobportal.api.dto;

import com.jobportal.api.http.ApiException;
import com.jobportal.model.ApplicationStatus;
import com.jobportal.model.InterviewType;
import com.jobportal.model.JobType;
import com.jobportal.model.Role;
import com.jobportal.service.dto.JobRequest;
import com.jobportal.service.dto.ScheduleInterviewRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * JSON request bodies. Each validates its own required fields so clients get a 400 with a clear
 * message instead of a NullPointerException deep inside a service.
 */
public final class Payloads {

    private Payloads() {
    }

    static <T> T required(T value, String field) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            throw ApiException.badRequest("'" + field + "' is required");
        }
        return value;
    }

    public record RegisterPayload(Role role, String name, String email, String phone, String password, Long companyId) {
        public RegisterPayload validate() {
            required(role, "role");
            required(name, "name");
            required(email, "email");
            required(password, "password");
            if (role == Role.ADMIN) throw ApiException.forbidden("Admins cannot self-register");
            if (role == Role.RECRUITER) required(companyId, "companyId");
            return this;
        }
    }

    public record LoginPayload(String email, String password) {
        public LoginPayload validate() {
            required(email, "email");
            required(password, "password");
            return this;
        }
    }

    public record JobPayload(String title, String description, Set<String> requiredSkills,
                             Integer minExperience, Integer maxExperience, Long minSalary, Long maxSalary,
                             String location, JobType jobType, Integer openings, LocalDate deadline) {
        public JobRequest toServiceRequest() {
            return new JobRequest(required(title, "title"), description, requiredSkills,
                    required(minExperience, "minExperience"), required(maxExperience, "maxExperience"),
                    required(minSalary, "minSalary"), required(maxSalary, "maxSalary"),
                    required(location, "location"), required(jobType, "jobType"),
                    required(openings, "openings"), deadline);
        }
    }

    public record StatusPayload(ApplicationStatus status) {
        public ApplicationStatus validated() {
            return required(status, "status");
        }
    }

    public record SchedulePayload(Long applicationId, String interviewerEmail, LocalDateTime start,
                                  Integer durationMinutes, InterviewType type) {
        public ScheduleInterviewRequest toServiceRequest() {
            return new ScheduleInterviewRequest(required(applicationId, "applicationId"),
                    required(interviewerEmail, "interviewerEmail"), required(start, "start"),
                    Duration.ofMinutes(required(durationMinutes, "durationMinutes")), required(type, "type"));
        }
    }

    public record ReschedulePayload(LocalDateTime start, Integer durationMinutes) {
        public ReschedulePayload validate() {
            required(start, "start");
            required(durationMinutes, "durationMinutes");
            return this;
        }
    }

    public record CancelPayload(String reason) {
    }

    public record FeedbackPayload(Integer rating, String comments, Boolean recommended) {
        public FeedbackPayload validate() {
            required(rating, "rating");
            required(recommended, "recommended");
            return this;
        }
    }
}

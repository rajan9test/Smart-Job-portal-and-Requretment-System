package com.jobportal.service.dto;

import com.jobportal.model.InterviewType;

import java.time.Duration;
import java.time.LocalDateTime;

public record ScheduleInterviewRequest(Long applicationId, String interviewerEmail,
                                       LocalDateTime start, Duration duration, InterviewType type) {
}

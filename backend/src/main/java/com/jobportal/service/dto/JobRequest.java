package com.jobportal.service.dto;

import com.jobportal.model.Job;
import com.jobportal.model.JobType;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/** What a recruiter submits to create or update a job. Salaries are annual INR. */
public record JobRequest(String title, String description, Set<String> requiredSkills,
                         int minExperience, int maxExperience, long minSalary, long maxSalary,
                         String location, JobType jobType, int openings, LocalDate deadline) {

    public JobRequest {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(jobType, "jobType");
        requiredSkills = requiredSkills == null ? Set.of() : Set.copyOf(requiredSkills);
    }

    /** Copies every field onto the job; Job's setters do the range validation. */
    public void applyTo(Job job) {
        job.setTitle(title);
        job.setDescription(description);
        job.setRequiredSkills(requiredSkills);
        job.setExperienceRange(minExperience, maxExperience);
        job.setSalaryRange(minSalary, maxSalary);
        job.setLocation(location);
        job.setJobType(jobType);
        job.setOpenings(openings);
        job.setDeadline(deadline);
    }
}

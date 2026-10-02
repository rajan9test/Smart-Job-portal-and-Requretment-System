package com.jobportal.model;

import com.jobportal.util.Skills;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Job implements Identifiable {

    private Long id;
    private String title;
    private String description;
    private final Long companyId;
    private final Long recruiterId;
    private final Set<String> requiredSkills = new HashSet<>();
    private int minExperience;
    private int maxExperience;
    /** Annual salary range in INR. */
    private long minSalary;
    private long maxSalary;
    private String location;
    private JobType jobType;
    private int openings;
    private LocalDate deadline;
    private JobStatus status = JobStatus.OPEN;
    private final LocalDateTime createdAt = LocalDateTime.now();

    public Job(String title, Long companyId, Long recruiterId) {
        this.title = Objects.requireNonNull(title, "title");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.recruiterId = Objects.requireNonNull(recruiterId, "recruiterId");
    }

    /** A job accepts applications only while it is OPEN and the deadline has not passed. */
    public boolean isAcceptingApplications(LocalDate today) {
        return status == JobStatus.OPEN && (deadline == null || !today.isAfter(deadline));
    }

    public void close() {
        status = JobStatus.CLOSED;
    }

    public void reopen() {
        status = JobStatus.OPEN;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "title");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getRecruiterId() {
        return recruiterId;
    }

    public Set<String> getRequiredSkills() {
        return Collections.unmodifiableSet(requiredSkills);
    }

    public void setRequiredSkills(Collection<String> skills) {
        requiredSkills.clear();
        requiredSkills.addAll(Skills.normalize(skills));
    }

    public int getMinExperience() {
        return minExperience;
    }

    public int getMaxExperience() {
        return maxExperience;
    }

    public void setExperienceRange(int min, int max) {
        if (min < 0 || max < min) throw new IllegalArgumentException("invalid experience range " + min + "-" + max);
        this.minExperience = min;
        this.maxExperience = max;
    }

    public long getMinSalary() {
        return minSalary;
    }

    public long getMaxSalary() {
        return maxSalary;
    }

    public void setSalaryRange(long min, long max) {
        if (min < 0 || max < min) throw new IllegalArgumentException("invalid salary range " + min + "-" + max);
        this.minSalary = min;
        this.maxSalary = max;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public int getOpenings() {
        return openings;
    }

    public void setOpenings(int openings) {
        if (openings < 1) throw new IllegalArgumentException("openings must be >= 1");
        this.openings = openings;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public JobStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Job{id=%d, title='%s', location='%s', status=%s}".formatted(id, title, location, status);
    }
}

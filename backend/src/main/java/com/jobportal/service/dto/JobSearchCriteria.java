package com.jobportal.service.dto;

import com.jobportal.model.JobType;

import java.util.Set;

/**
 * Optional filters for job search. A null field means "don't filter on this".
 *
 * @param title           case-insensitive substring of the job title
 * @param companyName     case-insensitive substring of the company name
 * @param location        case-insensitive exact match
 * @param experienceYears candidate's experience; the job's [minExperience, maxExperience] must contain it
 * @param minSalary       the job's salary range must overlap [minSalary, maxSalary]
 * @param maxSalary       (either bound may be null)
 * @param skills          the job must require ALL of these skills (case-insensitive)
 * @param jobType         exact match
 * @param includeClosed   closed jobs are excluded unless this is true
 */
public record JobSearchCriteria(String title, String companyName, String location, Integer experienceYears,
                                Long minSalary, Long maxSalary, Set<String> skills, JobType jobType,
                                boolean includeClosed) {

    public static Builder builder() {
        return new Builder();
    }

    /** Builder pattern: avoids a 9-argument constructor call full of nulls. */
    public static final class Builder {
        private String title;
        private String companyName;
        private String location;
        private Integer experienceYears;
        private Long minSalary;
        private Long maxSalary;
        private Set<String> skills;
        private JobType jobType;
        private boolean includeClosed;

        public Builder title(String title) { this.title = title; return this; }
        public Builder companyName(String companyName) { this.companyName = companyName; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder experienceYears(int years) { this.experienceYears = years; return this; }
        public Builder minSalary(long minSalary) { this.minSalary = minSalary; return this; }
        public Builder maxSalary(long maxSalary) { this.maxSalary = maxSalary; return this; }
        public Builder skills(String... skills) { this.skills = Set.of(skills); return this; }
        public Builder jobType(JobType jobType) { this.jobType = jobType; return this; }
        public Builder includeClosed(boolean includeClosed) { this.includeClosed = includeClosed; return this; }

        public JobSearchCriteria build() {
            return new JobSearchCriteria(title, companyName, location, experienceYears,
                    minSalary, maxSalary, skills, jobType, includeClosed);
        }
    }
}

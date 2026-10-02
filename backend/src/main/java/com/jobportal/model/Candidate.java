package com.jobportal.model;

import com.jobportal.util.Skills;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Candidate extends User {

    /** HashSet: O(1) membership checks, which the matching engine relies on. Stored normalized (lowercase). */
    private final Set<String> skills = new HashSet<>();
    private int experienceYears;
    private String education;
    private Resume resume;
    /** Expected annual salary in INR. */
    private long expectedSalary;
    private String location;

    public Candidate(String name, String email, String phone, String passwordHash) {
        super(name, email, phone, passwordHash, Role.CANDIDATE);
    }

    @Override
    public String showDashboard() {
        return "Candidate dashboard for %s: %d skills, %d yrs experience, location %s"
                .formatted(getName(), skills.size(), experienceYears, location);
    }

    public Set<String> getSkills() {
        return Collections.unmodifiableSet(skills);
    }

    public void setSkills(Collection<String> newSkills) {
        skills.clear();
        skills.addAll(Skills.normalize(newSkills));
    }

    public void addSkill(String skill) {
        skills.addAll(Skills.normalize(Set.of(skill)));
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        if (experienceYears < 0) throw new IllegalArgumentException("experienceYears must be >= 0");
        this.experienceYears = experienceYears;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public Resume getResume() {
        return resume;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
    }

    public long getExpectedSalary() {
        return expectedSalary;
    }

    public void setExpectedSalary(long expectedSalary) {
        if (expectedSalary < 0) throw new IllegalArgumentException("expectedSalary must be >= 0");
        this.expectedSalary = expectedSalary;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}

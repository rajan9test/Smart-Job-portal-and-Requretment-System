package com.jobportal.service;

import com.jobportal.model.Candidate;
import com.jobportal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/** Candidates edit their own profile: the data the matching engine scores against. */
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    /** Fields a candidate can change. A null field means "leave as is". */
    public record CandidateProfileUpdate(String name, String phone, Set<String> skills, Integer experienceYears,
                                         String education, Long expectedSalary, String location) {
    }

    private final UserRepository users;

    public ProfileService(UserRepository users) {
        this.users = users;
    }

    public Candidate updateCandidateProfile(Candidate candidate, CandidateProfileUpdate update) {
        if (update.name() != null && !update.name().isBlank()) candidate.setName(update.name().trim());
        if (update.phone() != null) candidate.setPhone(update.phone());
        if (update.skills() != null) candidate.setSkills(update.skills());
        if (update.experienceYears() != null) candidate.setExperienceYears(update.experienceYears());
        if (update.education() != null) candidate.setEducation(update.education());
        if (update.expectedSalary() != null) candidate.setExpectedSalary(update.expectedSalary());
        if (update.location() != null) candidate.setLocation(update.location());
        users.save(candidate);
        log.info("Candidate {} updated their profile", candidate.getId());
        return candidate;
    }
}

package com.jobportal.repository;

import com.jobportal.model.Application;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends Repository<Application, Long> {

    List<Application> findByJobId(Long jobId);

    List<Application> findByCandidateId(Long candidateId);

    Optional<Application> findByJobIdAndCandidateId(Long jobId, Long candidateId);
}

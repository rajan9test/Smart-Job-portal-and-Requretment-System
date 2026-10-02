package com.jobportal.repository.inmemory;

import com.jobportal.model.Application;
import com.jobportal.repository.ApplicationRepository;

import java.util.List;
import java.util.Optional;

public class InMemoryApplicationRepository extends InMemoryRepository<Application> implements ApplicationRepository {

    @Override
    public List<Application> findByJobId(Long jobId) {
        return findWhere(a -> a.getJobId().equals(jobId));
    }

    @Override
    public List<Application> findByCandidateId(Long candidateId) {
        return findWhere(a -> a.getCandidateId().equals(candidateId));
    }

    @Override
    public Optional<Application> findByJobIdAndCandidateId(Long jobId, Long candidateId) {
        return store.values().stream()
                .filter(a -> a.getJobId().equals(jobId) && a.getCandidateId().equals(candidateId))
                .findFirst();
    }
}

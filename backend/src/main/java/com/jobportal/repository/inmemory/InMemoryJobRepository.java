package com.jobportal.repository.inmemory;

import com.jobportal.model.Job;
import com.jobportal.repository.JobRepository;

import java.util.List;

public class InMemoryJobRepository extends InMemoryRepository<Job> implements JobRepository {

    @Override
    public List<Job> findByCompanyId(Long companyId) {
        return findWhere(j -> j.getCompanyId().equals(companyId));
    }
}

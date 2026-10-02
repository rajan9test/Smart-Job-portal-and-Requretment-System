package com.jobportal.repository;

import com.jobportal.model.Job;

import java.util.List;

public interface JobRepository extends Repository<Job, Long> {

    List<Job> findByCompanyId(Long companyId);
}

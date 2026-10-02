package com.jobportal.service;

import com.jobportal.exception.JobNotFoundException;
import com.jobportal.model.Job;
import com.jobportal.model.Recruiter;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.service.dto.JobRequest;
import com.jobportal.service.dto.JobSearchCriteria;
import com.jobportal.service.dto.Page;
import com.jobportal.service.dto.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobs;
    private final CompanyRepository companies;
    private final AccessPolicy access;

    public JobService(JobRepository jobs, CompanyRepository companies, AccessPolicy access) {
        this.jobs = jobs;
        this.companies = companies;
        this.access = access;
    }

    public Job createJob(Recruiter recruiter, JobRequest request) {
        if (recruiter.getCompanyId() == null) {
            throw new IllegalStateException("Recruiter must belong to a company to post jobs");
        }
        Job job = new Job(request.title(), recruiter.getCompanyId(), recruiter.getId());
        request.applyTo(job);
        jobs.save(job);
        log.info("Recruiter {} created job {} '{}'", recruiter.getId(), job.getId(), job.getTitle());
        return job;
    }

    public Job getJob(Long jobId) {
        return jobs.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    }

    public Job updateJob(Recruiter recruiter, Long jobId, JobRequest request) {
        Job job = getJob(jobId);
        access.requireCanModifyJob(recruiter, job);
        request.applyTo(job);
        jobs.save(job);
        log.info("Recruiter {} updated job {}", recruiter.getId(), jobId);
        return job;
    }

    public void deleteJob(Recruiter recruiter, Long jobId) {
        Job job = getJob(jobId);
        access.requireCanModifyJob(recruiter, job);
        // Exercise: what should happen to the job's applications and interviews? (Phase 2: FK + ON DELETE)
        jobs.deleteById(jobId);
        log.info("Recruiter {} deleted job {}", recruiter.getId(), jobId);
    }

    public Job closeJob(Recruiter recruiter, Long jobId) {
        Job job = getJob(jobId);
        access.requireCanModifyJob(recruiter, job);
        job.close();
        log.info("Job {} closed", jobId);
        return jobs.save(job);
    }

    public Job reopenJob(Recruiter recruiter, Long jobId) {
        Job job = getJob(jobId);
        access.requireCanModifyJob(recruiter, job);
        job.reopen();
        log.info("Job {} reopened", jobId);
        return jobs.save(job);
    }

    public List<Job> getJobsForCompany(Long companyId) {
        return jobs.findByCompanyId(companyId);
    }

    /**
     * Filters, sorts and paginates jobs.
     *
     * TODO(#10): implement with a single stream pipeline over jobs.findAll().
     *   1. Filter: apply every non-null field of {@code criteria} (semantics in JobSearchCriteria's javadoc).
     *      Tip: build a {@code Predicate<Job>} per criterion and combine them with .and(), which keeps
     *      each rule small and testable. Company name needs companies.findById(job.getCompanyId()).
     *      The salary filter is the same interval-overlap logic as TimeSlot (TODO #1).
     *   2. Sort: by page.sortBy(): "createdAt", "salary" (use maxSalary), "experience" (minExperience),
     *      or "title" (case-insensitive); reverse for DESC. Add .thenComparing(Job::getId) so pages are
     *      stable when values tie. A Map<String, Comparator<Job>> beats a long if/else.
     *   3. Paginate: count the filtered total BEFORE skipping, then skip(page.offset()).limit(page.size()).
     *   Return new Page<>(content, page.page(), page.size(), total).
     *
     *   In Phase 2 this becomes a dynamic SQL query with WHERE / ORDER BY / LIMIT / OFFSET. Keep the
     *   semantics identical so the tests carry over.
     */
    public Page<Job> search(JobSearchCriteria criteria, PageRequest page) {
        throw new UnsupportedOperationException("TODO(#10): JobService.search");
    }
}

package com.jobportal.api.controller;

import com.jobportal.PortalApplication;
import com.jobportal.api.dto.Payloads.JobPayload;
import com.jobportal.api.dto.Views;
import com.jobportal.api.http.ApiException;
import com.jobportal.api.http.Request;
import com.jobportal.api.http.Response;
import com.jobportal.api.http.Router;
import com.jobportal.matching.ExperienceMatchingStrategy;
import com.jobportal.matching.MatchingStrategy;
import com.jobportal.matching.SalaryMatchingStrategy;
import com.jobportal.matching.SkillMatchingStrategy;
import com.jobportal.matching.WeightedMatchingStrategy;
import com.jobportal.model.Job;
import com.jobportal.model.JobType;
import com.jobportal.model.Recruiter;
import com.jobportal.service.dto.JobSearchCriteria;
import com.jobportal.service.dto.PageRequest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Job search (public), job management (recruiter), applying (candidate), applicants and ranking (recruiter). */
public class JobController {

    private final PortalApplication app;
    private final Views views;

    public JobController(PortalApplication app, Views views) {
        this.app = app;
        this.views = views;
    }

    public void register(Router router) {
        // Literal paths first so "/mine" is not captured by "/{id}".
        router.get("/api/jobs", req -> views.jobPage(app.jobService.search(criteria(req), page(req))));

        router.get("/api/jobs/mine", req -> {
            Recruiter recruiter = req.recruiter();
            return app.jobService.getJobsForCompany(recruiter.getCompanyId()).stream()
                    .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                    .map(views::job)
                    .toList();
        });

        router.get("/api/jobs/{id}", req -> views.job(app.jobService.getJob(req.pathLong("id"))));

        router.post("/api/jobs", req -> Response.created(views.job(
                app.jobService.createJob(req.recruiter(), req.body(JobPayload.class).toServiceRequest()))));

        router.put("/api/jobs/{id}", req -> views.job(app.jobService.updateJob(
                req.recruiter(), req.pathLong("id"), req.body(JobPayload.class).toServiceRequest())));

        router.delete("/api/jobs/{id}", req -> {
            app.jobService.deleteJob(req.recruiter(), req.pathLong("id"));
            return Response.noContent();
        });

        router.post("/api/jobs/{id}/close", req -> views.job(app.jobService.closeJob(req.recruiter(), req.pathLong("id"))));
        router.post("/api/jobs/{id}/reopen", req -> views.job(app.jobService.reopenJob(req.recruiter(), req.pathLong("id"))));

        router.post("/api/jobs/{id}/apply", req -> Response.created(views.application(
                app.applicationService.apply(req.candidate(), req.pathLong("id")))));

        router.get("/api/jobs/{id}/applicants", req -> app.applicationService
                .getApplicantsForJob(req.recruiter(), req.pathLong("id")).stream()
                .map(views::application)
                .toList());

        router.get("/api/jobs/{id}/ranking", req -> app.matchingService
                .rankApplicants(req.recruiter(), req.pathLong("id"), strategy(req.query("strategy", "weighted"))).stream()
                .map(views::match)
                .toList());
    }

    /** GET /api/jobs?title=java&company=acme&location=Pune&experience=2&minSalary=..&maxSalary=..&skills=java,sql&jobType=FULL_TIME */
    private static JobSearchCriteria criteria(Request req) {
        String skills = req.query("skills");
        Set<String> skillSet = skills == null ? null : Arrays.stream(skills.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        String jobType = req.query("jobType");
        return new JobSearchCriteria(req.query("title"), req.query("company"), req.query("location"),
                req.queryInt("experience"), req.queryLong("minSalary"), req.queryLong("maxSalary"),
                skillSet, jobType == null ? null : JobType.valueOf(jobType.toUpperCase(Locale.ROOT)),
                Boolean.parseBoolean(req.query("includeClosed", "false")));
    }

    /** ?page=0&size=10&sort=salary,desc */
    private static PageRequest page(Request req) {
        int page = req.queryInt("page") == null ? 0 : req.queryInt("page");
        int size = req.queryInt("size") == null ? 10 : req.queryInt("size");
        return PageRequest.of(page, size, req.query("sort", "createdAt,desc"));
    }

    private static MatchingStrategy strategy(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "skill", "skills" -> new SkillMatchingStrategy();
            case "experience" -> new ExperienceMatchingStrategy();
            case "salary" -> new SalaryMatchingStrategy();
            case "weighted" -> new WeightedMatchingStrategy()
                    .with(new SkillMatchingStrategy(), 0.6)
                    .with(new ExperienceMatchingStrategy(), 0.3)
                    .with(new SalaryMatchingStrategy(), 0.1);
            default -> throw ApiException.badRequest("Unknown strategy '" + name + "'. Use skill, experience, salary or weighted");
        };
    }
}

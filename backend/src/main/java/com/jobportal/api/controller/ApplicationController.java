package com.jobportal.api.controller;

import com.jobportal.PortalApplication;
import com.jobportal.api.dto.Payloads.StatusPayload;
import com.jobportal.api.dto.Views;
import com.jobportal.api.http.Router;

/** Candidate's application history / withdraw, and recruiter status changes. */
public class ApplicationController {

    private final PortalApplication app;
    private final Views views;

    public ApplicationController(PortalApplication app, Views views) {
        this.app = app;
        this.views = views;
    }

    public void register(Router router) {
        router.get("/api/applications", req -> app.applicationService
                .getApplicationsForCandidate(req.candidate()).stream()
                .map(views::application)
                .toList());

        router.post("/api/applications/{id}/withdraw", req -> views.application(
                app.applicationService.withdraw(req.candidate(), req.pathLong("id"))));

        router.put("/api/applications/{id}/status", req -> views.application(app.applicationService.updateStatus(
                req.recruiter(), req.pathLong("id"), req.body(StatusPayload.class).validated())));
    }
}

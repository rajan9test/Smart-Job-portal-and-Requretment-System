package com.jobportal.api.controller;

import com.jobportal.PortalApplication;
import com.jobportal.api.dto.Views;
import com.jobportal.api.http.Router;
import com.jobportal.service.ProfileService.CandidateProfileUpdate;

/** The logged-in candidate edits their profile. */
public class ProfileController {

    private final PortalApplication app;
    private final Views views;

    public ProfileController(PortalApplication app, Views views) {
        this.app = app;
        this.views = views;
    }

    public void register(Router router) {
        router.put("/api/profile", req -> views.user(
                app.profileService.updateCandidateProfile(req.candidate(), req.body(CandidateProfileUpdate.class))));
    }
}

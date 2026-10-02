package com.jobportal.api.controller;

import com.jobportal.PortalApplication;
import com.jobportal.api.dto.Views;
import com.jobportal.api.http.Router;
import com.jobportal.model.Company;

import java.util.Comparator;

/** Public list of companies (used by the recruiter sign-up form). */
public class CompanyController {

    private final PortalApplication app;
    private final Views views;

    public CompanyController(PortalApplication app, Views views) {
        this.app = app;
        this.views = views;
    }

    public void register(Router router) {
        router.get("/api/companies", req -> app.companies.findAll().stream()
                .sorted(Comparator.comparing(Company::getName))
                .map(views::company)
                .toList());
    }
}

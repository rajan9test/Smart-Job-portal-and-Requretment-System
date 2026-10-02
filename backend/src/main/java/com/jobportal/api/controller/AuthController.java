package com.jobportal.api.controller;

import com.jobportal.PortalApplication;
import com.jobportal.api.dto.Payloads.LoginPayload;
import com.jobportal.api.dto.Payloads.RegisterPayload;
import com.jobportal.api.dto.Views;
import com.jobportal.api.http.Response;
import com.jobportal.api.http.Router;
import com.jobportal.model.User;
import com.jobportal.security.Session;

/** Register, login, logout, current user. Public endpoints except /me and /logout. */
public class AuthController {

    private final PortalApplication app;
    private final Views views;

    public AuthController(PortalApplication app, Views views) {
        this.app = app;
        this.views = views;
    }

    public void register(Router router) {
        router.post("/api/auth/register", req -> {
            RegisterPayload p = req.body(RegisterPayload.class).validate();
            switch (p.role()) {
                case CANDIDATE -> app.authService.registerCandidate(p.name(), p.email(), p.phone(), p.password());
                case RECRUITER -> app.authService.registerRecruiter(p.name(), p.email(), p.phone(), p.password(), p.companyId());
                case ADMIN -> throw new IllegalStateException("unreachable: validate() rejects ADMIN");
            }
            // Log the new user straight in so the client doesn't need a second round trip.
            return Response.created(login(p.email(), p.password()));
        });

        router.post("/api/auth/login", req -> {
            LoginPayload p = req.body(LoginPayload.class).validate();
            return login(p.email(), p.password());
        });

        router.post("/api/auth/logout", req -> {
            String token = req.token();
            if (token != null) app.authService.logout(token);
            return Response.noContent();
        });

        router.get("/api/auth/me", req -> views.user(req.user()));
    }

    private Views.LoginView login(String email, String password) {
        Session session = app.authService.login(email, password);
        User user = app.authService.authenticate(session.token());
        return views.login(session, user);
    }
}

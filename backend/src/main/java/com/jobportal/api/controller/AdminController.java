package com.jobportal.api.controller;

import com.jobportal.PortalApplication;
import com.jobportal.api.dto.Views;
import com.jobportal.api.http.Response;
import com.jobportal.api.http.Router;
import com.jobportal.model.Admin;
import com.jobportal.model.Role;

import java.util.Locale;

/** Admin-only: users, platform-wide jobs/applications, statistics. */
public class AdminController {

    private final PortalApplication app;
    private final Views views;

    public AdminController(PortalApplication app, Views views) {
        this.app = app;
        this.views = views;
    }

    public void register(Router router) {
        router.get("/api/admin/users", req -> {
            Admin admin = req.admin();
            String role = req.query("role");
            var users = role == null ? app.adminService.listUsers(admin)
                    : app.adminService.listUsersByRole(admin, Role.valueOf(role.toUpperCase(Locale.ROOT)));
            return users.stream().map(views::user).toList();
        });

        router.post("/api/admin/users/{id}/block", req -> {
            app.adminService.blockUser(req.admin(), req.pathLong("id"));
            return Response.noContent();
        });

        router.post("/api/admin/users/{id}/unblock", req -> {
            app.adminService.unblockUser(req.admin(), req.pathLong("id"));
            return Response.noContent();
        });

        router.delete("/api/admin/users/{id}", req -> {
            app.adminService.deleteUser(req.admin(), req.pathLong("id"));
            return Response.noContent();
        });

        router.get("/api/admin/jobs", req -> app.adminService.listJobs(req.admin()).stream().map(views::job).toList());

        router.get("/api/admin/applications", req ->
                app.adminService.listApplications(req.admin()).stream().map(views::application).toList());

        router.get("/api/admin/stats", req -> app.adminService.getStatistics(req.admin()));
    }
}

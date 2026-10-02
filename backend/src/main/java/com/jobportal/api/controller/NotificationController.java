package com.jobportal.api.controller;

import com.jobportal.PortalApplication;
import com.jobportal.api.dto.Views;
import com.jobportal.api.http.Response;
import com.jobportal.api.http.Router;
import com.jobportal.model.User;

import java.util.List;

/** The logged-in user's in-app inbox. */
public class NotificationController {

    public record InboxView(long unread, List<Views.NotificationView> items) {
    }

    private final PortalApplication app;
    private final Views views;

    public NotificationController(PortalApplication app, Views views) {
        this.app = app;
        this.views = views;
    }

    public void register(Router router) {
        router.get("/api/notifications", req -> {
            User user = req.user();
            return new InboxView(app.inboxService.unreadCount(user),
                    app.inboxService.getInbox(user).stream().map(views::notification).toList());
        });

        router.post("/api/notifications/read-all", req -> {
            app.inboxService.markAllRead(req.user());
            return Response.noContent();
        });

        router.post("/api/notifications/{id}/read", req -> {
            app.inboxService.markRead(req.user(), req.pathLong("id"));
            return Response.noContent();
        });
    }
}

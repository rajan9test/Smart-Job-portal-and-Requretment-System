package com.jobportal.api.controller;

import com.jobportal.PortalApplication;
import com.jobportal.api.dto.Payloads.CancelPayload;
import com.jobportal.api.dto.Payloads.FeedbackPayload;
import com.jobportal.api.dto.Payloads.ReschedulePayload;
import com.jobportal.api.dto.Payloads.SchedulePayload;
import com.jobportal.api.dto.Views;
import com.jobportal.api.http.ApiException;
import com.jobportal.api.http.Response;
import com.jobportal.api.http.Router;
import com.jobportal.model.Candidate;
import com.jobportal.model.Interview;
import com.jobportal.model.Recruiter;
import com.jobportal.model.User;

import java.time.Duration;
import java.util.List;

/** Interview scheduling (recruiter) and listing (candidate or recruiter). */
public class InterviewController {

    private final PortalApplication app;
    private final Views views;

    public InterviewController(PortalApplication app, Views views) {
        this.app = app;
        this.views = views;
    }

    public void register(Router router) {
        router.get("/api/interviews", req -> {
            User user = req.user();
            List<Interview> interviews = switch (user) {
                case Candidate c -> app.interviewService.getInterviewsForCandidate(c);
                case Recruiter r -> app.interviewService.getInterviewsForRecruiter(r);
                default -> throw ApiException.forbidden("Only candidates and recruiters have interviews");
            };
            return interviews.stream().map(views::interview).toList();
        });

        router.post("/api/interviews", req -> Response.created(views.interview(
                app.interviewService.schedule(req.recruiter(), req.body(SchedulePayload.class).toServiceRequest()))));

        router.put("/api/interviews/{id}", req -> {
            ReschedulePayload p = req.body(ReschedulePayload.class).validate();
            return views.interview(app.interviewService.reschedule(
                    req.recruiter(), req.pathLong("id"), p.start(), Duration.ofMinutes(p.durationMinutes())));
        });

        router.post("/api/interviews/{id}/cancel", req -> {
            String reason = req.body(CancelPayload.class).reason();
            return views.interview(app.interviewService.cancel(req.recruiter(), req.pathLong("id"),
                    reason == null || reason.isBlank() ? "No reason given" : reason));
        });

        router.post("/api/interviews/{id}/complete", req -> views.interview(
                app.interviewService.complete(req.recruiter(), req.pathLong("id"))));

        router.post("/api/interviews/{id}/feedback", req -> {
            FeedbackPayload p = req.body(FeedbackPayload.class).validate();
            return views.interview(app.interviewService.submitFeedback(
                    req.recruiter(), req.pathLong("id"), p.rating(), p.comments(), p.recommended()));
        });
    }
}

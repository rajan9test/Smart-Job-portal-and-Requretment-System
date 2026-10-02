# 06 · Applications

**Class:** `service.ApplicationService`

Connects candidates to jobs and moves each application through the hiring pipeline.

## Lifecycle

```
            candidate                recruiter                  recruiter            recruiter
   (none) ───apply───► APPLIED ───► SHORTLISTED ──(schedule)──► INTERVIEW ───► SELECTED
                           │             │                          │
                           └─────────────┴──────────────────────────┴──► REJECTED   (recruiter)
                           └─────────────┴──────────────────────────┴──► WITHDRAWN  (candidate)
```

Allowed moves are defined by `ApplicationStatus.canTransitionTo` (TODO #2) and enforced in `Application.changeStatus`. Illegal moves throw `InvalidStatusTransitionException` (HTTP 409); for example, a rejected candidate can't be selected.

## Operations

| Method | Who | Rules |
|--------|-----|-------|
| `apply(candidate, jobId)` | candidate | **exercise TODO #8**: job must exist (404), accept applications (`JobClosedException`, 409), and not already have this candidate's application (`AlreadyAppliedException`, 409). Saves, then publishes `ApplicationSubmitted` so the recruiter is notified. |
| `withdraw(candidate, applicationId)` | candidate | only their own application (`UnauthorizedException`), then status → WITHDRAWN |
| `updateStatus(recruiter, applicationId, status)` | recruiter | must own the job (`AccessPolicy`); cannot set WITHDRAWN; the state machine decides the rest |
| `getApplicationsForCandidate(candidate)` | candidate | newest first |
| `getApplicantsForJob(recruiter, jobId)` | recruiter | must own the job; oldest first |
| `getApplication(id)` | | `ApplicationNotFoundException` |

Every status change saves the application, appends to its history, logs at INFO and publishes `ApplicationStatusChanged`. The notification listener then tells the candidate, or tells the recruiter if the candidate withdrew.

The INTERVIEW status is normally set by **scheduling an interview** (see [07 · Interviews](07-interviews.md)), not by hand.

## Design questions left open (TODO #8)

- May a candidate who withdrew apply again? Pick a rule, document it in the Javadoc, and add a test.
- Two simultaneous `apply` calls for the same candidate and job can both pass the "already applied" check. How would you prevent that in memory? In Phase 2 a `UNIQUE(job_id, candidate_id)` constraint solves it.

## Endpoints

| Method | Path | Who |
|--------|------|-----|
| POST | `/api/jobs/{id}/apply` | candidate (201) |
| GET | `/api/applications` | candidate: own applications |
| POST | `/api/applications/{id}/withdraw` | candidate |
| PUT | `/api/applications/{id}/status` body `{"status":"SHORTLISTED"}` | recruiter |
| GET | `/api/jobs/{id}/applicants` | recruiter: applications with candidate profiles |

## Tests

`ApplicationServiceTest` is the **Mockito** example: repositories and the event publisher are mocks, and tests verify both results and side effects (`verify(events).publish(...)`, `verify(applications, never()).save(any())`). `ApplicationStatusTest` covers the state machine.

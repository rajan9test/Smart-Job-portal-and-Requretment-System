# 12 · REST API

**Package:** `com.jobportal.api` · **Entry point:** `api.ApiServer`

A small JSON-over-HTTP layer on top of the services, built on the JDK's own `com.sun.net.httpserver` with Jackson for JSON. There's no web framework on purpose: you can see every step Spring would otherwise hide, which makes Phase 3 easier to understand.

## Structure

```
api/
├── ApiServer               main(): builds PortalApplication, loads DemoData, registers controllers, starts HttpServer
├── http/
│   ├── Router              "METHOD /path/{id}" → Handler (regex per template)
│   ├── ApiHttpHandler      front controller: CORS → route → handler → JSON; errors via ExceptionMapper
│   ├── Request             path vars, query params, JSON body, current user + role helpers
│   ├── Response            (status, body); handlers may also return a plain object (200) or null (204)
│   ├── ExceptionMapper     exception → status + ErrorBody
│   ├── ApiException        HTTP-level errors (400/401/403/404/405)
│   └── Json                the configured ObjectMapper
├── controller/             AuthController, CompanyController, ProfileController, JobController,
│                           ApplicationController, InterviewController, NotificationController, AdminController
└── dto/
    ├── Payloads            request bodies, each validating its own required fields (→ 400)
    └── Views               response records + mapping from domain objects
```

## Request lifecycle

```
HttpServer (virtual thread per request)
  └─ ApiHttpHandler.handle
       ├─ add CORS headers; OPTIONS → 204
       ├─ Router.match(method, path) → 404 / 405
       ├─ handler(Request)
       │     ├─ req.recruiter()        → 401 if no/invalid token, 403 if wrong role
       │     ├─ req.body(Payload.class).validate() → 400 on bad JSON / missing fields
       │     ├─ service call            → business exceptions
       │     └─ views.xxx(domain)       → response DTO
       ├─ exception → ExceptionMapper → ErrorBody
       ├─ serialize JSON, send
       └─ log "POST /api/jobs/1/apply -> 201 (4 ms)"
```

Domain objects are never serialized directly: `Views` maps them to records, so password hashes can't leak and internal renames don't break clients.

## Conventions

- Base URL: `http://localhost:8080/api`
- Auth: `Authorization: Bearer <token>` (from login/register)
- JSON with camelCase fields; null fields are omitted
- Dates `"2026-11-01"`, date-times `"2026-10-03T11:00:00"` (local, no zone)
- Money: annual INR as an integer (`1000000` = 10 LPA)
- Success codes: 200 (body), 201 (created), 204 (no body)

## Error format

```json
{ "status": 409, "error": "Conflict", "message": "You already applied to this job", "todo": null }
```

| Status | When |
|--------|------|
| 400 | invalid JSON, missing field, bad parameter, domain validation |
| 401 | not logged in, expired session, wrong credentials |
| 403 | wrong role, not the owner, blocked account |
| 404 | unknown entity or endpoint |
| 405 | endpoint exists but not for this HTTP method |
| 409 | business rule conflict (already applied, closed job, illegal status move, interview clash, duplicate email) |
| 501 | the feature is an unfinished exercise; `todo` contains e.g. `"TODO(#8)"` |
| 500 | unexpected error (details only in the server log) |

## Endpoint reference

| Method | Path | Role | Body / query |
|--------|------|------|--------------|
| GET | `/api/health` | public | |
| POST | `/api/auth/register` | public | `{role: CANDIDATE\|RECRUITER, name, email, phone?, password, companyId?}` → login |
| POST | `/api/auth/login` | public | `{email, password}` → `{token, expiresAt, user}` |
| POST | `/api/auth/logout` | any | |
| GET | `/api/auth/me` | logged in | |
| GET | `/api/companies` | public | |
| PUT | `/api/profile` | candidate | `{name?, phone?, skills?, experienceYears?, education?, expectedSalary?, location?}` |
| GET | `/api/jobs` | public | `title, company, location, experience, minSalary, maxSalary, skills, jobType, includeClosed, page, size, sort` |
| GET | `/api/jobs/mine` | recruiter | |
| GET | `/api/jobs/{id}` | public | |
| POST | `/api/jobs` | recruiter | `{title, description?, requiredSkills[], minExperience, maxExperience, minSalary, maxSalary, location, jobType, openings, deadline?}` |
| PUT | `/api/jobs/{id}` | recruiter | same as POST |
| DELETE | `/api/jobs/{id}` | recruiter | |
| POST | `/api/jobs/{id}/close` · `/reopen` | recruiter | |
| POST | `/api/jobs/{id}/apply` | candidate | |
| GET | `/api/jobs/{id}/applicants` | recruiter | |
| GET | `/api/jobs/{id}/ranking` | recruiter | `strategy=weighted\|skill\|experience\|salary` |
| GET | `/api/applications` | candidate | |
| POST | `/api/applications/{id}/withdraw` | candidate | |
| PUT | `/api/applications/{id}/status` | recruiter | `{status}` |
| GET | `/api/interviews` | candidate / recruiter | |
| POST | `/api/interviews` | recruiter | `{applicationId, interviewerEmail, start, durationMinutes, type}` |
| PUT | `/api/interviews/{id}` | recruiter | `{start, durationMinutes}` |
| POST | `/api/interviews/{id}/cancel` | recruiter | `{reason}` |
| POST | `/api/interviews/{id}/complete` | recruiter | |
| POST | `/api/interviews/{id}/feedback` | recruiter | `{rating, comments, recommended}` |
| GET | `/api/notifications` | logged in | |
| POST | `/api/notifications/{id}/read` · `/read-all` | logged in | |
| GET | `/api/admin/users` | admin | `role?` |
| POST | `/api/admin/users/{id}/block` · `/unblock` | admin | |
| DELETE | `/api/admin/users/{id}` | admin | |
| GET | `/api/admin/jobs` · `/applications` · `/stats` | admin | |

## Try it with curl

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"amit@acme.dev","password":"password123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -s localhost:8080/api/jobs/mine -H "Authorization: Bearer $TOKEN"
curl -s "localhost:8080/api/jobs?title=java&sort=salary,desc"
```

## Adding an endpoint

1. Add the service method (business logic stays in `service/`).
2. Add a request record to `Payloads` (with `validate()`) and/or a response record + mapper to `Views`.
3. Register the route in the right controller's `register(Router)`. Put literal paths (`/jobs/mine`) **before** variable ones (`/jobs/{id}`).
4. If the service throws a new exception type, map it in `ExceptionMapper`.

## Phase 3 mapping to Spring Boot

| Here | Spring |
|------|--------|
| `Router.get("/api/jobs/{id}", …)` | `@GetMapping("/api/jobs/{id}")` |
| `req.pathLong("id")`, `req.query(...)` | `@PathVariable`, `@RequestParam` |
| `req.body(X.class).validate()` | `@RequestBody @Valid X` |
| `ExceptionMapper` | `@RestControllerAdvice` |
| `req.recruiter()` | `@PreAuthorize("hasRole('RECRUITER')")` |
| `ApiServer` wiring | `@SpringBootApplication` + component scan |

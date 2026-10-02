# Module documentation

One page per module. Each page covers what the module is responsible for, its main classes, how a request flows through it, its rules, and how to extend it. For setup instructions see the [root README](../README.md).

## How the modules fit together

```
 React UI (frontend/)
        │  fetch /api/...  (Vite proxy in dev)
        ▼
 REST layer (api/)           Router → Controller → Views (JSON DTOs) · ExceptionMapper
        │
        ▼
 Services (service/)         Auth · Profile · Job · Application · Interview · Matching · Admin · Inbox
        │        │                     │
        │        │ uses                └── publishes PortalEvent ──► EventPublisher (event/)
        │        ▼                                                      │
        │   matching/  (strategies)                                     ▼
        │                                           NotificationEventListener (notification/)
        ▼                                                               │ submit
 Repositories (repository/)  interfaces + in-memory impls               ▼
        │                                           NotificationDispatcher ── worker threads ──► Email / SMS / In-App
        ▼
 Domain model (model/)  ·  Exceptions (exception/)  ·  Config (config/)
```

Dependencies only point downwards: services never know about HTTP, and the model knows nothing about services. That is what will let Phase 3 swap `api/` for Spring controllers, and Phase 2 swap the in-memory repositories for JDBC ones, without touching the business logic.

## Modules

| # | Module | Package / folder |
|---|--------|------------------|
| 01 | [Domain model](modules/01-domain-model.md) | `model` |
| 02 | [Exceptions](modules/02-exceptions.md) | `exception` |
| 03 | [Repositories](modules/03-repositories.md) | `repository` |
| 04 | [Users, authentication & authorization](modules/04-users-and-auth.md) | `security`, `service.AuthService`, `service.ProfileService`, `service.AccessPolicy` |
| 05 | [Jobs & search](modules/05-jobs.md) | `service.JobService`, `service.dto` |
| 06 | [Applications](modules/06-applications.md) | `service.ApplicationService` |
| 07 | [Interviews](modules/07-interviews.md) | `service.InterviewService` |
| 08 | [Matching & ranking](modules/08-matching.md) | `matching`, `service.MatchingService` |
| 09 | [Events & notifications](modules/09-notifications-and-events.md) | `event`, `notification`, `service.InboxService` |
| 10 | [Admin](modules/10-admin.md) | `service.AdminService` |
| 11 | [Configuration, wiring & logging](modules/11-configuration.md) | `config`, `PortalApplication`, `DemoData` |
| 12 | [REST API](modules/12-rest-api.md) | `api` |
| 13 | [Frontend](modules/13-frontend.md) | `frontend/` |

All Java packages are under `backend/src/main/java/com/jobportal/`.

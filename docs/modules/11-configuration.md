# 11 · Configuration, wiring & logging

**Classes:** `config.ConfigurationManager`, `PortalApplication`, `DemoData`, `Main` · **Resources:** `application.properties`, `logback.xml`

## `ConfigurationManager` (Singleton)

Loads `backend/src/main/resources/application.properties` once and exposes `getString`, `getInt` and `getLong`, each with a default.

It uses the **initialization-on-demand holder** idiom:

```java
private static final class Holder {
    private static final ConfigurationManager INSTANCE = new ConfigurationManager();
}
public static ConfigurationManager getInstance() { return Holder.INSTANCE; }
```

The JVM initializes `Holder` lazily, on first call, and class initialization is thread-safe, so no `synchronized` or `volatile` is needed.

### Properties

| Key | Default | Used by |
|-----|---------|---------|
| `server.port` | 8080 | `ApiServer` |
| `server.cors.origin` | `http://localhost:5173` | `ApiHttpHandler` CORS headers |
| `demo.data.enabled` | true | `ApiServer` → `DemoData.seed` |
| `auth.session.ttl.minutes` | 60 | `AuthService` |
| `auth.password.min.length` | 8 | `AuthService` |
| `auth.pbkdf2.iterations` | 210000 | `PasswordHasher` |
| `notification.worker.threads` | 2 | `NotificationDispatcher` |
| `notification.channels` | `IN_APP,EMAIL` | `NotificationEventListener` |
| `pagination.default.size` / `pagination.max.size` | 20 / 100 | reserved (the API currently defaults to 10, max 100) |

## `PortalApplication`: the composition root

Creates every repository, service and the notification pipeline, and passes dependencies in through **constructors**. There are no static lookups or `new` calls inside services.

```
repositories ─┐
EventPublisher├─► services (Auth, Job, Application, Interview, Matching, Admin, Profile, Inbox)
Clock ────────┘
NotificationFactory → NotificationDispatcher → NotificationEventListener (subscribed to EventPublisher)
```

- `start()` starts the notification workers; `close()` drains and stops them (`AutoCloseable`).
- Everything takes a `java.time.Clock`: production passes `Clock.systemDefaultZone()`, and tests pass a fixed or movable clock to get deterministic dates.

Spring's `ApplicationContext` automates exactly this in Phase 3. Having written it by hand makes `@Autowired` much less magical.

## `DemoData`

Runs at backend startup (unless `demo.data.enabled=false`):

- companies: Acme Tech (Pune), Globex (Bengaluru), Initech (Hyderabad)
- users (password `password123`): admin, 2 recruiters, 3 candidates with profiles
- 5 jobs across the two companies
- 2 applications for *Java Backend Developer*, inserted **directly through the repository** so recruiter screens have data before `apply()` (TODO #8) is implemented

## Entry points

| Class | Run with | Purpose |
|-------|----------|---------|
| `api.ApiServer` | `mvnw compile exec:java` (default) | REST API for the frontend |
| `Main` | `mvnw compile exec:java -Dexec.mainClass=com.jobportal.Main` | console walkthrough; reports remaining TODOs |

The default main class is the `exec.mainClass` property in `pom.xml`.

## Logging

SLF4J API with Logback. Configured in `logback.xml`:

```
2026-10-02 10:20:12 INFO  [main] ApplicationService - Application 4 moved APPLIED -> SHORTLISTED
```

- `com.jobportal` logs at **INFO**; libraries at WARN. Set the `com.jobportal` logger to `DEBUG` for more detail (e.g. every published event).
- Conventions: INFO for business events (applied, shortlisted, job created), WARN for suspicious activity (failed logins, blocked users), ERROR for failures (interview conflicts, delivery errors, unhandled exceptions).
- Every HTTP request is logged by `ApiHttpHandler`: `GET /api/jobs -> 200 (3 ms)`.
- Never log passwords or tokens.
- Tests use the same config; adding `src/test/resources/logback-test.xml` with level WARN quiets them.

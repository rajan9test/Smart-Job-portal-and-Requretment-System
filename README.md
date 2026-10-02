# Smart Job Portal & Recruitment System

A simplified Naukri / LinkedIn Jobs clone: candidates search and apply for jobs, recruiters post jobs, rank applicants and schedule interviews, and admins manage the platform.

| Part | Tech |
|------|------|
| `backend/` | Java 21, plain Java business logic (no framework), REST API on the JDK's built-in HTTP server, Jackson, SLF4J + Logback, JUnit 5 + Mockito, Maven |
| `frontend/` | React 18, React Router 6, Vite 6 |

Data is kept **in memory** and reset on every backend restart. Sample users and jobs are loaded on startup.

> **Learning project.** Some core business rules are intentionally left as `TODO(#n)` exercises in the backend. Until each one is implemented, the related API endpoint returns **HTTP 501**, and the UI shows a yellow *"Not implemented yet: TODO(#n)"* box instead of crashing. See [Exercises](#exercises).

---

## 1. Prerequisites

| Tool | Version | Check with |
|------|---------|-----------|
| JDK | **21** or newer | `java -version` |
| Node.js | **18** or newer (20+ recommended) | `node -v` |
| npm | comes with Node | `npm -v` |
| Git | any | `git --version` |

You do **not** need to install Maven: the project ships with the Maven Wrapper (`mvnw` / `mvnw.cmd`), which downloads the right Maven version on first run.

No database is needed yet; PostgreSQL arrives in Phase 2 (see [Roadmap](#roadmap)).

## 2. Get the code

```bash
git clone <your-repo-url> smart-job-portal
cd smart-job-portal
```

## 3. Start the backend (port 8080)

```bash
cd backend

# Windows (PowerShell or cmd)
.\mvnw.cmd compile exec:java

# macOS / Linux / Git Bash
./mvnw compile exec:java
```

The first run downloads Maven and the dependencies, which takes a minute or two. When it's ready you'll see:

```
INFO  DemoData - Demo data loaded: 6 users, 3 companies, 5 jobs (password for every account: password123)
INFO  ApiServer - API listening on http://localhost:8080/api (CORS origin http://localhost:5173)
```

Check it: open <http://localhost:8080/api/health> and you should get `{"status":"UP"}`.

Leave this terminal running. Stop with `Ctrl+C`.

## 4. Start the frontend (port 5173)

In a **second terminal**:

```bash
cd frontend
npm install        # first time only
npm run dev
```

Open <http://localhost:5173>.

The Vite dev server forwards every `/api/...` request to `http://localhost:8080`, so the backend must be running.

## 5. Log in with a demo account

Every account's password is **`password123`**. The login page also has one-click buttons for these.

| Role | Email | Notes |
|------|-------|-------|
| Candidate | `rajan@mail.dev` | Java, Spring Boot, SQL, React · 2 yrs · has applied to *Java Backend Developer* |
| Candidate | `priya@mail.dev` | Java, Spring Boot, SQL, Docker, AWS · 4 yrs · has applied to *Java Backend Developer* |
| Candidate | `karan@mail.dev` | Python, SQL, React · 1 yr |
| Recruiter | `amit@acme.dev` | Acme Tech: 3 jobs |
| Recruiter | `sara@globex.dev` | Globex: 2 jobs |
| Admin | `admin@portal.dev` | |

You can also sign up as a new candidate or recruiter from the UI.

## 6. Run the tests

```bash
cd backend
.\mvnw.cmd test                         # Windows
./mvnw test                             # macOS / Linux

.\mvnw.cmd test -Dtest=TimeSlotTest     # a single test class
```

On a fresh checkout **many tests fail on purpose**: they are the tests for the unfinished exercises. Infrastructure tests (auth, password hashing, notification threads, experience matching) pass from the start.

## 7. Console demo (optional)

A text-only walkthrough of the whole hiring flow, with no server or UI:

```bash
cd backend
.\mvnw.cmd -q compile exec:java "-Dexec.mainClass=com.jobportal.Main"     # Windows
./mvnw -q compile exec:java -Dexec.mainClass=com.jobportal.Main           # macOS / Linux
```

It prints each step and reports which ones are still blocked by a TODO.

## 8. Build the frontend for production (optional)

```bash
cd frontend
npm run build      # outputs to frontend/dist
npm run preview    # serves the build on http://localhost:4173, with the same /api proxy
```

---

## Project structure

```
.
├── backend/                       Java project (Maven)
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd, .mvn/      Maven Wrapper
│   └── src/
│       ├── main/java/com/jobportal/
│       │   ├── model/             domain classes (User, Candidate, Job, Application, Interview, …)
│       │   ├── exception/         custom exceptions
│       │   ├── repository/        repository interfaces + in-memory implementations
│       │   ├── security/          password hashing, sessions
│       │   ├── service/           business logic (Auth, Job, Application, Interview, Matching, Admin, …)
│       │   ├── matching/          candidate-matching strategies
│       │   ├── event/             domain events (observer pattern)
│       │   ├── notification/      email / SMS / in-app delivery on background threads
│       │   ├── config/            ConfigurationManager (reads application.properties)
│       │   ├── api/               REST layer: router, controllers, JSON DTOs, ApiServer (main)
│       │   ├── PortalApplication  wires every object together
│       │   ├── DemoData           sample data loaded at startup
│       │   └── Main               console demo
│       ├── main/resources/        application.properties, logback.xml
│       └── test/java/             JUnit + Mockito tests
├── frontend/                      React app (Vite)
│   ├── vite.config.js             dev server + /api proxy
│   └── src/
│       ├── api/client.js          fetch wrapper (token, errors)
│       ├── auth/AuthContext.jsx   logged-in user state
│       ├── components/            layout, guards, badges, error box, …
│       ├── hooks/useApi.js        data loading / action helpers
│       └── pages/                 candidate/, recruiter/, admin/, shared pages
└── docs/                          per-module documentation
```

## Documentation

Each backend module and the frontend has its own page in [`docs/`](docs/README.md):

| Module | Doc |
|--------|-----|
| Domain model | [docs/modules/01-domain-model.md](docs/modules/01-domain-model.md) |
| Exceptions | [docs/modules/02-exceptions.md](docs/modules/02-exceptions.md) |
| Repositories | [docs/modules/03-repositories.md](docs/modules/03-repositories.md) |
| Users, authentication & authorization | [docs/modules/04-users-and-auth.md](docs/modules/04-users-and-auth.md) |
| Jobs & search | [docs/modules/05-jobs.md](docs/modules/05-jobs.md) |
| Applications | [docs/modules/06-applications.md](docs/modules/06-applications.md) |
| Interviews | [docs/modules/07-interviews.md](docs/modules/07-interviews.md) |
| Matching & ranking | [docs/modules/08-matching.md](docs/modules/08-matching.md) |
| Events & notifications | [docs/modules/09-notifications-and-events.md](docs/modules/09-notifications-and-events.md) |
| Admin | [docs/modules/10-admin.md](docs/modules/10-admin.md) |
| Configuration, wiring & logging | [docs/modules/11-configuration.md](docs/modules/11-configuration.md) |
| REST API | [docs/modules/12-rest-api.md](docs/modules/12-rest-api.md) |
| Frontend | [docs/modules/13-frontend.md](docs/modules/13-frontend.md) |

## Configuration

`backend/src/main/resources/application.properties`:

| Key | Default | Meaning |
|-----|---------|---------|
| `server.port` | `8080` | API port |
| `server.cors.origin` | `http://localhost:5173` | Browser origin allowed to call the API directly |
| `demo.data.enabled` | `true` | Load sample users/jobs at startup |
| `auth.session.ttl.minutes` | `60` | How long a login lasts |
| `auth.password.min.length` | `8` | Minimum password length |
| `auth.pbkdf2.iterations` | `210000` | Password hashing cost |
| `notification.worker.threads` | `2` | Background notification threads |
| `notification.channels` | `IN_APP,EMAIL` | Channels each notification is sent on |

If you change `server.port`, also change the proxy target in `frontend/vite.config.js`.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| UI says *"Cannot reach the backend"* | Start the backend (step 3) and check <http://localhost:8080/api/health>. |
| Yellow box *"Not implemented yet: TODO(#n)"* | Expected: that backend feature is an exercise. Implement it, then restart the backend. |
| Logged out after restarting the backend | Expected: sessions and data are in memory. Log in again. |
| `Address already in use` / `BindException` on 8080 | Another process uses the port. Stop it, or change `server.port` (and the Vite proxy). |
| `Port 5173 is in use` | Stop the other Vite instance, or run `npm run dev -- --port 5174` and set `server.cors.origin` accordingly. |
| `mvnw: Permission denied` (macOS/Linux) | `chmod +x mvnw` |
| `release version 21 not supported` | Your `JAVA_HOME` points to an older JDK. Install JDK 21 and point `JAVA_HOME` at it. |
| PowerShell splits `-Dexec.mainClass=…` | Wrap it in quotes: `"-Dexec.mainClass=com.jobportal.Main"`. |

---

## Exercises

The plumbing is done (models, repositories, auth, notifications, REST API, UI). The **interesting business logic is left as `TODO(#n)` stubs** that throw `UnsupportedOperationException`. Each has hints in its Javadoc and failing tests waiting for it. List them with `grep -rn "TODO(#" backend/src/main`.

Work in this order, since later ones build on earlier ones:

| # | Where (`backend/src/main/java/com/jobportal/…`) | What you practise | Test to make green | Unlocks in the UI |
|---|-------|-------------------|--------------------|-------------------|
| 1 | `model/TimeSlot.overlaps` | interval overlap logic | `TimeSlotTest` | (used by #9) |
| 2 | `model/ApplicationStatus.canTransitionTo` | enums, switch, state machines | `ApplicationStatusTest` | status changes, withdraw |
| 3 | `matching/SkillMatchingStrategy` | HashSet, streams | `MatchingStrategiesTest` | ranking |
| 4 | `matching/SalaryMatchingStrategy` | arithmetic edge cases | `MatchingStrategiesTest` | ranking |
| 5 | `service/MatchingService.rankCandidates` | Comparator chaining | `MatchingServiceTest` | ranking |
| 6 | `service/MatchingService.topCandidates` | PriorityQueue, top-K | `MatchingServiceTest` | |
| 7 | `service/AccessPolicy.canModifyJob` | authorization rules, `Long` equality | `JobServiceTest$Ownership` | every recruiter action |
| 8 | `service/ApplicationService.apply` | business rules, exceptions, events | `ApplicationServiceTest` | Apply button |
| 9 | `service/InterviewService.ensureNoConflict` | combining #1 with queries | `InterviewServiceTest` | scheduling interviews |
| 10 | `service/JobService.search` | Predicates, sorting, pagination | `JobServiceTest$Search` | Find jobs page |
| 11 | `service/AdminService.getStatistics` | groupingBy, counting, max | `AdminServiceTest` | admin stats |

Don't change a test to make it pass unless you've decided the spec in its Javadoc is wrong, and if so, write down why.

## Roadmap

- [x] REST API + React UI on top of the core
- [ ] **Phase 1 – Core Java**: finish TODO #1–#11 until `mvnw test` is fully green
- [ ] **Phase 2 – Database**: PostgreSQL schema, `Jdbc*Repository` implementations, transactions, indexes
- [ ] **Phase 3 – Spring Boot**: replace `api/` with `@RestController`s, `@ControllerAdvice`, validation, Spring Data JPA
- [ ] **Phase 4 – Security**: Spring Security, JWT instead of sessions, BCrypt
- [ ] **Phase 5 – Advanced**: caching, async notifications with retries, metrics
- [ ] **Phase 6 – Production**: Docker, Redis, Kafka/RabbitMQ, Swagger/OpenAPI, CI

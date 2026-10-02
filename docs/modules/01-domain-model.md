# 01 · Domain model

**Package:** `com.jobportal.model`

The plain Java objects that represent the business: users, companies, jobs, applications, interviews and notifications. They hold data and enforce their own invariants (e.g. a salary range can't be negative), but they never talk to repositories, services or HTTP.

## Class map

```
Identifiable  (getId / setId — anything a repository can store)
   │
   ├── User  (abstract)                     ── Role { CANDIDATE, RECRUITER, ADMIN }
   │     ├── Candidate   skills, experienceYears, education, resume, expectedSalary, location
   │     ├── Recruiter   companyId, designation
   │     └── Admin
   ├── Company           name, location, website
   ├── Job               title, companyId, recruiterId, requiredSkills, experience/salary range,
   │                     location, JobType, openings, deadline, JobStatus { OPEN, CLOSED }
   ├── Application       jobId, candidateId, ApplicationStatus, appliedAt, history[StatusChange]
   ├── Interview         applicationId, candidateId, jobId, interviewerEmail, TimeSlot,
   │                     InterviewType, InterviewStatus, InterviewFeedback
   └── Notification      recipientUserId, message, NotificationChannel, createdAt, read

Records (immutable values):  TimeSlot · InterviewFeedback · Resume · Application.StatusChange
```

## Key classes

### `User` and subclasses
- `User` is **abstract**: there is no "plain" user. Its fields are private (encapsulation), `role` is final and set by the subclass constructor.
- `showDashboard()` is abstract and each subclass returns its own text (polymorphism).
- Emails are trimmed and lower-cased in the constructor so login and uniqueness checks are case-insensitive.
- `equals`/`hashCode` are based on `id`, so two objects loaded for the same user compare equal.

### `Candidate`
- `skills` is a `HashSet<String>`, normalized to lower case through `util.Skills.normalize`, so `"Java"`, `" JAVA "` and `"java"` are the same skill. `getSkills()` returns an **unmodifiable** view; change skills with `setSkills` / `addSkill`.
- `expectedSalary` is **annual INR** (the UI converts to/from LPA).

### `Job`
- Ranges are set together with validation: `setExperienceRange(min, max)` and `setSalaryRange(min, max)` throw `IllegalArgumentException` if `max < min` or a value is negative.
- `isAcceptingApplications(today)`: OPEN **and** today is on or before the deadline (the deadline day itself still counts).
- `close()` / `reopen()` flip `JobStatus`.
- `companyId` and `recruiterId` are final: a job never moves to another company.

### `ApplicationStatus` (state machine)

```
APPLIED ──► SHORTLISTED ──► INTERVIEW ──► SELECTED
   │             │              │
   └─────────────┴──────────────┴──► REJECTED
   (candidate may WITHDRAW from any non-terminal state)
```

- `isTerminal()`: SELECTED, REJECTED and WITHDRAWN can't move anywhere.
- `canTransitionTo(next)` is **exercise TODO #2**.

### `Application`
- `changeStatus(next, at)` asks `ApplicationStatus.canTransitionTo` and throws `InvalidStatusTransitionException` if the move is illegal. Otherwise it updates the status and appends a `StatusChange(from, to, at)` to the history.
- This is the **only** way to change status, so the history can't get out of sync.

### `TimeSlot`
- A half-open interval `[start, end)`: 10:00–11:00 and 11:00–12:00 touch but do **not** overlap.
- The constructor rejects `end <= start`. `TimeSlot.of(start, duration)` is a convenience factory.
- `overlaps(other)` is **exercise TODO #1**.

### `Interview`
- Interviewers are identified by email (they don't need an account). The email is lower-cased.
- `isActive()` means `status == SCHEDULED`. Only active interviews count for conflict detection.

### `InterviewFeedback`
- A record whose compact constructor rejects ratings outside 1–5.

## Conventions

| Topic | Rule |
|-------|------|
| Money | `long`, annual INR |
| Time | `LocalDate` / `LocalDateTime`; services take "now" from an injected `java.time.Clock` |
| IDs | `Long`, assigned by the repository on first `save` |
| Collections exposed by getters | unmodifiable views |
| Validation | in constructors/setters, throwing `IllegalArgumentException` / `NullPointerException` |

## Extending

- **New user type** (e.g. `Interviewer`): add a `Role`, subclass `User`, implement `showDashboard()`, map it in `api/dto/Views.user`.
- **New application status** (e.g. `OFFERED`): add the constant, update `canTransitionTo` and `isTerminal`, and add cases to `ApplicationStatusTest`. `NotificationEventListener` picks it up automatically.

# 10 · Admin

**Class:** `service.AdminService`

Platform-wide management for users with the ADMIN role.

## Operations

| Method | Description |
|--------|-------------|
| `listUsers(admin)` | all users, ordered by id |
| `listUsersByRole(admin, role)` | filter by CANDIDATE / RECRUITER / ADMIN |
| `blockUser(admin, userId)` | sets `blocked = true`: the user can't log in and existing sessions stop working |
| `unblockUser(admin, userId)` | reverses it |
| `deleteUser(admin, userId)` | removes the user |
| `listJobs(admin)` / `listApplications(admin)` | everything on the platform |
| `getStatistics(admin)` | **exercise TODO #11**: returns `PlatformStats` |

Admins cannot block or delete **themselves** (`UnauthorizedException`); this stops a platform from locking out its last admin by accident.

## `PlatformStats`

| Field | Meaning |
|-------|---------|
| `totalUsers` | all users |
| `usersByRole` | `Map<Role, Long>` with **every** role present, 0 if none |
| `activeJobs` / `totalJobs` | OPEN jobs / all jobs |
| `totalApplications` | all applications |
| `applicationsByStatus` | `Map<ApplicationStatus, Long>` for statuses that occur |
| `mostAppliedJobTitle` | title of the job with the most applications; empty if there are none |

Collectors to use: `groupingBy` + `counting()`, `Map.Entry.comparingByValue()` with `max`, and an `EnumMap` pre-filled with zeros.

## Endpoints (admin only, everyone else gets 403)

| Method | Path |
|--------|------|
| GET | `/api/admin/users?role=RECRUITER` |
| POST | `/api/admin/users/{id}/block` · `/unblock` |
| DELETE | `/api/admin/users/{id}` |
| GET | `/api/admin/jobs` |
| GET | `/api/admin/applications` |
| GET | `/api/admin/stats` |

## Known gaps (good follow-up exercises)

- Deleting a user leaves their applications, jobs and interviews behind ("(deleted user)" in the UI). Decide: cascade, reassign, or soft-delete.
- There is no audit trail of admin actions beyond log lines. An event + listener would add one.

## Tests

`AdminServiceTest`: block/unblock, self-protection, statistics on an empty platform and on a populated one.

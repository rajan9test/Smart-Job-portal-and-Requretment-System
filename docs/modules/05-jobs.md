# 05 · Jobs & search

**Classes:** `service.JobService`, `service.dto.JobRequest`, `JobSearchCriteria`, `PageRequest`, `Page`

Recruiters create and manage jobs; everyone can search them.

## Job management

| Method | What it does | Guard |
|--------|--------------|-------|
| `createJob(recruiter, JobRequest)` | creates a job for the recruiter's company, status OPEN | recruiter must have a company |
| `getJob(id)` | | `JobNotFoundException` if missing |
| `updateJob(recruiter, id, JobRequest)` | overwrites every field | `AccessPolicy.requireCanModifyJob` |
| `closeJob` / `reopenJob` | flips `JobStatus` | same |
| `deleteJob` | removes the job | same |
| `getJobsForCompany(companyId)` | all jobs of a company | none (used by "My jobs") |

`JobRequest.applyTo(job)` copies the fields; `Job`'s setters do the range validation (`IllegalArgumentException` → HTTP 400).

> Open question left for you: what should happen to a deleted job's applications and interviews? Today they stay and show "(deleted job)". In Phase 2 this becomes a foreign-key decision (`ON DELETE CASCADE` vs soft delete).

## Search: `search(JobSearchCriteria, PageRequest)` (exercise TODO #10)

### Criteria (every field optional; `null` = don't filter)

| Field | Matches when |
|-------|--------------|
| `title` | case-insensitive substring of the job title |
| `companyName` | case-insensitive substring of the company's name |
| `location` | case-insensitive exact match |
| `experienceYears` | `job.minExperience ≤ years ≤ job.maxExperience` |
| `minSalary` / `maxSalary` | the job's salary range **overlaps** `[minSalary, maxSalary]` (either bound may be missing) |
| `skills` | the job requires **all** of these skills |
| `jobType` | exact match |
| `includeClosed` | closed jobs are excluded unless true |

Build it with `JobSearchCriteria.builder().title("java").location("Pune").build()`.

### Paging & sorting: `PageRequest`

- `page` is zero-based; `size` is 1–100 (`IllegalArgumentException` otherwise)
- sortable fields: `createdAt`, `salary` (by max salary), `experience` (by min experience), `title` (case-insensitive), each `ASC` or `DESC`
- `PageRequest.of(page, size, "salary,desc")` parses the REST-style parameter
- results must tie-break on job id so pages are stable

### Result: `Page<T>`

`content`, `page`, `size`, `totalElements` (counted **before** skipping), plus `totalPages()` and `hasNext()`.

## Endpoints

| Method | Path | Who | Notes |
|--------|------|-----|-------|
| GET | `/api/jobs` | public | query: `title, company, location, experience, minSalary, maxSalary, skills (comma-separated), jobType, includeClosed, page (0), size (10), sort (createdAt,desc)` |
| GET | `/api/jobs/{id}` | public | |
| GET | `/api/jobs/mine` | recruiter | all jobs of the recruiter's company, newest first |
| POST | `/api/jobs` | recruiter | 201 |
| PUT | `/api/jobs/{id}` | recruiter (owner) | |
| DELETE | `/api/jobs/{id}` | recruiter (owner) | 204 |
| POST | `/api/jobs/{id}/close` · `/reopen` | recruiter (owner) | |

Example: `GET /api/jobs?title=java&location=Pune&experience=2&minSalary=500000&sort=salary,desc&page=0&size=10`

## Tests

`JobServiceTest$Search` (filters, sorting, pagination edge cases) and `JobServiceTest$Ownership` (TODO #7: another company's recruiter can't modify a job; `Long` ids above 127 compared by value).

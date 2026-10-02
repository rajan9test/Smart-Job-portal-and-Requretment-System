# 08 · Matching & ranking

**Packages/classes:** `matching.*`, `service.MatchingService`

Scores how well each candidate fits a job and ranks the applicants, so a recruiter sees the best fits first.

## Strategy pattern

```java
@FunctionalInterface
public interface MatchingStrategy {
    double score(Candidate candidate, Job job);   // 0.0 – 100.0
}
```

| Strategy | Scoring | Status |
|----------|---------|--------|
| `SkillMatchingStrategy` | % of the job's required skills the candidate has; a job with no required skills → 100 | **TODO #3** |
| `ExperienceMatchingStrategy` | inside range → 100 · under-qualified: −25 per missing year (min 0) · over-qualified: −10 per extra year (min 50) | implemented (worked example) |
| `SalaryMatchingStrategy` | expected ≤ job max → 100 · above max: −1 point per 1% over (min 0) · undisclosed (max = 0) → 100 | **TODO #4** |
| `WeightedMatchingStrategy` | weighted average of other strategies; it is itself a strategy (composite) | implemented |

Example (skills):

```
Required:  java, spring boot, sql, docker, aws
Candidate: java, spring boot, sql, react
3 of 5 matched → 60.0
```

Skills on both sides are already lower-case `HashSet`s, so each lookup is O(1).

The default used by the UI and the console demo:

```java
new WeightedMatchingStrategy()
    .with(new SkillMatchingStrategy(), 0.6)
    .with(new ExperienceMatchingStrategy(), 0.3)
    .with(new SalaryMatchingStrategy(), 0.1);
```

## `MatchingService`

| Method | Description |
|--------|-------------|
| `rankCandidates(job, candidates, strategy)` | **TODO #5**: every candidate as a `MatchResult(candidate, score)`, sorted by score descending, ties broken by name ascending |
| `topCandidates(job, candidates, n, strategy)` | **TODO #6**: best `n`, best first, using a size-`n` **min-heap** (`PriorityQueue`): O(m log n) instead of sorting everything in O(m log m) |
| `rankApplicants(recruiter, jobId, strategy)` | loads the job's applicants (excluding WITHDRAWN and REJECTED), checks ownership, calls `rankCandidates` |

## Endpoint

`GET /api/jobs/{id}/ranking?strategy=weighted|skill|experience|salary` (recruiter, owner of the job) returns `[{candidate, score}]`, best first. Scores are rounded to one decimal.

## Adding a strategy

1. Implement `MatchingStrategy`, e.g. `LocationMatchingStrategy` (same city → 100, else 40).
2. Add a case to `JobController.strategy(...)` and an option in `frontend/src/pages/recruiter/ApplicantsPage.jsx`.
3. Optionally include it in the weighted default.

## Tests

`MatchingStrategiesTest` (each strategy, case-insensitivity, edge cases) and `MatchingServiceTest`. The service tests use a fake lambda strategy with fixed scores, so they don't depend on TODO #3/#4. They check ordering, tie-breaking, and that `topCandidates` agrees with the full ranking on 500 candidates.

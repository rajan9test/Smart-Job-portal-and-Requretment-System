# 07 · Interviews

**Classes:** `service.InterviewService`, `model.Interview`, `model.TimeSlot`, `model.InterviewFeedback`

Recruiters schedule interviews for shortlisted candidates, manage them, and record feedback.

## Lifecycle

```
schedule ──► SCHEDULED ──complete──► COMPLETED ──feedback──► (feedback stored once)
                │  ▲
                │  └── reschedule (stays SCHEDULED)
                └──cancel──► CANCELLED
```

## Operations

| Method | Rules |
|--------|-------|
| `schedule(recruiter, ScheduleInterviewRequest)` | recruiter owns the job · start is in the future · **no conflict** · application must be SHORTLISTED (moved to INTERVIEW) or already INTERVIEW (later rounds). Publishes `InterviewScheduled`. |
| `reschedule(recruiter, id, newStart, duration)` | interview must be SCHEDULED · future · no conflict, ignoring itself. Publishes `InterviewRescheduled`. |
| `cancel(recruiter, id, reason)` | must be SCHEDULED. Publishes `InterviewCancelled`. |
| `complete(recruiter, id)` | must be SCHEDULED |
| `submitFeedback(recruiter, id, rating, comments, recommended)` | must be COMPLETED · only once · rating 1–5 |
| `getInterviewsForCandidate(candidate)` | soonest first |
| `getInterviewsForRecruiter(recruiter)` | interviews for the recruiter's company's jobs, soonest first |

`ScheduleInterviewRequest` = `applicationId, interviewerEmail, start, duration, type` (`TECHNICAL`, `SYSTEM_DESIGN`, `MANAGERIAL`, `HR`).

## Conflict detection (exercises TODO #1 and #9)

Two time slots overlap when:

```
newStart < existingEnd  AND  newEnd > existingStart
```

Slots are half-open `[start, end)`, so back-to-back interviews (10–11, then 11–12) are allowed.

`ensureNoConflict(interviewerEmail, candidateId, slot, excludeInterviewId)` must reject a slot if:

- the **interviewer** has another SCHEDULED interview that overlaps, or
- the **candidate** has another SCHEDULED interview that overlaps.

Cancelled and completed interviews are ignored. When rescheduling, the interview being moved (`excludeInterviewId`) is ignored so it doesn't clash with its own old slot.

On conflict: `InterviewConflictException` (HTTP 409), logged at ERROR.

> Stretch: the scan is O(n) per call. A `TreeMap<LocalDateTime, Interview>` per interviewer lets you check only the neighbouring entries (`floorEntry` / `ceilingEntry`) in O(log n).

## Endpoints

| Method | Path | Body | Who |
|--------|------|------|-----|
| GET | `/api/interviews` | | candidate (own) or recruiter (company) |
| POST | `/api/interviews` | `{applicationId, interviewerEmail, start: "2026-10-03T11:00", durationMinutes, type}` | recruiter (201) |
| PUT | `/api/interviews/{id}` | `{start, durationMinutes}` | recruiter |
| POST | `/api/interviews/{id}/cancel` | `{reason}` | recruiter |
| POST | `/api/interviews/{id}/complete` | | recruiter |
| POST | `/api/interviews/{id}/feedback` | `{rating, comments, recommended}` | recruiter |

Times are local date-times without a time zone (`yyyy-MM-ddTHH:mm[:ss]`), interpreted in the server's zone.

## Tests

`TimeSlotTest` (overlap cases, including touching and identical slots) and `InterviewServiceTest` (interviewer/candidate double-booking, case-insensitive interviewer email, back-to-back, cancelled slots freed, rescheduling, past dates, feedback rules).

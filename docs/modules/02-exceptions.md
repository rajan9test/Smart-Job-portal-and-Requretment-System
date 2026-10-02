# 02 · Exceptions

**Package:** `com.jobportal.exception`

Business errors are expressed as custom **unchecked** exceptions. Services throw them; the REST layer translates them to HTTP status codes in a single place (`api/http/ExceptionMapper`).

## Hierarchy

```
RuntimeException
└── JobPortalException (abstract)
    ├── NotFoundException (abstract)            message: "<Entity> not found: <id>"
    │   ├── UserNotFoundException
    │   ├── JobNotFoundException
    │   ├── ApplicationNotFoundException
    │   ├── InterviewNotFoundException
    │   └── CompanyNotFoundException
    ├── InvalidCredentialsException             wrong email/password
    ├── UserBlockedException                    account blocked by an admin
    ├── UnauthorizedException                   logged in, but not allowed to do this
    ├── DuplicateEmailException                 email already registered
    ├── JobClosedException                      job closed or past its deadline
    ├── AlreadyAppliedException                 candidate already applied to this job
    ├── InvalidStatusTransitionException        illegal ApplicationStatus move
    └── InterviewConflictException              interviewer/candidate double-booked
```

## HTTP mapping

| Exception | Status |
|-----------|--------|
| any `NotFoundException` | 404 |
| `InvalidCredentialsException` | 401 |
| `UnauthorizedException`, `UserBlockedException` | 403 |
| `AlreadyAppliedException`, `DuplicateEmailException`, `InterviewConflictException`, `InvalidStatusTransitionException`, `JobClosedException`, `IllegalStateException` | 409 |
| `IllegalArgumentException` (validation) | 400 |
| `UnsupportedOperationException` (unfinished `TODO(#n)`) | 501 |
| anything else | 500 (message hidden from the client, full stack trace logged) |

See [12 · REST API](12-rest-api.md#error-format) for the JSON error body.

## Why unchecked?

- Service signatures stay readable: no `throws` clauses on every method.
- Callers that *can* recover catch the specific type; everything else bubbles up to the one central mapper.
- It matches how Spring handles exceptions, so Phase 3 maps 1:1 onto `@RestControllerAdvice`.

## Guidelines

- Throw the **most specific** type. Use `IllegalArgumentException` for bad input and `IllegalStateException` for "valid request, wrong moment" (e.g. feedback on an interview that isn't completed).
- Messages are shown to users by the UI, so write them for humans: `"You already applied to Java Developer"` beats `"dup"`.
- Never put secrets or internal details in messages.
- Adding a new exception: extend `JobPortalException` (or `NotFoundException`), then add a `case` to `ExceptionMapper.statusFor` if it shouldn't be a 500.

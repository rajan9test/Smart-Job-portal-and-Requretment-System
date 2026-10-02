# 04 · Users, authentication & authorization

**Classes:** `security.PasswordHasher`, `security.Session`, `service.AuthService`, `service.ProfileService`, `service.AccessPolicy`

Covers how accounts are created, how a user proves who they are, and how the system decides what they may do.

## Registration

`AuthService.registerCandidate(name, email, phone, password)` and `registerRecruiter(…, companyId)`:

1. email must contain `@`; password must be at least `auth.password.min.length` (8) characters → else `IllegalArgumentException`
2. email must not exist yet (case-insensitive) → else `DuplicateEmailException`
3. recruiter's company must exist → else `CompanyNotFoundException`
4. the password is hashed (never stored raw) and the user saved

Admins can't self-register; `createAdmin` is used by `DemoData`.

## Password hashing: `PasswordHasher`

- **PBKDF2-HMAC-SHA256** from the JDK, 16-byte random salt, `auth.pbkdf2.iterations` rounds (210,000)
- stored format: `iterations:base64(salt):base64(hash)`, so the iteration count can be raised later and old hashes still verify
- verification uses `MessageDigest.isEqual` (constant-time) so response timing doesn't leak how close a guess was
- same password → different hash each time (because of the salt)

## Login and sessions

```
POST /api/auth/login {email, password}
   └─ AuthService.login
        ├─ user not found OR wrong password → InvalidCredentialsException (same message for both)
        ├─ user blocked                    → UserBlockedException
        └─ create Session(token, userId, role, expiresAt = now + ttl)
             token = 32 random bytes, URL-safe Base64
```

- Sessions live in a `ConcurrentHashMap<token, Session>` inside `AuthService`.
- Every protected request sends `Authorization: Bearer <token>`. `AuthService.authenticate(token)` rejects unknown, expired (`auth.session.ttl.minutes`) and blocked sessions, and returns the `User`.
- `logout(token)` removes the session.
- Restarting the backend clears all sessions.

> Phase 4 replaces `Session` with a signed **JWT** carrying the same claims (user id, role, expiry), and `PasswordHasher` with BCrypt.

## Authorization

There are two layers.

**1. Role checks** happen in the REST layer: `Request.candidate()`, `.recruiter()` and `.admin()` return the typed user or throw **403**. Service methods then take typed parameters (`apply(Candidate …)`, `createJob(Recruiter …)`), so the compiler stops a recruiter from reaching candidate-only logic.

**2. Ownership checks** happen in `AccessPolicy`:

| Method | Rule |
|--------|------|
| `requireRole(user, roles…)` | role must be one of `roles` |
| `canModifyJob(recruiter, job)` | **exercise TODO #7**: decide whether "same company" or "only the poster" may manage a job |
| `requireCanModifyJob(recruiter, job)` | throws `UnauthorizedException` if `canModifyJob` is false |

`requireCanModifyJob` guards every recruiter action on a job: edit, close, reopen, delete, view applicants, change status, ranking, interviews.

Candidates may only withdraw **their own** applications (checked in `ApplicationService.withdraw`), and users may only mark **their own** notifications read (`InboxService`).

## Profile: `ProfileService`

`updateCandidateProfile(candidate, CandidateProfileUpdate)` updates any non-null field: name, phone, skills, experienceYears, education, expectedSalary, location. Skills are normalized to lower case. This is the data the [matching engine](08-matching.md) scores.

## Endpoints

| Method | Path | Who |
|--------|------|-----|
| POST | `/api/auth/register` | public; returns a login (token + user) |
| POST | `/api/auth/login` | public |
| POST | `/api/auth/logout` | any |
| GET | `/api/auth/me` | logged in |
| PUT | `/api/profile` | candidate |
| GET | `/api/companies` | public (for the recruiter sign-up dropdown) |

## Tests

`AuthServiceTest` covers register/login, case-insensitive email, identical errors for unknown email vs wrong password, blocked users, session expiry (using a movable test `Clock`) and logout. `PasswordHasherTest` covers salting and verification.

# 13 · Frontend

**Folder:** `frontend/` · React 18 · React Router 6 · Vite 6 · plain CSS (no UI library)

A deliberately small single-page app that exercises every backend feature for all three roles.

## Running

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173 (backend must run on :8080)
npm run build      # production bundle in dist/
```

`vite.config.js` proxies `/api/*` to `http://localhost:8080`, so the code always calls relative URLs (`fetch('/api/jobs')`) and there's no CORS in development.

## Structure

```
src/
├── main.jsx                 mounts <App/> inside BrowserRouter + AuthProvider
├── App.jsx                  route table + role-based home redirect
├── styles.css               all styles; CSS variables with a dark-mode variant
├── api/client.js            fetch wrapper: token, JSON, ApiError(status, message, todo), queryString()
├── auth/AuthContext.jsx     user state, login / register / logout, session restore
├── hooks/useApi.js          useApi(load, deps) for loading · useAction() for button/form actions
├── utils/format.js          LPA ↔ rupees, dates, humanize("FULL_TIME"), parseList("a, b")
├── components/
│   ├── Layout.jsx           top bar (links per role, unread count) + <Outlet/>
│   ├── ProtectedRoute.jsx   redirects guests to /login and wrong roles to /
│   ├── ErrorMessage.jsx     red error box; yellow "Not implemented yet: TODO(#n)" box for 501s
│   ├── StatusBadge.jsx      coloured status pill
│   ├── SkillChips.jsx       skill chips, highlighting matches
│   └── Pagination.jsx
└── pages/
    ├── LoginPage.jsx        includes one-click demo accounts
    ├── RegisterPage.jsx     candidate or recruiter (company dropdown)
    ├── NotificationsPage.jsx
    ├── InterviewsPage.jsx   shared; recruiters get reschedule / complete / cancel / feedback
    ├── candidate/  JobSearchPage · MyApplicationsPage · ProfilePage
    ├── recruiter/  MyJobsPage · JobForm · ApplicantsPage (status changes, scheduling, ranking)
    └── admin/      AdminDashboardPage (stats + user management)
```

## Routes

| Path | Page | Access |
|------|------|--------|
| `/` | redirects: candidate → `/jobs`, recruiter → `/recruiter/jobs`, admin → `/admin`, guest → `/jobs` | all |
| `/login`, `/register` | auth | all |
| `/jobs` | job search (Apply button for candidates) | all |
| `/applications` | my applications (withdraw) | candidate |
| `/profile` | edit profile and skills | candidate |
| `/recruiter/jobs` | company jobs: post, edit, close/reopen, delete | recruiter |
| `/recruiter/jobs/:jobId` | applicants, status changes, schedule interview, ranking | recruiter |
| `/interviews` | interviews | candidate, recruiter |
| `/notifications` | inbox | logged in |
| `/admin` | statistics and users | admin |

## Key patterns

### Auth flow
1. `login()` / `register()` stores the token in `localStorage` (`sjp.token`) and the user in context.
2. On page load, `AuthProvider` calls `/auth/me` to restore the session (`ready` stays false until then).
3. If any call returns **401**, `client.js` dispatches `auth:expired` and the context logs the user out. This happens after a backend restart, since sessions are in memory.

Route guards are UX only: the backend checks every role and ownership rule itself.

### Data loading
```jsx
const jobs = useApi(() => api.get(`/jobs${query}`), [query]);   // { data, error, loading, reload }
const action = useAction();                                      // { run, error, busy }
await action.run(() => api.post(`/jobs/${id}/apply`));          // returns true/false
```

### TODO-aware errors
When the backend answers 501, `ErrorMessage` shows which exercise is missing (`TODO(#10)`), so a fresh checkout still renders every page meaningfully. Once the exercise is implemented and the backend restarted, the page simply works.

### Money and dates
The API uses annual rupees; forms and lists use **LPA** (`toLpa`, `fromLpa`). Date-times are sent as `datetime-local` values (`2026-10-03T11:00`), which the backend parses as local time.

## Where each feature needs a backend exercise

| Screen | Needs |
|--------|-------|
| Find jobs | #10 search |
| Apply | #8 apply |
| Recruiter actions on jobs/applicants | #7 ownership; status changes also #2 |
| Schedule / reschedule interview | #1, #2, #7, #9 |
| Ranking | #3, #4, #5, #7 |
| Admin statistics | #11 |
| Login, register, profile, "My jobs" list, notifications, admin users | work out of the box |

## Extending

- New page: create it under `pages/`, add a `<Route>` in `App.jsx` (wrap with `ProtectedRoute roles={[…]}`), and a link in `Layout.jsx`'s `LINKS`.
- New API call: use `api.get/post/put/del` from `client.js`; never call `fetch` directly, so tokens and errors stay consistent.
- Ideas: a job detail page, a resume upload once the backend supports files, form validation messages per field, React Query for caching, TypeScript.

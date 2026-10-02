import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, queryString } from '../../api/client.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { useAction, useApi } from '../../hooks/useApi.js';
import ErrorMessage from '../../components/ErrorMessage.jsx';
import Pagination from '../../components/Pagination.jsx';
import SkillChips from '../../components/SkillChips.jsx';
import StatusBadge from '../../components/StatusBadge.jsx';
import { formatDate, fromLpa, humanize, salaryRange } from '../../utils/format.js';

const EMPTY_FILTERS = { title: '', company: '', location: '', experience: '', minLpa: '', maxLpa: '', skills: '', jobType: '' };
const PAGE_SIZE = 10;

/** Public job search (GET /api/jobs). Logged-in candidates can apply from here. */
export default function JobSearchPage() {
  const { user } = useAuth();
  const isCandidate = user?.role === 'CANDIDATE';

  const [filters, setFilters] = useState(EMPTY_FILTERS);   // what's typed in the form
  const [applied, setApplied] = useState(EMPTY_FILTERS);   // what was last searched
  const [sort, setSort] = useState('createdAt,desc');
  const [page, setPage] = useState(0);

  const query = queryString({
    title: applied.title,
    company: applied.company,
    location: applied.location,
    experience: applied.experience,
    minSalary: fromLpa(applied.minLpa),
    maxSalary: fromLpa(applied.maxLpa),
    skills: applied.skills,
    jobType: applied.jobType,
    sort,
    page,
    size: PAGE_SIZE,
  });
  const jobs = useApi(() => api.get(`/jobs${query}`), [query]);

  // Which jobs this candidate already applied to, so the button can say so.
  const myApplications = useApi(() => (isCandidate ? api.get('/applications') : Promise.resolve([])), [isCandidate]);
  const appliedJobIds = new Set((myApplications.data ?? []).filter((a) => a.status !== 'WITHDRAWN').map((a) => a.jobId));

  const action = useAction();
  const [message, setMessage] = useState(null);

  const set = (field) => (e) => setFilters({ ...filters, [field]: e.target.value });

  const search = (e) => {
    e.preventDefault();
    setPage(0);
    setApplied(filters);
  };

  const reset = () => {
    setFilters(EMPTY_FILTERS);
    setApplied(EMPTY_FILTERS);
    setPage(0);
  };

  const apply = async (job) => {
    setMessage(null);
    const ok = await action.run(() => api.post(`/jobs/${job.id}/apply`));
    if (ok) {
      setMessage(`Applied to ${job.title}. The recruiter has been notified.`);
      myApplications.reload();
    }
  };

  return (
    <>
      <h1>Find jobs</h1>

      <form className="card filters" onSubmit={search}>
        <input placeholder="Job title (e.g. Java)" value={filters.title} onChange={set('title')} />
        <input placeholder="Company" value={filters.company} onChange={set('company')} />
        <input placeholder="Location" value={filters.location} onChange={set('location')} />
        <input placeholder="Your experience (yrs)" type="number" min="0" value={filters.experience} onChange={set('experience')} />
        <input placeholder="Min salary (LPA)" type="number" min="0" step="0.5" value={filters.minLpa} onChange={set('minLpa')} />
        <input placeholder="Max salary (LPA)" type="number" min="0" step="0.5" value={filters.maxLpa} onChange={set('maxLpa')} />
        <input placeholder="Skills, comma separated" value={filters.skills} onChange={set('skills')} />
        <select value={filters.jobType} onChange={set('jobType')}>
          <option value="">Any job type</option>
          {['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP'].map((t) => (
            <option key={t} value={t}>{humanize(t)}</option>
          ))}
        </select>
        <div className="filter-actions">
          <button className="btn btn-primary">Search</button>
          <button type="button" className="btn btn-secondary" onClick={reset}>Reset</button>
        </div>
      </form>

      <div className="results-bar">
        <span className="muted">{jobs.data ? `${jobs.data.totalElements} job(s) found` : ''}</span>
        <label className="inline">
          Sort by
          <select value={sort} onChange={(e) => { setSort(e.target.value); setPage(0); }}>
            <option value="createdAt,desc">Newest</option>
            <option value="salary,desc">Salary: high to low</option>
            <option value="salary,asc">Salary: low to high</option>
            <option value="experience,asc">Experience: low to high</option>
            <option value="title,asc">Title A–Z</option>
          </select>
        </label>
      </div>

      {message && <div className="alert alert-success">{message}</div>}
      <ErrorMessage error={action.error} />
      <ErrorMessage error={jobs.error} />
      {jobs.loading && <p className="muted">Loading…</p>}
      {jobs.data?.content.length === 0 && <div className="card empty">No jobs match these filters.</div>}

      {jobs.data?.content.map((job) => (
        <article key={job.id} className="card job">
          <div className="job-head">
            <div>
              <h3>{job.title}</h3>
              <p className="muted">
                {job.companyName} · {job.location} · {humanize(job.jobType)}
              </p>
            </div>
            {job.status !== 'OPEN' && <StatusBadge status={job.status} />}
          </div>
          {job.description && <p>{job.description}</p>}
          <dl className="facts">
            <div><dt>Experience</dt><dd>{job.minExperience}–{job.maxExperience} yrs</dd></div>
            <div><dt>Salary</dt><dd>{salaryRange(job.minSalary, job.maxSalary)}</dd></div>
            <div><dt>Openings</dt><dd>{job.openings}</dd></div>
            <div><dt>Apply by</dt><dd>{formatDate(job.deadline)}</dd></div>
          </dl>
          <SkillChips skills={job.requiredSkills} highlight={user?.skills} />
          <div className="actions">
            {isCandidate && (
              appliedJobIds.has(job.id) ? (
                <span className="badge badge-applied">Applied</span>
              ) : (
                <button className="btn btn-primary" disabled={!job.acceptingApplications || action.busy} onClick={() => apply(job)}>
                  {job.acceptingApplications ? 'Apply' : 'Applications closed'}
                </button>
              )
            )}
            {!user && <Link to="/login" className="btn btn-secondary">Log in to apply</Link>}
          </div>
        </article>
      ))}

      <Pagination page={page} totalPages={jobs.data?.totalPages} onChange={setPage} />
    </>
  );
}

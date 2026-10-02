import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { useAction, useApi } from '../../hooks/useApi.js';
import ErrorMessage from '../../components/ErrorMessage.jsx';
import StatusBadge from '../../components/StatusBadge.jsx';
import JobForm from './JobForm.jsx';
import { formatDate, humanize, salaryRange } from '../../utils/format.js';

/** All jobs of the recruiter's company, with create / edit / close / reopen / delete. */
export default function MyJobsPage() {
  const { user } = useAuth();
  const jobs = useApi(() => api.get('/jobs/mine'), []);
  const action = useAction();
  const [editing, setEditing] = useState(null); // null = closed, 'new' = create form, or a job object

  const act = (fn) => action.run(async () => {
    await fn();
    setEditing(null);
    jobs.reload();
  });

  const save = (body) => act(() => (editing === 'new' ? api.post('/jobs', body) : api.put(`/jobs/${editing.id}`, body)));

  const remove = (job) => {
    if (window.confirm(`Delete "${job.title}"? This cannot be undone.`)) {
      act(() => api.del(`/jobs/${job.id}`));
    }
  };

  const list = jobs.data ?? [];

  return (
    <>
      <div className="page-header">
        <div>
          <h1>My jobs</h1>
          <p className="muted">{user.companyName}</p>
        </div>
        {!editing && (
          <button className="btn btn-primary" onClick={() => setEditing('new')}>
            + Post a job
          </button>
        )}
      </div>

      <ErrorMessage error={jobs.error ?? action.error} />

      {editing && (
        <JobForm
          key={editing === 'new' ? 'new' : editing.id}
          job={editing === 'new' ? null : editing}
          busy={action.busy}
          onSubmit={save}
          onCancel={() => setEditing(null)}
        />
      )}

      {jobs.loading && <p className="muted">Loading…</p>}
      {!jobs.loading && list.length === 0 && <div className="card empty">No jobs posted yet.</div>}

      {list.length > 0 && (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr>
                <th>Job</th>
                <th>Experience</th>
                <th>Salary</th>
                <th>Deadline</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {list.map((job) => (
                <tr key={job.id}>
                  <td>
                    <Link to={`/recruiter/jobs/${job.id}`}><strong>{job.title}</strong></Link>
                    <div className="muted small">{job.location} · {humanize(job.jobType)}</div>
                  </td>
                  <td>{job.minExperience}–{job.maxExperience} yrs</td>
                  <td>{salaryRange(job.minSalary, job.maxSalary)}</td>
                  <td>{formatDate(job.deadline)}</td>
                  <td><StatusBadge status={job.status} /></td>
                  <td className="actions">
                    <Link className="btn btn-primary btn-sm" to={`/recruiter/jobs/${job.id}`}>Applicants</Link>
                    <button className="btn btn-secondary btn-sm" onClick={() => setEditing(job)}>Edit</button>
                    {job.status === 'OPEN' ? (
                      <button className="btn btn-secondary btn-sm" onClick={() => act(() => api.post(`/jobs/${job.id}/close`))}>Close</button>
                    ) : (
                      <button className="btn btn-secondary btn-sm" onClick={() => act(() => api.post(`/jobs/${job.id}/reopen`))}>Reopen</button>
                    )}
                    <button className="btn btn-danger btn-sm" onClick={() => remove(job)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../api/client.js';
import { useAction, useApi } from '../../hooks/useApi.js';
import ErrorMessage from '../../components/ErrorMessage.jsx';
import SkillChips from '../../components/SkillChips.jsx';
import StatusBadge from '../../components/StatusBadge.jsx';
import { formatDate, humanize, salaryRange, toLpa } from '../../utils/format.js';

// What the recruiter can move an application to. The backend's state machine (TODO #2) decides
// which moves are actually legal and answers 409 otherwise.
const RECRUITER_STATUSES = ['SHORTLISTED', 'INTERVIEW', 'SELECTED', 'REJECTED'];
const CAN_INTERVIEW = ['SHORTLISTED', 'INTERVIEW'];

export default function ApplicantsPage() {
  const { jobId } = useParams();
  const job = useApi(() => api.get(`/jobs/${jobId}`), [jobId]);
  const applicants = useApi(() => api.get(`/jobs/${jobId}/applicants`), [jobId]);
  const action = useAction();
  const [scheduling, setScheduling] = useState(null); // application being scheduled
  const [notice, setNotice] = useState(null);

  const changeStatus = (application, status) => action.run(async () => {
    setNotice(null);
    await api.put(`/applications/${application.id}/status`, { status });
    setNotice(`${application.candidate?.name} moved to ${humanize(status)}.`);
    applicants.reload();
  });

  const schedule = (body) => action.run(async () => {
    setNotice(null);
    await api.post('/interviews', body);
    setNotice('Interview scheduled. The candidate has been notified.');
    setScheduling(null);
    applicants.reload();
  });

  const list = applicants.data ?? [];

  return (
    <>
      <Link to="/recruiter/jobs" className="muted small">← Back to my jobs</Link>
      {job.data && (
        <div className="page-header">
          <div>
            <h1>{job.data.title}</h1>
            <p className="muted">
              {job.data.location} · {job.data.minExperience}–{job.data.maxExperience} yrs ·{' '}
              {salaryRange(job.data.minSalary, job.data.maxSalary)} · <StatusBadge status={job.data.status} />
            </p>
            <SkillChips skills={job.data.requiredSkills} />
          </div>
        </div>
      )}
      <ErrorMessage error={job.error} />

      {notice && <div className="alert alert-success">{notice}</div>}
      <ErrorMessage error={action.error} />

      <h2>Applicants</h2>
      <ErrorMessage error={applicants.error} />
      {applicants.loading && <p className="muted">Loading…</p>}
      {!applicants.loading && !applicants.error && list.length === 0 && <div className="card empty">No applications yet.</div>}

      {list.length > 0 && (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr>
                <th>Candidate</th>
                <th>Skills</th>
                <th>Exp.</th>
                <th>Expects</th>
                <th>Applied</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {list.map((a) => (
                <tr key={a.id}>
                  <td>
                    <strong>{a.candidate?.name}</strong>
                    <div className="muted small">{a.candidate?.email}</div>
                    <div className="muted small">{a.candidate?.education}</div>
                  </td>
                  <td><SkillChips skills={a.candidate?.skills} highlight={job.data?.requiredSkills} /></td>
                  <td>{a.candidate?.experienceYears} yrs</td>
                  <td>{toLpa(a.candidate?.expectedSalary)} LPA</td>
                  <td>{formatDate(a.appliedAt)}</td>
                  <td><StatusBadge status={a.status} /></td>
                  <td className="actions">
                    <select
                      value=""
                      disabled={action.busy}
                      onChange={(e) => e.target.value && changeStatus(a, e.target.value)}
                      aria-label="Change status"
                    >
                      <option value="">Move to…</option>
                      {RECRUITER_STATUSES.filter((s) => s !== a.status).map((s) => (
                        <option key={s} value={s}>{humanize(s)}</option>
                      ))}
                    </select>
                    {CAN_INTERVIEW.includes(a.status) && (
                      <button className="btn btn-secondary btn-sm" onClick={() => setScheduling(a)}>
                        Schedule interview
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {scheduling && (
        <ScheduleForm application={scheduling} busy={action.busy} onSubmit={schedule} onCancel={() => setScheduling(null)} />
      )}

      <RankingPanel jobId={jobId} />
    </>
  );
}

function ScheduleForm({ application, busy, onSubmit, onCancel }) {
  const tomorrow11 = new Date(Date.now() + 86_400_000);
  tomorrow11.setHours(11, 0, 0, 0);
  const local = new Date(tomorrow11.getTime() - tomorrow11.getTimezoneOffset() * 60000).toISOString().slice(0, 16);

  const [form, setForm] = useState({ interviewerEmail: '', start: local, durationMinutes: 60, type: 'TECHNICAL' });
  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  return (
    <form
      className="card form form-wide"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit({ ...form, applicationId: application.id, durationMinutes: Number(form.durationMinutes) });
      }}
    >
      <h2>Schedule interview: {application.candidate?.name}</h2>
      <div className="grid-2">
        <label>
          Interviewer email
          <input type="email" value={form.interviewerEmail} onChange={set('interviewerEmail')} required placeholder="interviewer@acme.dev" />
        </label>
        <label>
          Type
          <select value={form.type} onChange={set('type')}>
            {['TECHNICAL', 'SYSTEM_DESIGN', 'MANAGERIAL', 'HR'].map((t) => (
              <option key={t} value={t}>{humanize(t)}</option>
            ))}
          </select>
        </label>
        <label>
          Start
          <input type="datetime-local" value={form.start} onChange={set('start')} required />
        </label>
        <label>
          Duration (minutes)
          <input type="number" min="15" step="15" value={form.durationMinutes} onChange={set('durationMinutes')} required />
        </label>
      </div>
      <div className="actions">
        <button className="btn btn-primary" disabled={busy}>Schedule</button>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}

/** Ranks active applicants with a chosen matching strategy (GET /api/jobs/{id}/ranking). */
function RankingPanel({ jobId }) {
  const [strategy, setStrategy] = useState('weighted');
  const [requested, setRequested] = useState(null);
  const ranking = useApi(
    () => (requested ? api.get(`/jobs/${jobId}/ranking?strategy=${requested}`) : Promise.resolve(null)),
    [jobId, requested],
  );

  return (
    <section className="card">
      <div className="page-header">
        <h2>Candidate ranking</h2>
        <div className="inline">
          <select value={strategy} onChange={(e) => setStrategy(e.target.value)}>
            <option value="weighted">Weighted (skills 60%, experience 30%, salary 10%)</option>
            <option value="skill">Skills only</option>
            <option value="experience">Experience only</option>
            <option value="salary">Salary only</option>
          </select>
          <button className="btn btn-primary" onClick={() => setRequested(strategy)}>Rank</button>
        </div>
      </div>
      <ErrorMessage error={ranking.error} />
      {requested && ranking.loading && <p className="muted">Ranking…</p>}
      {ranking.data?.length === 0 && <p className="muted">No active applicants to rank.</p>}
      <ol className="ranking">
        {(ranking.data ?? []).map((r) => (
          <li key={r.candidate.id}>
            <span className="rank-name">{r.candidate.name}</span>
            <span className="bar"><span style={{ width: `${r.score}%` }} /></span>
            <span className="rank-score">{r.score}%</span>
          </li>
        ))}
      </ol>
    </section>
  );
}

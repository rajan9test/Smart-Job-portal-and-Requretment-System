import { Link } from 'react-router-dom';
import { api } from '../../api/client.js';
import { useAction, useApi } from '../../hooks/useApi.js';
import ErrorMessage from '../../components/ErrorMessage.jsx';
import StatusBadge from '../../components/StatusBadge.jsx';
import { formatDate, humanize } from '../../utils/format.js';

const IN_PROGRESS = ['APPLIED', 'SHORTLISTED', 'INTERVIEW'];

export default function MyApplicationsPage() {
  const applications = useApi(() => api.get('/applications'), []);
  const action = useAction();

  const withdraw = (application) => {
    if (!window.confirm(`Withdraw your application for ${application.jobTitle}?`)) return;
    action.run(async () => {
      await api.post(`/applications/${application.id}/withdraw`);
      applications.reload();
    });
  };

  const list = applications.data ?? [];

  return (
    <>
      <h1>My applications</h1>
      <ErrorMessage error={applications.error ?? action.error} />
      {applications.loading && <p className="muted">Loading…</p>}
      {!applications.loading && list.length === 0 && (
        <div className="card empty">
          You haven't applied anywhere yet. <Link to="/jobs">Find jobs</Link>
        </div>
      )}

      {list.length > 0 && (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr>
                <th>Job</th>
                <th>Applied on</th>
                <th>Status</th>
                <th>History</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {list.map((a) => (
                <tr key={a.id}>
                  <td>
                    <strong>{a.jobTitle}</strong>
                    <div className="muted small">{a.companyName}</div>
                  </td>
                  <td>{formatDate(a.appliedAt)}</td>
                  <td><StatusBadge status={a.status} /></td>
                  <td className="small muted">
                    {['Applied', ...a.history.map((h) => humanize(h.to))].join(' → ')}
                  </td>
                  <td>
                    {IN_PROGRESS.includes(a.status) && (
                      <button className="btn btn-danger btn-sm" onClick={() => withdraw(a)} disabled={action.busy}>
                        Withdraw
                      </button>
                    )}
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

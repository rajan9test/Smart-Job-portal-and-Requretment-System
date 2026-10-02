import { useState } from 'react';
import { api } from '../../api/client.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { useAction, useApi } from '../../hooks/useApi.js';
import ErrorMessage from '../../components/ErrorMessage.jsx';
import StatusBadge from '../../components/StatusBadge.jsx';
import { formatDate, humanize } from '../../utils/format.js';

export default function AdminDashboardPage() {
  const { user: me } = useAuth();
  const stats = useApi(() => api.get('/admin/stats'), []);
  const [role, setRole] = useState('');
  const users = useApi(() => api.get(`/admin/users${role ? `?role=${role}` : ''}`), [role]);
  const action = useAction();

  const act = (fn) => action.run(async () => {
    await fn();
    users.reload();
    stats.reload();
  });

  const remove = (u) => {
    if (window.confirm(`Delete ${u.name} (${u.email})? This cannot be undone.`)) {
      act(() => api.del(`/admin/users/${u.id}`));
    }
  };

  const s = stats.data;

  return (
    <>
      <h1>Admin dashboard</h1>

      <h2>Platform statistics</h2>
      <ErrorMessage error={stats.error} />
      {s && (
        <>
          <div className="stats">
            <Stat label="Total users" value={s.totalUsers} />
            <Stat label="Candidates" value={s.usersByRole?.CANDIDATE ?? 0} />
            <Stat label="Recruiters" value={s.usersByRole?.RECRUITER ?? 0} />
            <Stat label="Active jobs" value={`${s.activeJobs} / ${s.totalJobs}`} />
            <Stat label="Applications" value={s.totalApplications} />
          </div>
          <div className="card">
            <p>
              <strong>Most applied job:</strong> {s.mostAppliedJobTitle ?? '—'}
            </p>
            <p>
              <strong>Applications by status:</strong>{' '}
              {Object.entries(s.applicationsByStatus ?? {}).length === 0
                ? '—'
                : Object.entries(s.applicationsByStatus).map(([status, count]) => (
                    <span key={status} className="stat-chip">
                      <StatusBadge status={status} /> {count}
                    </span>
                  ))}
            </p>
          </div>
        </>
      )}

      <div className="page-header">
        <h2>Users</h2>
        <select value={role} onChange={(e) => setRole(e.target.value)} aria-label="Filter by role">
          <option value="">All roles</option>
          <option value="CANDIDATE">Candidates</option>
          <option value="RECRUITER">Recruiters</option>
          <option value="ADMIN">Admins</option>
        </select>
      </div>
      <ErrorMessage error={users.error ?? action.error} />
      {users.data && (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Role</th>
                <th>Company</th>
                <th>Joined</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.data.map((u) => (
                <tr key={u.id}>
                  <td>
                    <strong>{u.name}</strong>
                    <div className="muted small">{u.email}</div>
                  </td>
                  <td>{humanize(u.role)}</td>
                  <td>{u.companyName ?? '—'}</td>
                  <td>{formatDate(u.createdAt)}</td>
                  <td>{u.blocked ? <span className="badge badge-rejected">Blocked</span> : <span className="badge badge-open">Active</span>}</td>
                  <td className="actions">
                    {u.id !== me.id && (
                      <>
                        {u.blocked ? (
                          <button className="btn btn-secondary btn-sm" onClick={() => act(() => api.post(`/admin/users/${u.id}/unblock`))}>Unblock</button>
                        ) : (
                          <button className="btn btn-secondary btn-sm" onClick={() => act(() => api.post(`/admin/users/${u.id}/block`))}>Block</button>
                        )}
                        <button className="btn btn-danger btn-sm" onClick={() => remove(u)}>Delete</button>
                      </>
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

function Stat({ label, value }) {
  return (
    <div className="card stat">
      <span className="stat-value">{value}</span>
      <span className="muted small">{label}</span>
    </div>
  );
}

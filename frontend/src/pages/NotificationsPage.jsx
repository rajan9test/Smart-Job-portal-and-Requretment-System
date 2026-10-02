import { api } from '../api/client.js';
import { useAction, useApi } from '../hooks/useApi.js';
import ErrorMessage from '../components/ErrorMessage.jsx';
import { formatDateTime } from '../utils/format.js';

export default function NotificationsPage() {
  const inbox = useApi(() => api.get('/notifications'), []);
  const action = useAction();

  const markRead = (id) => action.run(async () => {
    await api.post(`/notifications/${id}/read`);
    inbox.reload();
  });

  const markAllRead = () => action.run(async () => {
    await api.post('/notifications/read-all');
    inbox.reload();
  });

  const items = inbox.data?.items ?? [];

  return (
    <>
      <div className="page-header">
        <h1>Notifications</h1>
        {inbox.data?.unread > 0 && (
          <button className="btn btn-secondary" onClick={markAllRead} disabled={action.busy}>
            Mark all as read
          </button>
        )}
      </div>
      <ErrorMessage error={inbox.error ?? action.error} />
      {inbox.loading && <p className="muted">Loading…</p>}
      {!inbox.loading && items.length === 0 && (
        <div className="card empty">
          No notifications yet. You'll hear about applications, status changes and interviews here.
        </div>
      )}
      <ul className="notification-list">
        {items.map((n) => (
          <li key={n.id} className={`card notification ${n.read ? '' : 'unread'}`}>
            <div>
              <p>{n.message}</p>
              <span className="muted small">{formatDateTime(n.createdAt)}</span>
            </div>
            {!n.read && (
              <button className="btn btn-secondary btn-sm" onClick={() => markRead(n.id)}>
                Mark read
              </button>
            )}
          </li>
        ))}
      </ul>
    </>
  );
}

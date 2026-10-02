import { useState } from 'react';
import { api } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { useAction, useApi } from '../hooks/useApi.js';
import ErrorMessage from '../components/ErrorMessage.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import { formatDateTime, humanize, minutesBetween } from '../utils/format.js';

/** Candidates see their interviews; recruiters also reschedule, cancel, complete and leave feedback. */
export default function InterviewsPage() {
  const { user } = useAuth();
  const isRecruiter = user.role === 'RECRUITER';
  const interviews = useApi(() => api.get('/interviews'), []);
  const action = useAction();
  const [editing, setEditing] = useState(null); // { id, mode: 'reschedule' | 'feedback' }

  const act = (fn) => action.run(async () => {
    await fn();
    setEditing(null);
    interviews.reload();
  });

  const cancel = (interview) => {
    const reason = window.prompt('Reason for cancelling?', 'Position filled');
    if (reason === null) return;
    act(() => api.post(`/interviews/${interview.id}/cancel`, { reason }));
  };

  const list = interviews.data ?? [];

  return (
    <>
      <h1>Interviews</h1>
      <ErrorMessage error={interviews.error ?? action.error} />
      {interviews.loading && <p className="muted">Loading…</p>}
      {!interviews.loading && !interviews.error && list.length === 0 && (
        <div className="card empty">
          {isRecruiter
            ? 'No interviews yet. Schedule one from a job’s applicants page.'
            : 'No interviews scheduled yet.'}
        </div>
      )}

      {list.map((iv) => (
        <div key={iv.id} className="card interview">
          <div className="interview-main">
            <div>
              <h3>{iv.jobTitle}</h3>
              <p className="muted small">
                {isRecruiter && <>Candidate: <strong>{iv.candidateName}</strong> · </>}
                {humanize(iv.type)} with {iv.interviewerEmail}
              </p>
              <p>
                {formatDateTime(iv.start)} · {minutesBetween(iv.start, iv.end)} min
              </p>
            </div>
            <StatusBadge status={iv.status} />
          </div>

          {iv.feedback && (
            <div className="feedback">
              <strong>Feedback:</strong> {'★'.repeat(iv.feedback.rating)}{'☆'.repeat(5 - iv.feedback.rating)}{' '}
              {iv.feedback.recommended ? '· Recommended' : '· Not recommended'}
              {iv.feedback.comments && <p className="muted">{iv.feedback.comments}</p>}
            </div>
          )}

          {isRecruiter && (
            <div className="actions">
              {iv.status === 'SCHEDULED' && (
                <>
                  <button className="btn btn-secondary btn-sm" onClick={() => setEditing({ id: iv.id, mode: 'reschedule' })}>
                    Reschedule
                  </button>
                  <button className="btn btn-secondary btn-sm" onClick={() => act(() => api.post(`/interviews/${iv.id}/complete`))}>
                    Mark completed
                  </button>
                  <button className="btn btn-danger btn-sm" onClick={() => cancel(iv)}>
                    Cancel
                  </button>
                </>
              )}
              {iv.status === 'COMPLETED' && !iv.feedback && (
                <button className="btn btn-primary btn-sm" onClick={() => setEditing({ id: iv.id, mode: 'feedback' })}>
                  Add feedback
                </button>
              )}
            </div>
          )}

          {editing?.id === iv.id && editing.mode === 'reschedule' && (
            <RescheduleForm
              interview={iv}
              busy={action.busy}
              onCancel={() => setEditing(null)}
              onSubmit={(body) => act(() => api.put(`/interviews/${iv.id}`, body))}
            />
          )}
          {editing?.id === iv.id && editing.mode === 'feedback' && (
            <FeedbackForm
              busy={action.busy}
              onCancel={() => setEditing(null)}
              onSubmit={(body) => act(() => api.post(`/interviews/${iv.id}/feedback`, body))}
            />
          )}
        </div>
      ))}
    </>
  );
}

function RescheduleForm({ interview, busy, onSubmit, onCancel }) {
  const [start, setStart] = useState(interview.start.slice(0, 16));
  const [duration, setDuration] = useState(minutesBetween(interview.start, interview.end));
  return (
    <form
      className="inline-form"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit({ start, durationMinutes: Number(duration) });
      }}
    >
      <label>
        New start
        <input type="datetime-local" value={start} onChange={(e) => setStart(e.target.value)} required />
      </label>
      <label>
        Duration (min)
        <input type="number" min="15" step="15" value={duration} onChange={(e) => setDuration(e.target.value)} required />
      </label>
      <button className="btn btn-primary btn-sm" disabled={busy}>Save</button>
      <button type="button" className="btn btn-secondary btn-sm" onClick={onCancel}>Close</button>
    </form>
  );
}

function FeedbackForm({ busy, onSubmit, onCancel }) {
  const [rating, setRating] = useState(4);
  const [comments, setComments] = useState('');
  const [recommended, setRecommended] = useState(true);
  return (
    <form
      className="inline-form"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit({ rating: Number(rating), comments, recommended });
      }}
    >
      <label>
        Rating
        <select value={rating} onChange={(e) => setRating(e.target.value)}>
          {[5, 4, 3, 2, 1].map((r) => (
            <option key={r} value={r}>{r} / 5</option>
          ))}
        </select>
      </label>
      <label className="grow">
        Comments
        <input value={comments} onChange={(e) => setComments(e.target.value)} placeholder="Strong on Java, weaker on SQL…" />
      </label>
      <label className="checkbox">
        <input type="checkbox" checked={recommended} onChange={(e) => setRecommended(e.target.checked)} />
        Recommend
      </label>
      <button className="btn btn-primary btn-sm" disabled={busy}>Submit</button>
      <button type="button" className="btn btn-secondary btn-sm" onClick={onCancel}>Close</button>
    </form>
  );
}

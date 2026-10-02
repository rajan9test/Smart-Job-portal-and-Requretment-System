/**
 * Shows an API error. A 501 means the backend feature is an unfinished exercise (TODO in the Java
 * code), so it gets a friendlier explanation instead of looking like a crash.
 */
export default function ErrorMessage({ error }) {
  if (!error) return null;

  if (error.status === 501) {
    return (
      <div className="alert alert-todo" role="status">
        <strong>Not implemented yet{error.todo ? `: ${error.todo}` : ''}</strong>
        <p>{error.message}</p>
        <p className="muted small">
          Implement this in the Java backend, restart the server, and this screen will start working.
          See <code>docs/modules</code> for details.
        </p>
      </div>
    );
  }

  return (
    <div className="alert alert-error" role="alert">
      {error.message}
    </div>
  );
}

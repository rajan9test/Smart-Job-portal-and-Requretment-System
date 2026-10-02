import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

/**
 * Client-side guard for pages. This only improves UX: the backend enforces every role check
 * itself (401/403), so hiding a page here is never the security boundary.
 */
export default function ProtectedRoute({ roles, children }) {
  const { user, ready } = useAuth();
  const location = useLocation();

  if (!ready) return <p className="muted">Loading…</p>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

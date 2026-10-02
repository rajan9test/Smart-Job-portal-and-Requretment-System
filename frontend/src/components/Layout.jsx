import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { api } from '../api/client.js';
import { useApi } from '../hooks/useApi.js';

const LINKS = {
  GUEST: [{ to: '/jobs', label: 'Find jobs' }],
  CANDIDATE: [
    { to: '/jobs', label: 'Find jobs' },
    { to: '/applications', label: 'My applications' },
    { to: '/interviews', label: 'Interviews' },
    { to: '/profile', label: 'Profile' },
  ],
  RECRUITER: [
    { to: '/recruiter/jobs', label: 'My jobs' },
    { to: '/interviews', label: 'Interviews' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Dashboard' },
    { to: '/jobs', label: 'Browse jobs' },
  ],
};

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Refresh the unread count on every navigation.
  const inbox = useApi(() => (user ? api.get('/notifications') : Promise.resolve(null)), [user?.id, location.pathname]);
  const unread = inbox.data?.unread ?? 0;

  const links = LINKS[user?.role ?? 'GUEST'];

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand">
            Smart Job Portal
          </NavLink>
          <nav className="nav">
            {links.map((link) => (
              <NavLink key={link.to} to={link.to} className="nav-link">
                {link.label}
              </NavLink>
            ))}
            {user && (
              <NavLink to="/notifications" className="nav-link">
                Notifications{unread > 0 && <span className="count">{unread}</span>}
              </NavLink>
            )}
          </nav>
          <div className="session">
            {user ? (
              <>
                <span className="muted small">
                  {user.name} · {user.role.toLowerCase()}
                </span>
                <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
                  Log out
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login" className="nav-link">
                  Log in
                </NavLink>
                <NavLink to="/register" className="btn btn-primary btn-sm">
                  Sign up
                </NavLink>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  );
}

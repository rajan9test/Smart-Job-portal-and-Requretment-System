import { Link, Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import { useAuth } from './auth/AuthContext.jsx';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import NotificationsPage from './pages/NotificationsPage.jsx';
import InterviewsPage from './pages/InterviewsPage.jsx';
import JobSearchPage from './pages/candidate/JobSearchPage.jsx';
import MyApplicationsPage from './pages/candidate/MyApplicationsPage.jsx';
import ProfilePage from './pages/candidate/ProfilePage.jsx';
import MyJobsPage from './pages/recruiter/MyJobsPage.jsx';
import ApplicantsPage from './pages/recruiter/ApplicantsPage.jsx';
import AdminDashboardPage from './pages/admin/AdminDashboardPage.jsx';

/** Sends each role to its own landing page. */
function Home() {
  const { user, ready } = useAuth();
  if (!ready) return <p className="muted">Loading…</p>;
  if (!user) return <Navigate to="/jobs" replace />;
  const home = { CANDIDATE: '/jobs', RECRUITER: '/recruiter/jobs', ADMIN: '/admin' }[user.role];
  return <Navigate to={home} replace />;
}

function NotFound() {
  return (
    <div className="card">
      <h1>Page not found</h1>
      <Link to="/">Go home</Link>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/jobs" element={<JobSearchPage />} />

        <Route path="/applications" element={<ProtectedRoute roles={['CANDIDATE']}><MyApplicationsPage /></ProtectedRoute>} />
        <Route path="/profile" element={<ProtectedRoute roles={['CANDIDATE']}><ProfilePage /></ProtectedRoute>} />

        <Route path="/recruiter/jobs" element={<ProtectedRoute roles={['RECRUITER']}><MyJobsPage /></ProtectedRoute>} />
        <Route path="/recruiter/jobs/:jobId" element={<ProtectedRoute roles={['RECRUITER']}><ApplicantsPage /></ProtectedRoute>} />

        <Route path="/interviews" element={<ProtectedRoute roles={['CANDIDATE', 'RECRUITER']}><InterviewsPage /></ProtectedRoute>} />
        <Route path="/notifications" element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />

        <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage /></ProtectedRoute>} />

        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}

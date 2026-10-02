import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { useAction } from '../hooks/useApi.js';
import ErrorMessage from '../components/ErrorMessage.jsx';

// Accounts created by DemoData.java on every backend start.
const DEMO_ACCOUNTS = [
  { label: 'Candidate (Rajan)', email: 'rajan@mail.dev' },
  { label: 'Candidate (Priya)', email: 'priya@mail.dev' },
  { label: 'Recruiter (Amit, Acme)', email: 'amit@acme.dev' },
  { label: 'Recruiter (Sara, Globex)', email: 'sara@globex.dev' },
  { label: 'Admin', email: 'admin@portal.dev' },
];
const DEMO_PASSWORD = 'password123';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { run, error, busy } = useAction();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    const ok = await run(() => login(email, password));
    if (ok) navigate(location.state?.from ?? '/', { replace: true });
  };

  return (
    <div className="auth-page">
      <form className="card form" onSubmit={submit}>
        <h1>Log in</h1>
        <ErrorMessage error={error} />
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button className="btn btn-primary" disabled={busy}>
          {busy ? 'Logging in…' : 'Log in'}
        </button>
        <p className="muted small">
          No account? <Link to="/register">Sign up</Link>
        </p>
      </form>

      <div className="card demo-accounts">
        <h2>Demo accounts</h2>
        <p className="muted small">
          Password for all: <code>{DEMO_PASSWORD}</code>. Data resets when the backend restarts.
        </p>
        {DEMO_ACCOUNTS.map((account) => (
          <button
            key={account.email}
            type="button"
            className="btn btn-secondary btn-block"
            onClick={() => {
              setEmail(account.email);
              setPassword(DEMO_PASSWORD);
            }}
          >
            {account.label}
          </button>
        ))}
      </div>
    </div>
  );
}

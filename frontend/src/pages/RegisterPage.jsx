import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { api } from '../api/client.js';
import { useAction, useApi } from '../hooks/useApi.js';
import ErrorMessage from '../components/ErrorMessage.jsx';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const { run, error, busy } = useAction();
  const companies = useApi(() => api.get('/companies'), []);
  const [form, setForm] = useState({ role: 'CANDIDATE', name: '', email: '', phone: '', password: '', companyId: '' });

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const payload = { ...form, companyId: form.role === 'RECRUITER' ? Number(form.companyId) : null };
    const ok = await run(() => register(payload));
    if (ok) navigate('/', { replace: true });
  };

  return (
    <div className="auth-page">
      <form className="card form" onSubmit={submit}>
        <h1>Create an account</h1>
        <ErrorMessage error={error} />

        <fieldset className="role-picker">
          <legend>I am a</legend>
          {['CANDIDATE', 'RECRUITER'].map((role) => (
            <label key={role} className={`role-option ${form.role === role ? 'selected' : ''}`}>
              <input type="radio" name="role" value={role} checked={form.role === role} onChange={set('role')} />
              {role === 'CANDIDATE' ? 'Job seeker' : 'Recruiter'}
            </label>
          ))}
        </fieldset>

        <label>
          Full name
          <input value={form.name} onChange={set('name')} required />
        </label>
        <label>
          Email
          <input type="email" value={form.email} onChange={set('email')} required />
        </label>
        <label>
          Phone <span className="muted">(optional)</span>
          <input value={form.phone} onChange={set('phone')} />
        </label>
        <label>
          Password <span className="muted">(at least 8 characters)</span>
          <input type="password" value={form.password} onChange={set('password')} minLength={8} required />
        </label>

        {form.role === 'RECRUITER' && (
          <label>
            Company
            <select value={form.companyId} onChange={set('companyId')} required>
              <option value="">Select your company…</option>
              {(companies.data ?? []).map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.location})
                </option>
              ))}
            </select>
          </label>
        )}

        <button className="btn btn-primary" disabled={busy}>
          {busy ? 'Creating account…' : 'Sign up'}
        </button>
        <p className="muted small">
          Already registered? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  );
}

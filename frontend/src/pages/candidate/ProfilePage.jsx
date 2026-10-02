import { useState } from 'react';
import { api } from '../../api/client.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import { useAction } from '../../hooks/useApi.js';
import ErrorMessage from '../../components/ErrorMessage.jsx';
import SkillChips from '../../components/SkillChips.jsx';
import { fromLpa, parseList, toLpa } from '../../utils/format.js';

/** The candidate's profile is what the recruiter's matching engine scores against. */
export default function ProfilePage() {
  const { user, setUser } = useAuth();
  const { run, error, busy } = useAction();
  const [saved, setSaved] = useState(false);
  const [form, setForm] = useState({
    name: user.name ?? '',
    phone: user.phone ?? '',
    education: user.education ?? '',
    location: user.location ?? '',
    experienceYears: user.experienceYears ?? 0,
    expectedLpa: toLpa(user.expectedSalary ?? 0),
    skills: (user.skills ?? []).join(', '),
  });

  const set = (field) => (e) => {
    setSaved(false);
    setForm({ ...form, [field]: e.target.value });
  };

  const submit = async (e) => {
    e.preventDefault();
    await run(async () => {
      const updated = await api.put('/profile', {
        name: form.name,
        phone: form.phone,
        education: form.education,
        location: form.location,
        experienceYears: Number(form.experienceYears),
        expectedSalary: fromLpa(form.expectedLpa) ?? 0,
        skills: parseList(form.skills),
      });
      setUser(updated);
      setSaved(true);
    });
  };

  return (
    <>
      <h1>My profile</h1>
      <form className="card form form-wide" onSubmit={submit}>
        <ErrorMessage error={error} />
        {saved && <div className="alert alert-success">Profile saved.</div>}
        <div className="grid-2">
          <label>
            Full name
            <input value={form.name} onChange={set('name')} required />
          </label>
          <label>
            Email
            <input value={user.email} disabled />
          </label>
          <label>
            Phone
            <input value={form.phone} onChange={set('phone')} />
          </label>
          <label>
            Location
            <input value={form.location} onChange={set('location')} placeholder="Pune" />
          </label>
          <label>
            Experience (years)
            <input type="number" min="0" value={form.experienceYears} onChange={set('experienceYears')} />
          </label>
          <label>
            Expected salary (LPA)
            <input type="number" min="0" step="0.5" value={form.expectedLpa} onChange={set('expectedLpa')} />
          </label>
        </div>
        <label>
          Education
          <input value={form.education} onChange={set('education')} placeholder="B.E. Computer Engineering" />
        </label>
        <label>
          Skills <span className="muted">(comma separated)</span>
          <input value={form.skills} onChange={set('skills')} placeholder="Java, Spring Boot, SQL" />
        </label>
        <SkillChips skills={parseList(form.skills)} />
        <button className="btn btn-primary" disabled={busy}>
          {busy ? 'Saving…' : 'Save profile'}
        </button>
      </form>
    </>
  );
}

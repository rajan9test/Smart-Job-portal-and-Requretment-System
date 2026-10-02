import { useState } from 'react';
import { fromLpa, humanize, parseList, toLpa } from '../../utils/format.js';

function initialState(job) {
  const inThirtyDays = new Date(Date.now() + 30 * 86_400_000).toISOString().slice(0, 10);
  return {
    title: job?.title ?? '',
    description: job?.description ?? '',
    skills: (job?.requiredSkills ?? []).join(', '),
    minExperience: job?.minExperience ?? 0,
    maxExperience: job?.maxExperience ?? 2,
    minLpa: job ? toLpa(job.minSalary) : '',
    maxLpa: job ? toLpa(job.maxSalary) : '',
    location: job?.location ?? '',
    jobType: job?.jobType ?? 'FULL_TIME',
    openings: job?.openings ?? 1,
    deadline: job?.deadline ?? inThirtyDays,
  };
}

/** Create / edit form. Calls onSubmit with the JSON body expected by POST/PUT /api/jobs. */
export default function JobForm({ job, busy, onSubmit, onCancel }) {
  const [form, setForm] = useState(() => initialState(job));
  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = (e) => {
    e.preventDefault();
    onSubmit({
      title: form.title,
      description: form.description,
      requiredSkills: parseList(form.skills),
      minExperience: Number(form.minExperience),
      maxExperience: Number(form.maxExperience),
      minSalary: fromLpa(form.minLpa) ?? 0,
      maxSalary: fromLpa(form.maxLpa) ?? 0,
      location: form.location,
      jobType: form.jobType,
      openings: Number(form.openings),
      deadline: form.deadline || null,
    });
  };

  return (
    <form className="card form form-wide" onSubmit={submit}>
      <h2>{job ? `Edit: ${job.title}` : 'Post a new job'}</h2>
      <label>
        Job title
        <input value={form.title} onChange={set('title')} required />
      </label>
      <label>
        Description
        <textarea rows="3" value={form.description} onChange={set('description')} />
      </label>
      <label>
        Required skills <span className="muted">(comma separated)</span>
        <input value={form.skills} onChange={set('skills')} placeholder="Java, Spring Boot, SQL" />
      </label>
      <div className="grid-3">
        <label>
          Min experience (yrs)
          <input type="number" min="0" value={form.minExperience} onChange={set('minExperience')} required />
        </label>
        <label>
          Max experience (yrs)
          <input type="number" min="0" value={form.maxExperience} onChange={set('maxExperience')} required />
        </label>
        <label>
          Openings
          <input type="number" min="1" value={form.openings} onChange={set('openings')} required />
        </label>
        <label>
          Min salary (LPA)
          <input type="number" min="0" step="0.5" value={form.minLpa} onChange={set('minLpa')} required />
        </label>
        <label>
          Max salary (LPA)
          <input type="number" min="0" step="0.5" value={form.maxLpa} onChange={set('maxLpa')} required />
        </label>
        <label>
          Job type
          <select value={form.jobType} onChange={set('jobType')}>
            {['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP'].map((t) => (
              <option key={t} value={t}>{humanize(t)}</option>
            ))}
          </select>
        </label>
        <label>
          Location
          <input value={form.location} onChange={set('location')} required />
        </label>
        <label>
          Application deadline
          <input type="date" value={form.deadline} onChange={set('deadline')} />
        </label>
      </div>
      <div className="actions">
        <button className="btn btn-primary" disabled={busy}>{job ? 'Save changes' : 'Post job'}</button>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}

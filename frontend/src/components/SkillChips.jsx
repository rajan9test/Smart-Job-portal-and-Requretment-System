/** Renders skills as chips; skills present in `highlight` are emphasised (e.g. matching a job). */
export default function SkillChips({ skills, highlight }) {
  if (!skills || skills.length === 0) return <span className="muted">—</span>;
  const wanted = new Set((highlight ?? []).map((s) => s.toLowerCase()));
  return (
    <span className="chips">
      {skills.map((skill) => (
        <span key={skill} className={`chip ${wanted.has(skill.toLowerCase()) ? 'chip-match' : ''}`}>
          {skill}
        </span>
      ))}
    </span>
  );
}

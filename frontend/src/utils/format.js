// Display helpers. Salaries are annual INR on the backend; the UI shows them in LPA (lakhs per annum).

const LAKH = 100_000;

export function toLpa(rupees) {
  if (rupees == null) return '';
  const lpa = rupees / LAKH;
  return Number.isInteger(lpa) ? String(lpa) : lpa.toFixed(1);
}

export function fromLpa(lpa) {
  if (lpa === '' || lpa == null) return null;
  return Math.round(parseFloat(lpa) * LAKH);
}

export function salaryRange(min, max) {
  if (!max) return 'Not disclosed';
  return `${toLpa(min)}–${toLpa(max)} LPA`;
}

export function formatDate(value) {
  if (!value) return '—';
  return new Date(value).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export function formatDateTime(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** "FULL_TIME" -> "Full Time" */
export function humanize(value) {
  if (!value) return '';
  return value
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

/** "java, Spring Boot ,sql" -> ["java", "Spring Boot", "sql"] */
export function parseList(text) {
  return text
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

export function minutesBetween(start, end) {
  return Math.round((new Date(end) - new Date(start)) / 60000);
}

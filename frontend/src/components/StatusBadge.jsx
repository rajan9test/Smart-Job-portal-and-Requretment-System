import { humanize } from '../utils/format.js';

/** Coloured pill for application / interview / job statuses. */
export default function StatusBadge({ status }) {
  if (!status) return null;
  return <span className={`badge badge-${status.toLowerCase()}`}>{humanize(status)}</span>;
}

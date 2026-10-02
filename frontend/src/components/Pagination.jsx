export default function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Pagination">
      <button className="btn btn-secondary" disabled={page === 0} onClick={() => onChange(page - 1)}>
        ← Previous
      </button>
      <span>
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn btn-secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Next →
      </button>
    </nav>
  );
}

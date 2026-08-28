export function PageTitle({ eyebrow, title, description, action }) {
  return (
    <div className="page-title">
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action}
    </div>
  );
}

export function StatusBadge({ value }) {
  const tone = ["ACTIVE", "OPEN", "COMPLETED"].includes(value)
    ? "positive"
    : ["LOCKED", "CANCELLED", "INACTIVE"].includes(value)
      ? "negative"
      : "neutral";
  return (
    <span className={`status-badge ${tone}`}>
      <i />
      {value || "—"}
    </span>
  );
}

export function Modal({ title, children, onClose }) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="modal"
        role="dialog"
        aria-modal="true"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header>
          <h2>{title}</h2>
          <button className="icon-button" onClick={onClose}>
            ×
          </button>
        </header>
        {children}
      </section>
    </div>
  );
}

export function EmptyState({ message = "Chưa có dữ liệu phù hợp" }) {
  return (
    <div className="empty-state">
      <span>⌕</span>
      <strong>{message}</strong>
      <small>Thử thay đổi bộ lọc hoặc thêm dữ liệu mới.</small>
    </div>
  );
}

export function ErrorAlert({ message }) {
  return message ? <div className="alert error">{message}</div> : null;
}

export function LoadingRows({ columns = 5 }) {
  return Array.from({ length: 4 }).map((_, row) => (
    <tr key={row}>
      {Array.from({ length: columns }).map((__, col) => (
        <td key={col}>
          <span className="skeleton" />
        </td>
      ))}
    </tr>
  ));
}

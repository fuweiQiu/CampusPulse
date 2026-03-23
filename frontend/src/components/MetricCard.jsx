export default function MetricCard({ label, value, hint, accent }) {
  return (
    <div className="panel rounded-[24px] p-5">
      <p className="text-sm font-semibold uppercase tracking-[0.24em] text-[var(--muted)]">
        {label}
      </p>
      <div className="mt-4 flex items-end gap-3">
        <p className="text-4xl font-extrabold tracking-tight text-slate-900">{value}</p>
        <span
          className="mb-1 inline-block h-3 w-3 rounded-full"
          style={{ backgroundColor: accent }}
        />
      </div>
      <p className="mt-2 text-sm text-[var(--muted)]">{hint}</p>
    </div>
  );
}

export default function SectionTitle({ eyebrow, title, description }) {
  return (
    <div className="mb-5">
      <p className="text-xs font-bold uppercase tracking-[0.28em] text-[var(--accent-deep)]">
        {eyebrow}
      </p>
      <h2 className="mt-2 text-2xl font-extrabold tracking-tight text-slate-900">{title}</h2>
      {description ? (
        <p className="mt-2 max-w-3xl text-sm leading-6 text-[var(--muted)]">{description}</p>
      ) : null}
    </div>
  );
}

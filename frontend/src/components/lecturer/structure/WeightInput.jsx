export default function WeightInput({
  id,
  label = 'Weight',
  value,
  onChange,
  className = '',
}) {
  const numeric = Number(value);
  const display = Number.isFinite(numeric) && numeric > 0 ? numeric : 1;

  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1 block text-xs text-foreground-muted">
        {label}
      </label>
      <input
        id={id}
        type="number"
        min={1}
        step={1}
        className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white"
        value={display}
        onChange={(event) => {
          const next = Number.parseInt(event.target.value, 10);
          onChange(Number.isFinite(next) && next > 0 ? next : 1);
        }}
      />
    </div>
  );
}

export function formatWeight(value) {
  const numeric = Number(value);
  return Number.isFinite(numeric) && numeric > 0 ? numeric : 1;
}

export default function Select({ label, options, className = "" }) {
  return (
    <div className={`bg-surface rounded-xl p-4 shadow-sm dark:shadow-none ${className}`}>
      {label && <label className="block text-foreground-muted text-sm mb-2">{label}</label>}
      <select className="w-full bg-surface-secondary text-foreground px-4 py-2.5 rounded-lg border border-border focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none transition-colors">
        {options.map((opt, idx) => (
          <option key={idx} value={opt}>{opt}</option>
        ))}
      </select>
    </div>
  );
}
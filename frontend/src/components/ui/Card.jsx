export default function Card({ children, className = "" }) {
  return (
    <div className={`bg-surface rounded-2xl p-6 shadow-stage-1 border border-border transition-colors ${className}`}>
      {children}
    </div>
  );
}
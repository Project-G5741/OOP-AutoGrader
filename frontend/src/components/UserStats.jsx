import { Users } from 'lucide-react';

export default function UserStats({ stats }) {
  return (
    <div className="grid grid-cols-3 gap-4 mb-6">
      {stats.map(({ label, value, color }) => (
        <div key={label} className="bg-surface rounded-xl p-4 border border-border">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-foreground-muted">{label}</span>
            <Users className={`w-4 h-4 ${color}`} />
          </div>
          <p className={`text-2xl font-semibold ${color}`}>{value}</p>
        </div>
      ))}
    </div>
  );
}

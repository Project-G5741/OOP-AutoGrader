import { Users } from 'lucide-react';

export default function UserStats({ stats }) {
  return (
    <div className="grid grid-cols-3 gap-4 mb-6">
      {stats.map(({ label, value, color }) => (
        <div key={label} className="bg-white dark:bg-[#1e2530] rounded-xl p-4 border border-gray-100 dark:border-gray-800">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-gray-500 dark:text-gray-400">{label}</span>
            <Users className={`w-4 h-4 ${color}`} />
          </div>
          <p className={`text-2xl font-semibold ${color}`}>{value}</p>
        </div>
      ))}
    </div>
  );
}

import React from 'react';

export default function LecturerOverviewCard({ title, value, subtitle, icon, accent }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm text-gray-500 dark:text-gray-400">{title}</p>
          <p className="mt-2 text-2xl font-semibold text-gray-900 dark:text-white">{value}</p>
          {subtitle && <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{subtitle}</p>}
        </div>
        <div className={`rounded-lg p-3 ${accent}`}>{icon}</div>
      </div>
    </div>
  );
}

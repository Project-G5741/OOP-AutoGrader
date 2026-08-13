import React from 'react';

export default function LecturerOverviewCard({ title, value, icon, accent }) {
  return (
    <div className="rounded-xl border border-border-subtle bg-surface p-5 transition-colors">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm text-foreground-secondary">{title}</p>
          <p className="mt-2 text-2xl font-semibold text-foreground">{value}</p>
        </div>
        <div className={`rounded-lg p-3 ${accent}`}>{icon}</div>
      </div>
    </div>
  );
}

import React from 'react';

export default function DashboardSection({ title, actions, children }) {
  return (
    <section className="rounded-xl border border-border bg-surface p-6 shadow-sm transition-colors">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-foreground">{title}</h2>
        </div>
        {actions}
      </div>
      {children}
    </section>
  );
}

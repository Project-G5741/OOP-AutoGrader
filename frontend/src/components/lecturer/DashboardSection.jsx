import React from 'react';

export default function DashboardSection({ title, actions, children }) {
  return (
    <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">{title}</h2>
        </div>
        {actions}
      </div>
      {children}
    </section>
  );
}

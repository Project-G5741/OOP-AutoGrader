import React from 'react';

export default function DashboardSection({ title, actions, children, className = '', contentClassName = '' }) {
  return (
    <section
      className={`rounded-xl border border-border bg-surface p-4 shadow-sm transition-colors dark:border-transparent dark:shadow-none sm:p-6 ${className}`}
    >
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-foreground">{title}</h2>
        </div>
        {actions}
      </div>
      {contentClassName ? <div className={contentClassName}>{children}</div> : children}
    </section>
  );
}

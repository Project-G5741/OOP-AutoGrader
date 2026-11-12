import React from 'react';

export default function DashboardSection({ title, actions, children, className = '', contentClassName = '' }) {
  return (
    <section className={`transition-colors ${className}`}>
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
        <div className="flex items-center gap-2.5">
          <span className="h-5 w-1 rounded-full bg-navy dark:bg-primary" aria-hidden="true" />
          <h2 className="text-lg font-bold tracking-tight text-foreground">{title}</h2>
        </div>
        {actions}
      </div>
      {contentClassName ? <div className={contentClassName}>{children}</div> : children}
    </section>
  );
}

import React from 'react';
import { Upload } from 'lucide-react';

export default function UploadPanel({ title, actionLabel = '+ Select files' }) {
  return (
    <div className="rounded-xl bg-surface p-6 shadow-sm transition-colors">
      <div className="rounded-lg border-2 border-dashed border-primary/30 p-8">
        <div className="flex flex-col items-center justify-center text-center">
          <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-primary-light">
            <Upload className="h-8 w-8 text-primary" />
          </div>
          <h3 className="mb-4 text-lg font-semibold text-foreground">{title}</h3>
          <button className="rounded-lg bg-primary px-6 py-2 text-white transition-colors hover:bg-primary-hover" type="button">
            {actionLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

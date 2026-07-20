import React from 'react';
import { Upload } from 'lucide-react';

export default function UploadPanel({ title, description, actionLabel = '+ Select files' }) {
  return (
    <div className="rounded-xl bg-white p-6 shadow-sm transition-colors dark:bg-[#1e2530]">
      <div className="rounded-lg border-2 border-dashed border-blue-500/30 p-8">
        <div className="flex flex-col items-center justify-center text-center">
          <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-blue-500/10">
            <Upload className="h-8 w-8 text-blue-500" />
          </div>
          <h3 className="mb-2 text-lg font-semibold text-gray-900 dark:text-white">{title}</h3>
          <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">{description}</p>
          <button className="rounded-lg bg-blue-600 px-6 py-2 text-white transition-colors hover:bg-blue-700" type="button">
            {actionLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

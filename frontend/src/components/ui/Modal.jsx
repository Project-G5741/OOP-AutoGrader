import React from 'react';

export default function Modal({ children, onClose, className = '' }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className={`w-full max-w-2xl rounded-2xl bg-surface border border-border p-6 shadow-xl ${className}`}>
        <button onClick={onClose} className="mb-4 text-sm text-foreground-secondary hover:text-foreground">Close</button>
        {children}
      </div>
    </div>
  );
}

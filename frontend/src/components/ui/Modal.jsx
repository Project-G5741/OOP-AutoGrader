import React from 'react';

export default function Modal({ children, onClose, className = '' }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className={`w-full max-w-2xl rounded-2xl bg-white border border-gray-200 p-6 shadow-xl dark:bg-[#161b22] dark:border-gray-700 ${className}`}>
        <button onClick={onClose} className="mb-4 text-sm text-gray-600 hover:text-gray-800 dark:text-gray-300">Close</button>
        {children}
      </div>
    </div>
  );
}

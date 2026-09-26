import React from 'react';
import ModalOverlay from './ModalOverlay';

export default function Modal({ children, onClose, className = '', showClose = true }) {
  return (
    <ModalOverlay onBackdropClick={onClose}>
      <div
        className={`w-full max-w-2xl rounded-2xl bg-surface border border-border p-6 shadow-xl ${className}`}
        onClick={(event) => event.stopPropagation()}
      >
        {showClose ? (
          <button onClick={onClose} className="mb-4 text-sm text-foreground-secondary hover:text-foreground">Close</button>
        ) : null}
        {children}
      </div>
    </ModalOverlay>
  );
}

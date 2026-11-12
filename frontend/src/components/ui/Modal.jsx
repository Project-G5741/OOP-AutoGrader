import React from 'react';
import { motion } from 'framer-motion';
import { X } from 'lucide-react';
import ModalOverlay from './ModalOverlay';

export default function Modal({ children, onClose, className = '', showClose = true }) {
  return (
    <ModalOverlay onBackdropClick={onClose}>
      <motion.div
        initial={{ opacity: 0, scale: 0.96, y: 8 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        transition={{ duration: 0.18, ease: [0.22, 1, 0.36, 1] }}
        className={`w-full max-w-2xl rounded-2xl bg-surface border border-border p-6 shadow-stage-3 ${className}`}
        onClick={(event) => event.stopPropagation()}
      >
        {showClose ? (
          <div className="mb-3 flex justify-end">
            <button
              type="button"
              onClick={onClose}
              className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-medium text-foreground-secondary transition-colors hover:bg-surface-secondary hover:text-foreground"
            >
              <X className="h-3.5 w-3.5" />
              Close
            </button>
          </div>
        ) : null}
        {children}
      </motion.div>
    </ModalOverlay>
  );
}

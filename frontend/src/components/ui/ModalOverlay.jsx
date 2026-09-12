import { createPortal } from 'react-dom';

export default function ModalOverlay({ children, className = '', onBackdropClick }) {
  return createPortal(
    <div
      className={`fixed inset-0 z-[100] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm dark:bg-black/70 ${className}`}
      onClick={onBackdropClick}
      role="presentation"
    >
      {children}
    </div>,
    document.body,
  );
}

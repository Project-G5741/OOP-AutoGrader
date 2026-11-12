import { createPortal } from 'react-dom';
import { motion } from 'framer-motion';

export default function ModalOverlay({ children, className = '', onBackdropClick }) {
  return createPortal(
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.16, ease: 'easeOut' }}
      className={`fixed inset-0 z-[100] flex items-center justify-center bg-black/60 p-4 backdrop-blur-[2px] dark:bg-black/75 ${className}`}
      onClick={onBackdropClick}
      role="presentation"
    >
      {children}
    </motion.div>,
    document.body,
  );
}

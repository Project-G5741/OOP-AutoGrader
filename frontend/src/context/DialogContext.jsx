import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import PopupDialog from '../components/ui/PopupDialog';
import {
  registerDialogHandler,
  dispatchSystemAlert,
  dispatchSystemConfirm,
} from '../utils/dialogService';

const DialogContext = createContext(null);

export function DialogProvider({ children }) {
  const [activeDialog, setActiveDialog] = useState(null);

  const closeDialog = useCallback(() => {
    setActiveDialog(null);
  }, []);

  const showAlert = useCallback((options) => {
    return new Promise((resolve) => {
      const opts = typeof options === 'string' ? { message: options } : options || {};
      setActiveDialog({
        type: 'alert',
        level: opts.level || 'safety',
        title: opts.title,
        message: opts.message,
        confirmText: opts.confirmText,
        onConfirm: () => {
          closeDialog();
          resolve();
        },
        onClose: () => {
          closeDialog();
          resolve();
        },
      });
    });
  }, [closeDialog]);

  const showConfirm = useCallback((options) => {
    return new Promise((resolve) => {
      const opts = typeof options === 'string' ? { message: options } : options || {};
      setActiveDialog({
        type: 'confirm',
        level: opts.level || 'warning',
        title: opts.title,
        message: opts.message,
        confirmText: opts.confirmText,
        cancelText: opts.cancelText,
        onConfirm: () => {
          closeDialog();
          resolve(true);
        },
        onCancel: () => {
          closeDialog();
          resolve(false);
        },
        onClose: () => {
          closeDialog();
          resolve(false);
        },
      });
    });
  }, [closeDialog]);

  // Register with global dialog service so outside/system alerts trigger this modal
  useEffect(() => {
    const unregister = registerDialogHandler((item) => {
      setActiveDialog({
        ...item,
        onConfirm: () => {
          closeDialog();
          item.onConfirm?.();
        },
        onCancel: () => {
          closeDialog();
          item.onCancel?.();
        },
        onClose: () => {
          closeDialog();
          item.onClose?.();
        },
      });
    });
    return unregister;
  }, [closeDialog]);

  const value = {
    alert: showAlert,
    confirm: showConfirm,
  };

  return (
    <DialogContext.Provider value={value}>
      {children}
      {activeDialog && (
        <PopupDialog
          isOpen={Boolean(activeDialog)}
          type={activeDialog.type}
          level={activeDialog.level}
          title={activeDialog.title}
          message={activeDialog.message}
          confirmText={activeDialog.confirmText}
          cancelText={activeDialog.cancelText}
          onConfirm={activeDialog.onConfirm}
          onCancel={activeDialog.onCancel}
          onClose={activeDialog.onClose}
        />
      )}
    </DialogContext.Provider>
  );
}

/**
 * Hook to access dialog popups (alert & confirm)
 * Safe fallback to global dispatch if used outside DialogProvider.
 */
export function useDialog() {
  const context = useContext(DialogContext);
  if (!context) {
    return {
      alert: dispatchSystemAlert,
      confirm: dispatchSystemConfirm,
    };
  }
  return context;
}

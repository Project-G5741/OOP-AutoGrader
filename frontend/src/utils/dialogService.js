/**
 * Global bridge service for dispatching custom dialog popups.
 * Allows components, services, or runtime interceptors outside React tree
 * to trigger in-app popups without native browser "says:" dialogs.
 */

let activeHandler = null;
const pendingQueue = [];

/**
 * Registers the active dialog provider handler.
 * Process any pending alerts queued prior to React mounting.
 */
export function registerDialogHandler(handler) {
  activeHandler = handler;
  while (pendingQueue.length > 0 && activeHandler) {
    const item = pendingQueue.shift();
    activeHandler(item);
  }
  return () => {
    if (activeHandler === handler) {
      activeHandler = null;
    }
  };
}

/**
 * Dispatches a system alert popup.
 *
 * @param {Object} options
 * @param {'safety'|'warning'|'dangerous'} [options.level='safety']
 * @param {string} [options.title]
 * @param {string|React.ReactNode} options.message
 * @param {string} [options.confirmText]
 * @returns {Promise<void>}
 */
export function dispatchSystemAlert(options) {
  return new Promise((resolve) => {
    const item = {
      type: 'alert',
      level: options?.level || 'safety',
      title: options?.title,
      message: options?.message,
      confirmText: options?.confirmText,
      onConfirm: () => resolve(),
      onClose: () => resolve(),
    };

    if (activeHandler) {
      activeHandler(item);
    } else {
      pendingQueue.push(item);
    }
  });
}

/**
 * Dispatches a system confirm popup.
 *
 * @param {Object} options
 * @param {'safety'|'warning'|'dangerous'} [options.level='warning']
 * @param {string} [options.title]
 * @param {string|React.ReactNode} options.message
 * @param {string} [options.confirmText]
 * @param {string} [options.cancelText]
 * @returns {Promise<boolean>}
 */
export function dispatchSystemConfirm(options) {
  return new Promise((resolve) => {
    const item = {
      type: 'confirm',
      level: options?.level || 'warning',
      title: options?.title,
      message: options?.message,
      confirmText: options?.confirmText,
      cancelText: options?.cancelText,
      onConfirm: () => resolve(true),
      onCancel: () => resolve(false),
      onClose: () => resolve(false),
    };

    if (activeHandler) {
      activeHandler(item);
    } else {
      pendingQueue.push(item);
    }
  });
}

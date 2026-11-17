/**
 * Global Guard to intercept native browser dialogs (window.alert, window.confirm, window.prompt).
 * Guarantees that neither "localhost:5173 says:" nor "oop-autograder.vercel.app says:"
 * ever appears in the user experience.
 */
import { dispatchSystemAlert } from './dialogService.js';

let isGuardInstalled = false;

export function installBrowserDialogGuard(force = false) {
  if (typeof window === 'undefined') return;
  if (isGuardInstalled && !force && window.alert?.name === 'patchedAlert') return;

  // Preserve native references for tests or low-level debugging if needed
  if (!window.__nativeAlert) {
    window.__nativeAlert = window.alert;
    window.__nativeConfirm = window.confirm;
    window.__nativePrompt = window.prompt;
  }

  // Intercept window.alert
  window.alert = function patchedAlert(message) {
    const text = String(message ?? '');
    const lower = text.toLowerCase();
    let level = 'warning';

    if (
      lower.includes('error') ||
      lower.includes('fail') ||
      lower.includes('danger') ||
      lower.includes('delete')
    ) {
      level = 'dangerous';
    } else if (
      lower.includes('success') ||
      lower.includes('cleared') ||
      lower.includes('saved') ||
      lower.includes('done')
    ) {
      level = 'safety';
    }

    dispatchSystemAlert({
      level,
      title:
        level === 'dangerous'
          ? 'System Alert'
          : level === 'safety'
            ? 'Success'
            : 'Notice',
      message: text,
      confirmText: 'OK',
    });
  };

  // Intercept window.confirm
  window.confirm = function patchedConfirm(message) {
    console.warn(
      '[DialogGuard] window.confirm was intercepted. Native browser confirm is disabled to prevent "web says:" popups. Use useDialog().confirm() for async popups instead.',
      message,
    );

    dispatchSystemAlert({
      level: 'warning',
      title: 'Action Requires Confirmation',
      message: String(message ?? ''),
      confirmText: 'OK',
    });

    return false;
  };

  // Intercept window.prompt
  window.prompt = function patchedPrompt(message) {
    console.warn(
      '[DialogGuard] window.prompt was intercepted. Native browser prompt is disabled.',
      message,
    );
    return null;
  };

  isGuardInstalled = true;
}

export function isBrowserDialogGuardInstalled() {
  return isGuardInstalled;
}

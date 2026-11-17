import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { installBrowserDialogGuard } from '../dialogGuard';
import * as dialogService from '../dialogService';

describe('dialogGuard', () => {
  let originalWindow;

  beforeEach(() => {
    originalWindow = globalThis.window;
    globalThis.window = {
      alert: vi.fn(),
      confirm: vi.fn(),
      prompt: vi.fn(),
    };
    installBrowserDialogGuard();
  });

  afterEach(() => {
    globalThis.window = originalWindow;
  });

  it('intercepts window.alert and dispatches system dialog instead of native dialog', () => {
    const alertSpy = vi.spyOn(dialogService, 'dispatchSystemAlert').mockImplementation(() => Promise.resolve());

    window.alert('Operation completed successfully');
    expect(alertSpy).toHaveBeenCalledTimes(1);
    expect(alertSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        level: 'safety',
        message: 'Operation completed successfully',
      })
    );

    window.alert('Critical delete failure error occurred');
    expect(alertSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        level: 'dangerous',
        message: 'Critical delete failure error occurred',
      })
    );

    alertSpy.mockRestore();
  });

  it('intercepts window.confirm, logs a warning, dispatches an alert, and returns false safely', () => {
    const alertSpy = vi.spyOn(dialogService, 'dispatchSystemAlert').mockImplementation(() => Promise.resolve());
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

    const result = window.confirm('Are you sure you want to proceed?');
    expect(result).toBe(false);
    expect(warnSpy).toHaveBeenCalled();
    expect(alertSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        level: 'warning',
        title: 'Action Requires Confirmation',
        message: 'Are you sure you want to proceed?',
      })
    );

    alertSpy.mockRestore();
    warnSpy.mockRestore();
  });

  it('intercepts window.prompt and returns null without opening a browser modal', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

    const result = window.prompt('Enter your name');
    expect(result).toBeNull();
    expect(warnSpy).toHaveBeenCalled();

    warnSpy.mockRestore();
  });
});

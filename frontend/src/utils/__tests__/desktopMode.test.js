import { afterEach, describe, expect, it, vi } from 'vitest';
import { desktopApiBase, isDesktopLocalMode } from '../desktopMode.js';

describe('desktopMode', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('detects student and lecturer desktop modes', () => {
    vi.stubEnv('VITE_APP_MODE', 'desktop');
    expect(isDesktopLocalMode()).toBe(true);
    vi.stubEnv('VITE_APP_MODE', 'desktop-lecturer');
    expect(isDesktopLocalMode()).toBe(true);
    vi.stubEnv('VITE_APP_MODE', 'web');
    expect(isDesktopLocalMode()).toBe(false);
  });

  it('defaults API bases by mode when VITE_API_URL unset', () => {
    vi.stubEnv('VITE_API_URL', '');
    vi.stubEnv('VITE_APP_MODE', 'desktop');
    expect(desktopApiBase()).toBe('http://127.0.0.1:18002');
    vi.stubEnv('VITE_APP_MODE', 'desktop-lecturer');
    expect(desktopApiBase()).toBe('http://127.0.0.1:18003');
    vi.stubEnv('VITE_APP_MODE', 'web');
    expect(desktopApiBase()).toBe('http://localhost:8002');
  });

  it('prefers explicit VITE_API_URL', () => {
    vi.stubEnv('VITE_API_URL', 'http://example.test:9');
    vi.stubEnv('VITE_APP_MODE', 'desktop');
    expect(desktopApiBase()).toBe('http://example.test:9');
  });
});

import { afterEach, describe, expect, it, vi } from 'vitest';
import { isWebMaintenanceMode } from '../maintenanceMode.js';

describe('maintenanceMode', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('is off by default', () => {
    vi.stubEnv('VITE_APP_MODE', 'web');
    vi.stubEnv('VITE_MAINTENANCE_MODE', '');
    vi.stubEnv('NEXT_PUBLIC_MAINTENANCE_MODE', '');
    expect(isWebMaintenanceMode()).toBe(false);
  });

  it('turns on for truthy VITE_MAINTENANCE_MODE on web', () => {
    vi.stubEnv('VITE_APP_MODE', 'web');
    vi.stubEnv('VITE_MAINTENANCE_MODE', 'true');
    expect(isWebMaintenanceMode()).toBe(true);
  });

  it('never locks desktop practice', () => {
    vi.stubEnv('VITE_APP_MODE', 'desktop');
    vi.stubEnv('VITE_MAINTENANCE_MODE', 'true');
    expect(isWebMaintenanceMode()).toBe(false);
  });
});

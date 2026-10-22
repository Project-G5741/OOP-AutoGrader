const TRUTHY = new Set(['true', '1', 'yes', 'on']);

function envIsOn(value) {
  return TRUTHY.has(String(value ?? '').trim().toLowerCase());
}

/**
 * Build-time lock for the web SPA. Desktop practice (`VITE_APP_MODE=desktop`)
 * never enters this gate. Vite inlines `VITE_*` and `NEXT_PUBLIC_*` at build.
 */
export function isWebMaintenanceMode() {
  if (import.meta.env.VITE_APP_MODE === 'desktop') {
    return false;
  }
  return envIsOn(import.meta.env.VITE_MAINTENANCE_MODE)
    || envIsOn(import.meta.env.NEXT_PUBLIC_MAINTENANCE_MODE);
}

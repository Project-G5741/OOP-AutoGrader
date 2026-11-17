import { useEffect, useState } from 'react';

const IS_DESKTOP_APP = import.meta.env.VITE_APP_MODE === 'desktop';

async function loadBundledPracticeBundleVersion() {
  const base = import.meta.env.BASE_URL || '/';
  const res = await fetch(`${base}practice-bundle-version.json`, { cache: 'no-store' });
  if (!res.ok) {
    return null;
  }
  const data = await res.json();
  if (!data?.version) {
    return null;
  }
  const date = String(data.updatedOn ?? '').trim();
  const label = date ? `version ${data.version} - ${date}` : `version ${data.version}`;
  return {
    version: data.version,
    updatedOn: date,
    headerLabel: IS_DESKTOP_APP ? label : null,
  };
}

/**
 * Web + desktop: {@code frontend/public/practice-bundle-version.json} (copied into the practice zip at assemble).
 * Bump that file and redeploy web + publish the zip on each release.
 */
export function useDesktopPracticeBundleVersion({ enabled = true } = {}) {
  const [meta, setMeta] = useState(null);

  useEffect(() => {
    if (!enabled) {
      setMeta(null);
      return undefined;
    }

    let cancelled = false;

    async function load() {
      try {
        const bundled = await loadBundledPracticeBundleVersion();
        if (!cancelled && bundled) {
          setMeta(bundled);
        }
      } catch {
        // Omit label when metadata is unavailable (zip may still 503).
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [enabled]);

  return meta;
}
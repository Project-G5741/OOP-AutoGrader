/**
 * Visit-scoped lecturer tab bootstrap cache.
 * Survives App.jsx remount of LecturerDashboard on each lecturer route.
 * Always-fresh: only store successful fetches for this browser session; clear on logout / invalidate.
 */

const TAB_PATHS = {
  dashboard: '/api/lecturer/bootstrap/dashboard',
  score: '/api/lecturer/bootstrap/score',
  grading: '/api/lecturer/bootstrap/grading',
  users: '/api/lecturer/bootstrap/users',
  terms: '/api/lecturer/bootstrap/quarters',
  projects: '/api/lecturer/bootstrap/solution',
  reports: '/api/lecturer/bootstrap/reports',
};

/** @type {Map<string, { promise?: Promise<unknown>, data?: unknown, error?: Error }>} */
const entries = new Map();

let generation = 0;

export function lecturerBootstrapPath(tabId) {
  return TAB_PATHS[tabId] || null;
}

export function clearLecturerBootstrapStore() {
  generation += 1;
  entries.clear();
}

export function invalidateLecturerBootstrap(tabId) {
  if (tabId) {
    entries.delete(tabId);
  } else {
    entries.clear();
  }
}

/**
 * @param {string} tabId
 * @param {(url: string) => Promise<unknown>} fetcher — resolves JSON body
 * @returns {Promise<unknown>}
 */
export function getOrFetchLecturerBootstrap(tabId, fetcher) {
  const path = TAB_PATHS[tabId];
  if (!path) {
    return Promise.reject(new Error(`Unknown lecturer tab: ${tabId}`));
  }

  const existing = entries.get(tabId);
  if (existing?.data !== undefined) {
    return Promise.resolve(existing.data);
  }
  if (existing?.promise) {
    return existing.promise;
  }

  const gen = generation;
  const promise = Promise.resolve()
    .then(() => fetcher(path))
    .then((data) => {
      if (gen !== generation) {
        return data;
      }
      entries.set(tabId, { data });
      return data;
    })
    .catch((error) => {
      if (gen === generation) {
        entries.delete(tabId);
      }
      throw error;
    });

  entries.set(tabId, { promise });
  return promise;
}

/** Prefetch without surfacing errors to the caller. */
export function prefetchLecturerBootstrap(tabId, fetcher) {
  return getOrFetchLecturerBootstrap(tabId, fetcher).catch(() => undefined);
}

export function peekLecturerBootstrap(tabId) {
  const entry = entries.get(tabId);
  return entry?.data;
}

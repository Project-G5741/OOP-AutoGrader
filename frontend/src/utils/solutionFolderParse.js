/**
 * Parse lecturer Solution import folder selection into challenge groups.
 * Accepts lab-root: Name/challenge_n/... or single-challenge: challenge_n/...
 */

import { CHALLENGE_FOLDER_PATTERN } from './bulkFolderParse';

const CHALLENGE_NUMBER = /challenge[_-]?(\d+)/i;

/**
 * @param {Array<{ file: File, relativePath: string }>} entries
 * @returns {{
 *   ok: boolean,
 *   shape?: 'lab-root' | 'single-challenge',
 *   challenges?: Array<{ challengeNumber: number, folderName: string, files: Array<{ file: File, relativePath: string }> }>,
 *   skipped?: Array<{ challengeNumber?: number, reason: string }>,
 *   error?: string,
 * }}
 */
export function parseSolutionFolder(entries) {
  if (!Array.isArray(entries) || entries.length === 0) {
    return { ok: false, error: 'No .java or .mmd files found.' };
  }

  const normalized = entries
    .map((e) => ({
      file: e.file,
      relativePath: String(e.relativePath || e.file?.webkitRelativePath || e.file?.name || '')
        .replace(/\\/g, '/')
        .replace(/^\/+/, ''),
    }))
    .filter((e) => {
      const lower = e.relativePath.toLowerCase();
      return e.relativePath && (lower.endsWith('.java') || lower.endsWith('.mmd'));
    });

  if (normalized.length === 0) {
    return { ok: false, error: 'No .java or .mmd files found.' };
  }

  const firstSegs = normalized.map((e) => e.relativePath.split('/').filter(Boolean)[0]).filter(Boolean);
  const sharedTop = firstSegs.length && firstSegs.every((s) => s === firstSegs[0]) ? firstSegs[0] : null;

  let shape;
  let challengeSegIndex;
  if (sharedTop && CHALLENGE_FOLDER_PATTERN.test(sharedTop)) {
    shape = 'single-challenge';
    challengeSegIndex = 0;
  } else if (sharedTop) {
    const hasChallengeUnderTop = normalized.some((e) => {
      const parts = e.relativePath.split('/').filter(Boolean);
      return parts.length >= 3 && parts[0] === sharedTop && CHALLENGE_FOLDER_PATTERN.test(parts[1]);
    });
    if (hasChallengeUnderTop) {
      shape = 'lab-root';
      challengeSegIndex = 1;
    }
  }

  if (!shape) {
    // Fallback: any path segment matching challenge_n
    const byNumber = new Map();
    for (const entry of normalized) {
      const parts = entry.relativePath.split('/').filter(Boolean);
      const challengePart = parts.find((p) => CHALLENGE_FOLDER_PATTERN.test(p));
      if (!challengePart) continue;
      const match = challengePart.match(CHALLENGE_NUMBER);
      if (!match) continue;
      const n = Number(match[1]);
      if (!byNumber.has(n)) {
        byNumber.set(n, { challengeNumber: n, folderName: challengePart, files: [] });
      }
      byNumber.get(n).files.push(entry);
    }
    if (byNumber.size === 0) {
      return {
        ok: false,
        error: 'Folder must be Name/challenge_n/... or a single challenge_n folder.',
      };
    }
    return finalizeChallenges(Array.from(byNumber.values()), 'lab-root');
  }

  const byNumber = new Map();
  for (const entry of normalized) {
    const parts = entry.relativePath.split('/').filter(Boolean);
    if (parts.length <= challengeSegIndex) continue;
    const folderName = parts[challengeSegIndex];
    if (!CHALLENGE_FOLDER_PATTERN.test(folderName)) continue;
    const match = folderName.match(CHALLENGE_NUMBER);
    if (!match) continue;
    const n = Number(match[1]);
    if (!byNumber.has(n)) {
      byNumber.set(n, { challengeNumber: n, folderName, files: [] });
    }
    byNumber.get(n).files.push(entry);
  }

  if (byNumber.size === 0) {
    return {
      ok: false,
      error: 'No challenge_n folders with .java or .mmd files found.',
    };
  }

  return finalizeChallenges(Array.from(byNumber.values()), shape);
}

function finalizeChallenges(challenges, shape) {
  const skipped = [];
  const accepted = [];
  for (const challenge of challenges.sort((a, b) => a.challengeNumber - b.challengeNumber)) {
    const hasJava = challenge.files.some((f) => f.relativePath.toLowerCase().endsWith('.java'));
    if (!hasJava) {
      skipped.push({
        challengeNumber: challenge.challengeNumber,
        reason: `challenge_${challenge.challengeNumber}: No .java files in challenge folder`,
      });
      continue;
    }
    accepted.push(challenge);
  }
  if (accepted.length === 0) {
    return {
      ok: false,
      error: skipped[0]?.reason || 'No challenges with .java files.',
      skipped,
    };
  }
  return { ok: true, shape, challenges: accepted, skipped };
}

/**
 * Build multipart FormData for POST .../solution-import.
 * Relative paths are sent as the file name (Bulk Grading pattern).
 */
export function buildSolutionImportFormData(challenges) {
  const form = new FormData();
  for (const challenge of challenges) {
    for (const { file, relativePath } of challenge.files) {
      form.append('files', file, relativePath);
    }
  }
  return form;
}

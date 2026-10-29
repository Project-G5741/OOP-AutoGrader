/**
 * Parse lecturer Solution import folder selection into challenge groups.
 * Accepts lab-root: Name/challenge_n/... or single-challenge: challenge_n/...
 * Midterm layout: Name/*.java|.mmd (no challenge_n); maps to challenge 1 for the API.
 */

import { CHALLENGE_FOLDER_PATTERN } from './bulkFolderParse';

const CHALLENGE_NUMBER = /challenge[_-]?(\d+)/i;

export const SOLUTION_IMPORT_KIND = {
  OFFICIAL: 'official',
  PRACTICE: 'practice',
  MIDTERM: 'midterm',
};

export function layoutForImportKind(importKind) {
  return importKind === SOLUTION_IMPORT_KIND.MIDTERM ? 'midterm' : 'standard';
}

export function prefixForImportKind(importKind) {
  if (importKind === SOLUTION_IMPORT_KIND.PRACTICE) return 'Practice';
  if (importKind === SOLUTION_IMPORT_KIND.MIDTERM) return 'Midterm';
  return 'Official';
}

export function defaultImportKindFromLabPrefix(labPrefix) {
  if (labPrefix === 'Practice') return SOLUTION_IMPORT_KIND.PRACTICE;
  if (labPrefix === 'Midterm') return SOLUTION_IMPORT_KIND.MIDTERM;
  return SOLUTION_IMPORT_KIND.OFFICIAL;
}

/**
 * @param {Array<{ file: File, relativePath: string }>} entries
 * @param {{ layout?: 'standard' | 'midterm' }} [options]
 * @returns {{
 *   ok: boolean,
 *   shape?: 'lab-root' | 'single-challenge' | 'midterm-flat',
 *   challenges?: Array<{ challengeNumber: number, folderName: string, files: Array<{ file: File, relativePath: string }> }>,
 *   skipped?: Array<{ challengeNumber?: number, reason: string }>,
 *   error?: string,
 * }}
 */
export function parseSolutionFolder(entries, options = {}) {
  const layout = options.layout === 'midterm' ? 'midterm' : 'standard';
  if (layout === 'midterm') {
    return parseMidtermSolutionFolder(entries);
  }
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

function parseMidtermSolutionFolder(entries) {
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

  const challengePattern = CHALLENGE_FOLDER_PATTERN;
  let rootFolder = null;
  const files = [];

  for (const entry of normalized) {
    const parts = entry.relativePath.split('/').filter(Boolean);
    if (parts.length === 1) {
      files.push(entry);
      continue;
    }
    if (parts.length === 2) {
      if (challengePattern.test(parts[1])) {
        return {
          ok: false,
          error: 'Midterm layout expects .java and .mmd directly under one folder, not challenge_n subfolders.',
        };
      }
      if (rootFolder == null) {
        rootFolder = parts[0];
      } else if (rootFolder !== parts[0]) {
        return { ok: false, error: 'Midterm import accepts one folder at a time.' };
      }
      files.push(entry);
      continue;
    }
    if (parts.length >= 3 && parts[1] === '.git') {
      continue;
    }
    return {
      ok: false,
      error: 'Midterm layout expects IRN_Name/file.java (or loose .java files), not nested challenge folders.',
    };
  }

  if (files.length === 0) {
    return { ok: false, error: 'No .java or .mmd files found.' };
  }

  const challengeNumber = 1;
  const withChallengePaths = files.map(({ file, relativePath }) => {
    const parts = relativePath.split('/').filter(Boolean);
    const fileName = parts[parts.length - 1];
    const root = parts.length >= 2 ? parts[0] : 'solution';
    return {
      file,
      relativePath: `${root}/challenge_${challengeNumber}/${fileName}`,
    };
  });

  const challenge = {
    challengeNumber,
    folderName: `challenge_${challengeNumber}`,
    files: withChallengePaths,
  };

  return finalizeChallenges([challenge], 'midterm-flat');
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

/**
 * Parse a multi-student Main folder drop for Bulk Grading.
 * Paths are webkitRelativePath-style: Main/IRN_Name/... or Main/IRN_Name/challenge_1/...
 */

export const SUBMISSION_ROOT_PATTERN = /^(\d+)_([a-z0-9_\s]+)(_.*)?$/i;
export const CHALLENGE_FOLDER_PATTERN = /^challenge[_-]?\d+$/i;

/**
 * @param {Array<{ file: File, relativePath: string }>} entries
 * @param {'LAB'|'EXAM'} mode
 * @returns {{ accepted: Array, skipped: Array }}
 */
export function parseBulkMainFolder(entries, mode) {
  const skipped = [];
  if (!Array.isArray(entries) || entries.length === 0) {
    return { accepted: [], skipped: [{ path: '(empty)', reason: 'No files in drop' }] };
  }

  const normalized = entries
    .map((e) => ({
      file: e.file,
      relativePath: String(e.relativePath || e.file?.webkitRelativePath || e.file?.name || '').replace(/\\/g, '/'),
    }))
    .filter((e) => e.relativePath && !e.relativePath.split('/').some((p) => p === '.git' || p.startsWith('.git/')));

  // Strip a single shared Main prefix only when it is NOT already an IRN_Name root
  const firstSegs = normalized.map((e) => e.relativePath.split('/').filter(Boolean)[0]).filter(Boolean);
  const sharedTop = firstSegs.length && firstSegs.every((s) => s === firstSegs[0]) ? firstSegs[0] : null;
  const mainName = sharedTop && !SUBMISSION_ROOT_PATTERN.test(sharedTop) ? sharedTop : null;

  const byStudent = new Map();
  for (const entry of normalized) {
    const parts = entry.relativePath.split('/').filter(Boolean);
    if (parts.length < 2) {
      skipped.push({ path: entry.relativePath, reason: 'File not under a student folder' });
      continue;
    }
    let idx = 0;
    if (mainName && parts[0] === mainName) {
      idx = 1;
    }
    if (idx >= parts.length) {
      skipped.push({ path: entry.relativePath, reason: 'Empty path after Main' });
      continue;
    }
    const studentFolder = parts[idx];
    const rest = parts.slice(idx + 1);
    if (!byStudent.has(studentFolder)) {
      byStudent.set(studentFolder, []);
    }
    byStudent.get(studentFolder).push({
      file: entry.file,
      // Path relative to student folder (for later multipart / server remap)
      studentRelativePath: rest.join('/'),
      originalPath: entry.relativePath,
    });
  }

  const accepted = [];
  for (const [folderName, files] of byStudent.entries()) {
    const match = folderName.match(SUBMISSION_ROOT_PATTERN);
    if (!match) {
      skipped.push({ path: folderName, reason: 'Folder name must be IRN_Name (e.g. 2331200057_DOAN TUAN KIET)' });
      continue;
    }
    const studentId = match[1];
    const studentName = match[2].replace(/_/g, ' ').trim().toUpperCase();

    const relevant = files.filter((f) => {
      const name = f.studentRelativePath.toLowerCase();
      return name.endsWith('.java') || name.endsWith('.mmd');
    });

    if (relevant.length === 0) {
      skipped.push({ path: folderName, reason: 'No .java or .mmd files' });
      continue;
    }

    if (mode === 'EXAM') {
      const nestedChallenge = relevant.some((f) => {
        const top = f.studentRelativePath.split('/')[0];
        return CHALLENGE_FOLDER_PATTERN.test(top);
      });
      if (nestedChallenge) {
        skipped.push({ path: folderName, reason: 'Exam mode expects files directly under IRN_Name (no challenge_n folders)' });
        continue;
      }
      const flatOk = relevant.every((f) => !f.studentRelativePath.includes('/') || f.studentRelativePath.split('/').length === 1);
      // Allow one extra nesting only if not challenge_*; still require at least one file at student root depth ≤ 2
      const hasRootLevel = relevant.some((f) => f.studentRelativePath.split('/').length === 1);
      if (!hasRootLevel && !flatOk) {
        skipped.push({ path: folderName, reason: 'Exam mode: place .java/.mmd directly under the student folder' });
        continue;
      }
      accepted.push({
        folderName,
        studentId,
        studentName,
        mode: 'EXAM',
        files: relevant,
      });
      continue;
    }

    // LAB mode
    const byChallenge = new Map();
    let nonChallenge = 0;
    for (const f of relevant) {
      const segs = f.studentRelativePath.split('/').filter(Boolean);
      if (segs.length < 2 || !CHALLENGE_FOLDER_PATTERN.test(segs[0])) {
        nonChallenge += 1;
        continue;
      }
      const ch = segs[0].toLowerCase().replace(/-/g, '_');
      if (!byChallenge.has(ch)) byChallenge.set(ch, []);
      byChallenge.get(ch).push(f);
    }
    if (byChallenge.size === 0) {
      skipped.push({
        path: folderName,
        reason: nonChallenge
          ? 'Lab mode expects challenge_n folders under IRN_Name'
          : 'No challenge folders with .java/.mmd',
      });
      continue;
    }
    accepted.push({
      folderName,
      studentId,
      studentName,
      mode: 'LAB',
      files: relevant.filter((f) => {
        const top = f.studentRelativePath.split('/')[0];
        return CHALLENGE_FOLDER_PATTERN.test(top);
      }),
      challengeFolders: [...byChallenge.keys()],
    });
  }

  accepted.sort((a, b) => a.studentId.localeCompare(b.studentId, undefined, { numeric: true }));
  return { accepted, skipped };
}

export function modeMatchesLab(mode, challengeCount) {
  const n = Number(challengeCount) || 0;
  if (mode === 'EXAM') return n === 1;
  if (mode === 'LAB') return n >= 1;
  return false;
}

export function challengeCountForLab(lab) {
  if (!lab) return 0;
  if (Array.isArray(lab.challenges)) return lab.challenges.length;
  if (typeof lab.challengeCount === 'number') return lab.challengeCount;
  return 0;
}

/**
 * Within-batch plagiarism from file content hashes (SHA-256 hex strings).
 * Jaccard similarity > 0.90 flags (same threshold spirit as PlagiarismComparator).
 */

const HASH_FLAG_THRESHOLD = 0.9;

async function sha256Hex(buffer) {
  const digest = await crypto.subtle.digest('SHA-256', buffer);
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

async function hashesFromFiles(files) {
  const hashes = [];
  for (const item of files || []) {
    const path = (item.studentRelativePath || item.file?.name || '').toLowerCase();
    if (!path.endsWith('.java') && !path.endsWith('.mmd')) continue;
    const buf = await item.file.arrayBuffer();
    hashes.push(await sha256Hex(buf));
  }
  return hashes;
}

function jaccard(a, b) {
  if (!a.length && !b.length) return 0;
  const setA = new Set(a);
  const setB = new Set(b);
  let inter = 0;
  for (const h of setA) {
    if (setB.has(h)) inter += 1;
  }
  const union = setA.size + setB.size - inter;
  return union === 0 ? 0 : inter / union;
}

/**
 * @param {Array<{ studentId: string, fileHashes?: string[]|null, files?: Array }>} students
 * @returns {Record<string, { overlap: number, label: string }>}
 */
export function computeWithinBatchPlagiarism(students) {
  // Sync path when hashes already present; async hashing is done by caller via prepareHashes.
  const prepared = students.map((s) => ({
    studentId: s.studentId,
    hashes: Array.isArray(s.fileHashes) ? s.fileHashes : [],
  }));

  const out = {};
  for (const s of prepared) {
    out[s.studentId] = { overlap: null, label: '—' };
  }

  for (let i = 0; i < prepared.length; i += 1) {
    for (let j = i + 1; j < prepared.length; j += 1) {
      const left = prepared[i];
      const right = prepared[j];
      if (!left.hashes.length || !right.hashes.length) continue;
      const sim = jaccard(left.hashes, right.hashes);
      if (sim > HASH_FLAG_THRESHOLD) {
        const pct = Math.floor(sim * 100);
        const label = `${pct}%`;
        if (out[left.studentId].overlap == null || sim > out[left.studentId].overlap) {
          out[left.studentId] = { overlap: sim, label };
        }
        if (out[right.studentId].overlap == null || sim > out[right.studentId].overlap) {
          out[right.studentId] = { overlap: sim, label };
        }
      }
    }
  }
  return out;
}

/** Prefer server hashes; otherwise hash client files. */
export async function attachClientHashes(students) {
  const result = [];
  for (const s of students) {
    if (Array.isArray(s.fileHashes) && s.fileHashes.length) {
      result.push(s);
      continue;
    }
    const fileHashes = await hashesFromFiles(s.files);
    result.push({ ...s, fileHashes });
  }
  return result;
}

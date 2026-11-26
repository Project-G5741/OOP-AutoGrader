export const OOP_BLITZ_QUESTIONS_PER_MATCH = 8;
export const OOP_BLITZ_DAILY_CAP = 250;
export const OOP_BLITZ_MAX_RUNS = 3;
export const OOP_BLITZ_MAX_MATCH_POINTS = 100;

const JAVA_KEYWORDS = new Set([
  'public',
  'private',
  'protected',
  'class',
  'interface',
  'abstract',
  'extends',
  'implements',
  'static',
  'final',
  'void',
  'int',
  'double',
  'boolean',
  'new',
  'return',
  'this',
  'super',
  'package',
  'if',
  'else',
]);

/**
 * Single-pass Java line tokenizer for safe React rendering without HTML injection.
 * @param {string} line
 * @returns {Array<{ type: 'kw'|'tp'|'str'|'num'|'cmt'|'plain', text: string }>}
 */
export function tokenizeJavaLine(line) {
  const source = typeof line === 'string' ? line : '';
  const commentIndex = source.indexOf('//');
  const codePart = commentIndex >= 0 ? source.slice(0, commentIndex) : source;
  const commentPart = commentIndex >= 0 ? source.slice(commentIndex) : '';

  const tokens = [];
  const tokenPattern = /("(?:\\.|[^"])*"|\b\d+(?:\.\d+)?\b|\b[A-Za-z_][A-Za-z0-9_]*\b)/g;
  let lastIndex = 0;
  let match;

  while ((match = tokenPattern.exec(codePart)) !== null) {
    if (match.index > lastIndex) {
      tokens.push({ type: 'plain', text: codePart.slice(lastIndex, match.index) });
    }
    const word = match[0];
    if (word.startsWith('"')) {
      tokens.push({ type: 'str', text: word });
    } else if (/^\d/.test(word)) {
      tokens.push({ type: 'num', text: word });
    } else if (JAVA_KEYWORDS.has(word)) {
      tokens.push({ type: 'kw', text: word });
    } else if (/^[A-Z]/.test(word)) {
      tokens.push({ type: 'tp', text: word });
    } else {
      tokens.push({ type: 'plain', text: word });
    }
    lastIndex = tokenPattern.lastIndex;
  }

  if (lastIndex < codePart.length) {
    tokens.push({ type: 'plain', text: codePart.slice(lastIndex) });
  }
  if (commentPart) {
    tokens.push({ type: 'cmt', text: commentPart });
  }
  return tokens;
}

export function computeBlitzRunsRemaining(status) {
  const allowed = Number(status?.rewardedRunsAllowed ?? 1);
  const used = Number(status?.rewardedRunsUsed ?? 0);
  if (!Number.isFinite(allowed) || !Number.isFinite(used)) {
    return 0;
  }
  return Math.max(0, Math.floor(allowed) - Math.floor(used));
}

export function formatBlitzTimer(ms) {
  const safeMs = Number.isFinite(Number(ms)) ? Math.max(0, Number(ms)) : 0;
  return `${(safeMs / 1000).toFixed(1)}s`;
}

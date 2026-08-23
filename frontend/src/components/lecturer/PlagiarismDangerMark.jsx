const ROLE_ORIGINAL = 'ORIGINAL';
const ROLE_PLAGIARIZER = 'PLAGIARIZER';

function WarningBadgeIcon({
  className = '',
  label = 'Plagiarism suspected',
  tone = 'warning',
}) {
  const toneClass =
    tone === 'error'
      ? 'bg-error-bg text-error'
      : 'bg-warning-bg text-warning';
  return (
    <span
      className={`inline-flex size-4 shrink-0 items-center justify-center rounded-[3px] ${toneClass} ${className}`}
      aria-label={label}
      role="img"
      title={label}
    >
      <svg viewBox="0 0 16 16" width={11} height={11} aria-hidden="true" focusable="false">
        <path
          fill="currentColor"
          fillRule="evenodd"
          d="M7.13 1.9a1 1 0 0 1 1.74 0l6.35 11.55A1 1 0 0 1 14.35 15H1.65a1 1 0 0 1-.87-1.55L7.13 1.9zm.32 3.85a.55.55 0 0 1 1.1.08l-.28 3.25a.27.27 0 0 1-.54 0L7.45 5.83a.55.55 0 0 1 0-.08zM8 11.2a.75.75 0 1 1 0 1.5.75.75 0 0 1 0-1.5z"
        />
      </svg>
    </span>
  );
}

/**
 * Lecturer plagiarism mark.
 * - ORIGINAL (victim / earlier first submit): yellow warning
 * - PLAGIARIZER (later first submit): red warning
 * - `show` without role: generic lab presence (yellow)
 */
export default function PlagiarismDangerMark({ show, role = null, className = '' }) {
  const normalized = role ? String(role).toUpperCase() : null;
  if (normalized === ROLE_ORIGINAL) {
    return (
      <WarningBadgeIcon
        className={className}
        label="Victim of plagiarism"
        tone="warning"
      />
    );
  }
  if (normalized === ROLE_PLAGIARIZER || normalized === 'BOTH') {
    return (
      <WarningBadgeIcon
        className={className}
        label="Plagiarism suspected"
        tone="error"
      />
    );
  }
  if (!show) {
    return null;
  }
  return (
    <WarningBadgeIcon
      className={`ml-1 ${className}`}
      label="Plagiarism detected"
      tone="warning"
    />
  );
}

export function labHasPlagiarism(labId, flaggedLabIds) {
  if (!labId || !flaggedLabIds) {
    return false;
  }
  return flaggedLabIds.has(String(labId));
}

export function studentLabHasPlagiarism(studentId, labId, flaggedLabsByStudentId) {
  if (!studentId || !labId || !flaggedLabsByStudentId) {
    return false;
  }
  const labs = flaggedLabsByStudentId[studentId] ?? flaggedLabsByStudentId[String(studentId)] ?? [];
  return labs.some((id) => String(id) === String(labId));
}

/** Role string ORIGINAL | PLAGIARIZER, or null. */
export function studentLabPlagiarismRole(studentId, labId, rolesByStudentAndLab) {
  if (!studentId || !labId || !rolesByStudentAndLab) {
    return null;
  }
  const byLab =
    rolesByStudentAndLab[studentId] ??
    rolesByStudentAndLab[String(studentId)] ??
    null;
  if (!byLab) {
    return null;
  }
  const role = byLab[labId] ?? byLab[String(labId)];
  if (role == null) {
    return null;
  }
  const normalized = String(role).toUpperCase();
  if (normalized === 'BOTH') {
    return ROLE_PLAGIARIZER;
  }
  return normalized;
}

/** Max hash similarity 0–1 (or already 0–100) from flags payload; returns percent 0–100 or null. */
export function studentLabOverlapPercent(studentId, labId, overlapByStudentAndLab) {
  if (!studentId || !labId || !overlapByStudentAndLab) {
    return null;
  }
  const byLab =
    overlapByStudentAndLab[studentId] ??
    overlapByStudentAndLab[String(studentId)] ??
    null;
  if (!byLab) {
    return null;
  }
  const raw = byLab[labId] ?? byLab[String(labId)];
  if (raw == null || Number.isNaN(Number(raw))) {
    return null;
  }
  const value = Number(raw);
  return value <= 1 ? value * 100 : value;
}

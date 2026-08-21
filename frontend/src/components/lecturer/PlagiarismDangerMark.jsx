export default function PlagiarismDangerMark({ show, className = '' }) {
  if (!show) {
    return null;
  }
  return (
    <svg
      viewBox="0 0 16 16"
      width={16}
      height={16}
      className={`ml-1 size-4 shrink-0 text-error ${className}`}
      aria-label="Plagiarism detected"
      role="img"
    >
      <title>Plagiarism detected</title>
      <path
        fill="currentColor"
        fillRule="evenodd"
        d="M7.13 1.9a1 1 0 0 1 1.74 0l6.35 11.55A1 1 0 0 1 14.35 15H1.65a1 1 0 0 1-.87-1.55L7.13 1.9zm.32 3.85a.55.55 0 0 1 1.1.08l-.28 3.25a.27.27 0 0 1-.54 0L7.45 5.83a.55.55 0 0 1 0-.08zM8 11.2a.75.75 0 1 1 0 1.5.75.75 0 0 1 0-1.5z"
      />
    </svg>
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

/** @typedef {{ id: string, type: string, severity: string, labId?: string, title: string, message: string }} StudentNotification */

const SEVERITY_ORDER = { urgent: 0, warning: 1, info: 2, muted: 3 };

function formatDeadlineDate(deadlineDate) {
  if (!deadlineDate) return '';
  const [y, m, d] = String(deadlineDate).split('-');
  if (d && m && y) return `${d}/${m}/${y}`;
  return deadlineDate;
}

/**
 * Build in-app notifications from lab list + optional per-lab submission summaries.
 * @param {Array<{ id: string, name: string, deadlineDate?: string, urgencyState?: string }>} labs
 * @param {Record<string, { attempts?: number }>} labSummariesById
 * @returns {StudentNotification[]}
 */
export function buildStudentNotifications(labs = [], labSummariesById = {}) {
  /** @type {StudentNotification[]} */
  const items = [];

  for (const lab of labs) {
    const urgency = lab.urgencyState ?? 'NONE';
    const dateLabel = formatDeadlineDate(lab.deadlineDate);
    const summary = labSummariesById[lab.id];
    const hasSubmitted = summary != null ? (summary.attempts ?? 0) > 0 : false;

    if (urgency === 'URGENT') {
      items.push({
        id: `deadline-urgent-${lab.id}`,
        type: 'deadline',
        severity: 'urgent',
        labId: lab.id,
        title: `${lab.name} — deadline tomorrow`,
        message: `Due ${dateLabel} at 23:59 (Vietnam time). Submit before the deadline if you want this attempt counted for lecturer grading.`,
      });
    } else if (urgency === 'WARNING') {
      items.push({
        id: `deadline-warning-${lab.id}`,
        type: 'deadline',
        severity: 'warning',
        labId: lab.id,
        title: `${lab.name} — 3 days until deadline`,
        message: `Due ${dateLabel} at 23:59 (Vietnam time). Plan your submission before the cutoff.`,
      });
    } else if (urgency === 'EXPIRED') {
      items.push({
        id: `deadline-expired-${lab.id}`,
        type: 'deadline',
        severity: 'muted',
        labId: lab.id,
        title: `${lab.name} — deadline passed`,
        message: 'You can still upload for practice and history. Scores after the deadline are not shown to lecturers unless the deadline is extended.',
      });
    }

    if (hasSubmitted === false && lab.deadlineDate && urgency !== 'EXPIRED' && urgency !== 'NONE') {
      items.push({
        id: `no-submit-${lab.id}`,
        type: 'submission',
        severity: urgency === 'URGENT' ? 'urgent' : 'warning',
        labId: lab.id,
        title: `${lab.name} — no submission yet`,
        message: 'Upload your project before the deadline to have it counted toward lecturer grading views.',
      });
    }
  }

  items.sort((a, b) => (SEVERITY_ORDER[a.severity] ?? 9) - (SEVERITY_ORDER[b.severity] ?? 9));
  return items;
}

export function notificationSeverityClasses(severity) {
  switch (severity) {
    case 'urgent':
      return 'border-error/30 bg-error-bg/30';
    case 'warning':
      return 'border-warning/30 bg-warning-bg/30';
    case 'muted':
      return 'border-border bg-surface-secondary';
    default:
      return 'border-border bg-surface-secondary';
  }
}

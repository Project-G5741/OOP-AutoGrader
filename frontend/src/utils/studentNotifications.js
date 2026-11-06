import { deadlineDaysLeft } from '../theme/statusClasses';

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
 * @param {Date} [now]
 * @returns {StudentNotification[]}
 */
export function buildStudentNotifications(labs = [], labSummariesById = {}, now = new Date()) {
  /** @type {StudentNotification[]} */
  const items = [];

  for (const lab of labs) {
    const urgency = lab.urgencyState ?? 'NONE';
    const dateLabel = formatDeadlineDate(lab.deadlineDate);
    const summary = labSummariesById[lab.id];
    const hasSubmitted = summary != null ? (summary.attempts ?? 0) > 0 : false;
    const daysLeft = deadlineDaysLeft(lab.deadlineDate, now);

    const expired = daysLeft === 0 || urgency === 'EXPIRED';
    if (expired) {
      items.push({
        id: `deadline-expired-${lab.id}`,
        type: 'deadline',
        severity: 'muted',
        labId: lab.id,
        title: `${lab.name} — deadline passed`,
        message: 'You can still upload for practice and history. Scores after the deadline are not shown to lecturers unless the deadline is extended.',
      });
    } else if (daysLeft === 1 || daysLeft === 2 || daysLeft === 3) {
      const dueToday = daysLeft === 1;
      items.push({
        id: `deadline-countdown-${lab.id}`,
        type: 'deadline',
        severity: dueToday ? 'urgent' : 'warning',
        labId: lab.id,
        title: dueToday
          ? `${lab.name} — due today`
          : `${lab.name} — ${daysLeft} days until deadline`,
        message: `Due ${dateLabel} at 23:59 (Vietnam time). Submit before the deadline if you want this attempt counted for lecturer grading.`,
      });
    }

    if (!expired && hasSubmitted === false && (daysLeft === 1 || daysLeft === 2 || daysLeft === 3)) {
      items.push({
        id: `no-submit-${lab.id}`,
        type: 'submission',
        severity: daysLeft === 1 ? 'urgent' : 'warning',
        labId: lab.id,
        title: `${lab.name} — no submission yet`,
        message: 'Upload your project before the deadline to have it counted toward lecturer grading views.',
      });
    }
  }

  items.sort((a, b) => (SEVERITY_ORDER[a.severity] ?? 9) - (SEVERITY_ORDER[b.severity] ?? 9));
  return items;
}

/**
 * Lecturer replies on the author's resolved Feedback items.
 * @param {Array<{ id: string, itemType?: string, lecturerResponse?: string, responseAt?: string }>} replies
 * @returns {StudentNotification[]}
 */
export function buildFeedbackReplyNotifications(replies = []) {
  return replies
    .filter((reply) => reply?.id && String(reply.lecturerResponse || '').trim())
    .map((reply) => {
      const title = reply.itemType === 'CONTRIBUTION' ? 'Re: Contribution' : 'Re: Bug Report';
      return {
        id: `feedback-reply-${reply.id}-${reply.responseAt || ''}`,
        type: 'feedback-reply',
        severity: 'info',
        title,
        message: String(reply.lecturerResponse).trim(),
      };
    });
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

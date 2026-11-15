/**
 * Legacy leave-path helper. Intentionally a no-op: author replies must not
 * hard-delete feedback tickets on pagehide/logout (see feedback-reply-independence plan).
 */
export function setFeedbackReplyIdsSource() {
  // no-op — kept so older imports do not break
}

/**
 * @deprecated Leave-path hard-delete removed; forever-hide clears reply snapshots instead.
 */
export function consumeFeedbackRepliesOnLeave() {
  // no-op
}

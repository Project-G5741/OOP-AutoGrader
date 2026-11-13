const STUDENT_AREA_DELAY_MS = 5000;

let loggedInAt = 0;
let generation = 0;

/** Start the post-login wait. Lecturer-dashboard sign-in only. */
export function markLecturerLogin() {
  generation += 1;
  loggedInAt = Date.now();
}

/** Drop a pending wait so logout cannot still open the student area. */
export function clearLecturerLoginGate() {
  generation += 1;
  loggedInAt = 0;
}

export function studentAreaWaitMs(now = Date.now()) {
  if (!loggedInAt) return 0;
  return Math.max(0, STUDENT_AREA_DELAY_MS - (now - loggedInAt));
}

/**
 * After a lecturer-dashboard login, wait out the rest of the 5s burst window
 * before the student area may mount. Resolves false when logout cancels the wait.
 */
export async function awaitStudentAreaAfterLogin() {
  const seen = generation;
  const wait = studentAreaWaitMs();
  if (wait > 0) {
    await new Promise((resolve) => {
      setTimeout(resolve, wait);
    });
  }
  if (seen !== generation) return false;
  loggedInAt = 0;
  return true;
}

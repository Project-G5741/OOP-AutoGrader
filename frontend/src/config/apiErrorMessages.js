/**
 * User-facing API error copy for the web SPA.
 *
 * Edit strings here when you need different wording. HTTP mapping logic lives in
 * `src/utils/apiError.js` — pass a `context` string from each screen (e.g.
 * `solution-import`, `import`, `login`, `save`).
 *
 * Status keys under `auth` and `solutionImport` are numeric HTTP codes; `default`
 * is used when no status-specific line exists. Backend `message` may still override
 * for contexts listed in `backendMessageContexts` (see apiError.js).
 */

/** Shared fallbacks used across many contexts */
export const FRIENDLY = {
  SERVER_BUSY: 'Server Busy',
  LOGIN_WRONG: 'IRN or password is wrong',
  SESSION_EXPIRED: 'Session expired. Please sign in again.',
  SOMETHING_WRONG: 'Something went wrong. Please try again.',
  LOAD_FAILED: 'Unable to load data. Please try again.',
  SAVE_FAILED: 'Unable to save. Please try again.',
  DELETE_FAILED: 'Unable to delete. Please try again.',
  UPLOAD_FAILED: 'Upload failed. Please try again.',
};

export const ACCOUNT_INACTIVE = 'This account is inactive.';

/** Desktop practice pack import — network failure (context `import`) */
export const DESKTOP_IMPORT_NETWORK =
  'Practice backend is not running. Start OOP-AutoGrader-Practice and try again.';

/** Operational testcase dry-run / save when no backend message is available */
export const TESTCASE_CONFIG_FALLBACK =
  'Check the testcase configuration and try again.';

/**
 * Auth flows (`readFriendlyAuthError` / `toFriendlyError` with matching context).
 * Keys: login | google | setup | forgot-password | reset-password | change-password
 */
export const AUTH_ERROR_MESSAGES = {
  login: {
    401: FRIENDLY.LOGIN_WRONG,
    403: ACCOUNT_INACTIVE,
    default: FRIENDLY.SOMETHING_WRONG,
  },
  google: {
    401: 'Unable to sign in with Google. Please try again.',
    403: 'Unable to sign in with Google. Please try again.',
    423: ACCOUNT_INACTIVE,
    default: 'Unable to sign in with Google. Please try again.',
  },
  setup: {
    400: 'Unable to complete setup. Please check your details and try again.',
    409: 'Unable to complete setup. Please check your details and try again.',
    default: 'Unable to complete setup. Please check your details and try again.',
  },
  'forgot-password': {
    default: 'Unable to send reset email. Please try again.',
  },
  'reset-password': {
    400: 'This reset link is invalid or expired. Please request a new one.',
    404: 'This reset link is invalid or expired. Please request a new one.',
    default: 'This reset link is invalid or expired. Please request a new one.',
  },
  'change-password': {
    401: 'Current password is incorrect.',
    default: 'Unable to change password. Please try again.',
  },
};

/** Lecturer solution folder import (`context: 'solution-import'`) */
export const SOLUTION_IMPORT_ERROR_MESSAGES = {
  404: 'Lab not found. Refresh the page and select the lab again.',
  413: 'This solution folder is too large to upload (limit 50MB).',
  default: 'Unable to import solution. Please try again.',
};

/**
 * API contexts that may show the backend JSON `message` for 400/404/409/413/422.
 * Add a context name here when a new flow should surface server validation text.
 */
export const BACKEND_MESSAGE_CONTEXTS = [
  'testcase-dry-run',
  'testcase-save',
  'delete',
  'save',
  'import',
  'solution-import',
  'feedback',
];

/**
 * Client-thrown Error messages that should pass through unchanged in toFriendlyError.
 */
export const CLIENT_VALIDATION_PREFIXES = [
  'Please drop',
  'Invalid folder',
  'Missing lab',
  'You must be signed in',
  'No authentication token',
  'Google login did not receive',
  'Google login failed',
  'You are not authorized',
  'Could not find Student ID',
  'Not matched:',
  'Step ',
  'Testcase:',
  'Add at least',
  'Each ',
  'Complete the',
  'Check the testcase',
  'Add reference Java',
];

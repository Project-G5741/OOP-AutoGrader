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

const AUTH_ERROR_MESSAGES = {
  login: {
    401: FRIENDLY.LOGIN_WRONG,
    default: FRIENDLY.SOMETHING_WRONG,
  },
  google: {
    401: 'Unable to sign in with Google. Please try again.',
    403: 'Unable to sign in with Google. Please try again.',
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

const CLIENT_VALIDATION_PREFIXES = [
  'Please drop',
  'Invalid folder',
  'Missing lab',
  'You must be signed in',
  'No authentication token',
  'Google login did not receive',
  'Google login failed',
  'You are not authorized',
];

function collectFriendlyMessages() {
  const messages = new Set(Object.values(FRIENDLY));
  Object.values(AUTH_ERROR_MESSAGES).forEach((entry) => {
    Object.values(entry).forEach((message) => messages.add(message));
  });
  return messages;
}

const FRIENDLY_MESSAGE_SET = collectFriendlyMessages();

export function isNetworkError(error) {
  if (!error) return false;
  if (error instanceof TypeError) return true;
  const message = String(error.message || '');
  return (
    message === 'Failed to fetch' ||
    message.includes('NetworkError') ||
    (message.toLowerCase().includes('network') && message.toLowerCase().includes('fetch'))
  );
}

export function isServerBusyStatus(status) {
  return status === 0 || status >= 500 || status === 408 || status === 429;
}

export function isFriendlyMessage(message) {
  return FRIENDLY_MESSAGE_SET.has(message);
}

function isClientValidationMessage(message) {
  if (!message) return false;
  return CLIENT_VALIDATION_PREFIXES.some((prefix) => message.startsWith(prefix) || message.includes(prefix));
}

async function consumeResponseBody(response) {
  try {
    await response.text();
  } catch {
    // Ignore body read failures — response status still drives the user message.
  }
}

export async function readFriendlyApiError(response, context = 'read') {
  await consumeResponseBody(response);

  if (isServerBusyStatus(response.status)) {
    return FRIENDLY.SERVER_BUSY;
  }

  const authMessages = AUTH_ERROR_MESSAGES[context];
  if (authMessages) {
    const statusMessage = authMessages[response.status];
    if (statusMessage) {
      return statusMessage;
    }
    return authMessages.default || FRIENDLY.SOMETHING_WRONG;
  }

  if (response.status === 401 || response.status === 403) {
    return FRIENDLY.SESSION_EXPIRED;
  }

  switch (context) {
    case 'save':
      return FRIENDLY.SAVE_FAILED;
    case 'delete':
      return FRIENDLY.DELETE_FAILED;
    case 'upload':
      return FRIENDLY.UPLOAD_FAILED;
    case 'read':
    default:
      return FRIENDLY.SERVER_BUSY;
  }
}

export async function readApiErrorMessage(response, context = 'read') {
  return readFriendlyApiError(response, context);
}

export async function readFriendlyAuthError(response, context) {
  return readFriendlyApiError(response, context);
}

export async function friendlyLoadErrorFromResponse(response) {
  return readFriendlyApiError(response, 'read');
}

export function toFriendlyError(error, context = 'read') {
  if (isNetworkError(error)) {
    return FRIENDLY.SERVER_BUSY;
  }

  const message = error?.message;
  if (message && (isFriendlyMessage(message) || isClientValidationMessage(message))) {
    return message;
  }

  const authMessages = AUTH_ERROR_MESSAGES[context];
  if (authMessages) {
    return authMessages.default || FRIENDLY.SOMETHING_WRONG;
  }

  switch (context) {
    case 'save':
      return FRIENDLY.SAVE_FAILED;
    case 'delete':
      return FRIENDLY.DELETE_FAILED;
    case 'upload':
      return FRIENDLY.UPLOAD_FAILED;
    case 'read':
    default:
      return FRIENDLY.SERVER_BUSY;
  }
}

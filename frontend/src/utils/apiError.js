import {
  ACCOUNT_INACTIVE,
  AUTH_ERROR_MESSAGES,
  BACKEND_MESSAGE_CONTEXTS,
  CLIENT_VALIDATION_PREFIXES,
  DESKTOP_IMPORT_NETWORK,
  FRIENDLY,
  SOLUTION_IMPORT_ERROR_MESSAGES,
  TESTCASE_CONFIG_FALLBACK,
} from '../config/apiErrorMessages';

export { FRIENDLY } from '../config/apiErrorMessages';

const BACKEND_MESSAGE_CONTEXT_SET = new Set(BACKEND_MESSAGE_CONTEXTS);

function collectFriendlyMessages() {
  const messages = new Set(Object.values(FRIENDLY));
  messages.add(ACCOUNT_INACTIVE);
  messages.add(DESKTOP_IMPORT_NETWORK);
  messages.add(TESTCASE_CONFIG_FALLBACK);
  Object.values(SOLUTION_IMPORT_ERROR_MESSAGES).forEach((message) => messages.add(message));
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
  // 429 is handled by the rate-limit jail navigation; do not treat it as toast-only busy.
  return status === 0 || status >= 500 || status === 408;
}

export function isFriendlyMessage(message) {
  return FRIENDLY_MESSAGE_SET.has(message);
}

function isClientValidationMessage(message) {
  if (!message) return false;
  return CLIENT_VALIDATION_PREFIXES.some((prefix) => message.startsWith(prefix) || message.includes(prefix));
}

async function readBackendErrorMessage(response) {
  try {
    const text = await response.text();
    if (!text) return null;
    const data = JSON.parse(text);
    const message = data?.message;
    if (typeof message !== 'string') return null;
    const trimmed = message.trim();
    return trimmed.length > 0 && trimmed.length <= 500 ? trimmed : null;
  } catch {
    return null;
  }
}

export async function readFriendlyApiError(response, context = 'read') {
  const backendMessage = await readBackendErrorMessage(response);

  if (
    context === 'download'
    && backendMessage
    && (response.status === 403 || response.status === 404 || response.status === 503)
  ) {
    return backendMessage;
  }

  if (isServerBusyStatus(response.status)) {
    return FRIENDLY.SERVER_BUSY;
  }

  if (
    (response.status === 400
      || response.status === 404
      || response.status === 413
      || response.status === 422
      || response.status === 409)
    && backendMessage
    && BACKEND_MESSAGE_CONTEXT_SET.has(context)
  ) {
    return backendMessage;
  }

  if (context === 'solution-import') {
    if (response.status === 401) {
      return FRIENDLY.SESSION_EXPIRED;
    }
    if (response.status === 403) {
      return FRIENDLY.LOAD_FAILED;
    }
    const statusMessage = SOLUTION_IMPORT_ERROR_MESSAGES[response.status];
    if (statusMessage) {
      return statusMessage;
    }
    return SOLUTION_IMPORT_ERROR_MESSAGES.default;
  }

  const authMessages = AUTH_ERROR_MESSAGES[context];
  if (authMessages) {
    const statusMessage = authMessages[response.status];
    if (statusMessage) {
      return statusMessage;
    }
    return authMessages.default || FRIENDLY.SOMETHING_WRONG;
  }

  if (response.status === 401) {
    return FRIENDLY.SESSION_EXPIRED;
  }

  if (response.status === 403) {
    return FRIENDLY.LOAD_FAILED;
  }

  switch (context) {
    case 'testcase-dry-run':
    case 'testcase-save':
      return backendMessage || TESTCASE_CONFIG_FALLBACK;
    case 'save':
      return FRIENDLY.SAVE_FAILED;
    case 'delete':
      if (response.status === 409 && backendMessage) {
        return backendMessage;
      }
      return FRIENDLY.DELETE_FAILED;
    case 'upload':
      return FRIENDLY.UPLOAD_FAILED;
    case 'download':
      return FRIENDLY.SERVER_BUSY;
    case 'import':
      return FRIENDLY.SOMETHING_WRONG;
    case 'solution-import':
      return SOLUTION_IMPORT_ERROR_MESSAGES.default;
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
    if (context === 'import') {
      return DESKTOP_IMPORT_NETWORK;
    }
    return FRIENDLY.SERVER_BUSY;
  }

  const message = error?.message;
  if (message && (isFriendlyMessage(message) || isClientValidationMessage(message))) {
    return message;
  }

  // Already resolved by readFriendlyApiError (or a client-thrown Error with that text).
  if (message && (BACKEND_MESSAGE_CONTEXT_SET.has(context) || context === 'download')) {
    return message;
  }

  const authMessages = AUTH_ERROR_MESSAGES[context];
  if (authMessages) {
    return authMessages.default || FRIENDLY.SOMETHING_WRONG;
  }

  switch (context) {
    case 'testcase-dry-run':
    case 'testcase-save':
      return TESTCASE_CONFIG_FALLBACK;
    case 'save':
      return FRIENDLY.SAVE_FAILED;
    case 'delete':
      return FRIENDLY.DELETE_FAILED;
    case 'upload':
      return FRIENDLY.UPLOAD_FAILED;
    case 'download':
      return FRIENDLY.SERVER_BUSY;
    case 'import':
      return FRIENDLY.SOMETHING_WRONG;
    case 'solution-import':
      return SOLUTION_IMPORT_ERROR_MESSAGES.default;
    case 'read':
    default:
      return FRIENDLY.SERVER_BUSY;
  }
}

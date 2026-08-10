const AUTH_ERROR_MESSAGES = {
  login: {
    401: 'Wrong username or password',
    default: 'Unable to sign in. Please try again.',
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

async function readResponseBody(response) {
  const text = await response.text();
  if (!text) {
    return { text: '', parsed: null };
  }
  try {
    return { text: text.trim(), parsed: JSON.parse(text) };
  } catch {
    return { text: text.trim(), parsed: null };
  }
}

export async function readApiErrorMessage(response, fallback = 'Request failed') {
  const { text, parsed } = await readResponseBody(response);
  if (!text) {
    return fallback;
  }
  if (parsed) {
    if (typeof parsed.message === 'string' && parsed.message.trim()) {
      return parsed.message.trim();
    }
    if (typeof parsed.error === 'string' && parsed.error.trim()) {
      return parsed.error.trim();
    }
    if (typeof parsed.detail === 'string' && parsed.detail.trim()) {
      return parsed.detail.trim();
    }
  }
  return text;
}

export async function readFriendlyAuthError(response, context) {
  const messages = AUTH_ERROR_MESSAGES[context];
  if (!messages) {
    return 'Something went wrong. Please try again.';
  }

  await readResponseBody(response);

  const statusMessage = messages[response.status];
  if (statusMessage) {
    return statusMessage;
  }

  return messages.default || 'Something went wrong. Please try again.';
}

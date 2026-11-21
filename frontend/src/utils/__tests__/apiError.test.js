import { describe, expect, it } from 'vitest';
import { FRIENDLY, readFriendlyApiError, toFriendlyError } from '../apiError.js';
import { STREAK_PET_ERROR_MESSAGES } from '../../config/apiErrorMessages.js';

function jsonResponse(status, body, headers = {}) {
  return {
    status,
    headers: {
      get(name) {
        return headers[name] ?? headers[name?.toLowerCase()] ?? null;
      },
    },
    async text() {
      return JSON.stringify(body);
    },
  };
}

describe('apiError', () => {
  it('maps 500 read context to friendly busy, not raw backend message', async () => {
    const response = jsonResponse(500, { message: 'SQLException: detail leak' });
    const message = await readFriendlyApiError(response, 'read');
    expect(message).toBe(FRIENDLY.SERVER_BUSY);
    expect(message).not.toContain('SQLException');
  });

  it('maps network TypeError to friendly busy', () => {
    expect(toFriendlyError(new TypeError('Failed to fetch'), 'read')).toBe(FRIENDLY.SERVER_BUSY);
  });

  it('maps 401 to session expired for default read', async () => {
    const response = jsonResponse(401, { message: 'Unauthorized' });
    expect(await readFriendlyApiError(response, 'read')).toBe(FRIENDLY.SESSION_EXPIRED);
  });

  it('maps streak-pet 502 to friendly busy but 404 to streak default', async () => {
    expect(await readFriendlyApiError(jsonResponse(502, {}), 'streak-pet')).toBe(FRIENDLY.SERVER_BUSY);
    expect(await readFriendlyApiError(jsonResponse(404, {}), 'streak-pet')).toBe(
      STREAK_PET_ERROR_MESSAGES.default,
    );
  });

  it('maps streak-pet network failure to streak default, not generic busy', () => {
    expect(toFriendlyError(new TypeError('Failed to fetch'), 'streak-pet')).toBe(
      STREAK_PET_ERROR_MESSAGES.default,
    );
  });
});

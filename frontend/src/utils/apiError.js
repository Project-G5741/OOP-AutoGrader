export async function readApiErrorMessage(response, fallback = 'Request failed') {
  const text = await response.text();
  if (!text) {
    return fallback;
  }
  try {
    const data = JSON.parse(text);
    if (typeof data.message === 'string' && data.message.trim()) {
      return data.message.trim();
    }
    if (typeof data.error === 'string' && data.error.trim()) {
      return data.error.trim();
    }
    if (typeof data.detail === 'string' && data.detail.trim()) {
      return data.detail.trim();
    }
  } catch {
    // Plain-text error body
  }
  return text.trim();
}

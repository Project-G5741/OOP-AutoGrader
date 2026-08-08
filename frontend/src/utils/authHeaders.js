export function authHeaders(extra = {}) {
  const token = sessionStorage.getItem('accessToken');
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
}

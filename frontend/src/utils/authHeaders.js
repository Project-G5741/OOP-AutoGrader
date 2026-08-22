export function authHeaders(extra = {}) {
  const token = sessionStorage.getItem('accessToken');
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
}

/** Clear stored session and return to login (e.g. expired JWT after backend restart). */
export function clearSessionAndRedirectToLogin() {
  sessionStorage.removeItem('accessToken');
  sessionStorage.removeItem('user');
  localStorage.removeItem('token');
  const path = window.location.pathname;
  if (path !== '/' && path !== '') {
    window.location.assign('/');
  }
}

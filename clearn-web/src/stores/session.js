const STORAGE_KEYS = {
  token: 'clearn_token',
  username: 'clearn_username',
  role: 'clearn_role'
};

export function defaultRouteForRole(role) {
  if (role === 'ADMIN') {
    return '/admin/problems';
  }
  if (role === 'STUDENT') {
    return '/student/practice';
  }
  return '/login';
}

export function getSession() {
  return {
    token: localStorage.getItem(STORAGE_KEYS.token) || '',
    username: localStorage.getItem(STORAGE_KEYS.username) || '',
    role: localStorage.getItem(STORAGE_KEYS.role) || ''
  };
}

export function saveSession(session) {
  localStorage.setItem(STORAGE_KEYS.token, session.token || '');
  localStorage.setItem(STORAGE_KEYS.username, session.username || '');
  localStorage.setItem(STORAGE_KEYS.role, session.role || '');
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEYS.token);
  localStorage.removeItem(STORAGE_KEYS.username);
  localStorage.removeItem(STORAGE_KEYS.role);
}

export function isAuthenticated() {
  return Boolean(getSession().token);
}

export function isAdmin() {
  return getSession().role === 'ADMIN';
}

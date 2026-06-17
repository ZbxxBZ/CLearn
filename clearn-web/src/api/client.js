import { getSession } from '../stores/session';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const hasBody = options.body !== undefined && options.body !== null;
  if (hasBody && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const { token } = getSession();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(payload?.message || response.statusText || `HTTP ${response.status}`);
  }
  return payload?.data ?? payload;
}

export function toJsonBody(value) {
  return JSON.stringify(value);
}

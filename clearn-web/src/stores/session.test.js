import { afterEach, describe, expect, it } from 'vitest';
import { clearSession, defaultRouteForRole, getSession, saveSession } from './session';

const memoryStorage = new Map();

globalThis.localStorage = {
  getItem: (key) => memoryStorage.get(key) || null,
  setItem: (key, value) => memoryStorage.set(key, String(value)),
  removeItem: (key) => memoryStorage.delete(key),
  clear: () => memoryStorage.clear()
};

afterEach(() => {
  localStorage.clear();
});

describe('session store', () => {
  it('routes admins and students to their own portals', () => {
    expect(defaultRouteForRole('ADMIN')).toBe('/admin/problems');
    expect(defaultRouteForRole('STUDENT')).toBe('/student/practice');
    expect(defaultRouteForRole('')).toBe('/login');
  });

  it('persists and clears login state', () => {
    saveSession({ token: 't', username: 'admin', role: 'ADMIN' });

    expect(getSession()).toEqual({ token: 't', username: 'admin', role: 'ADMIN' });

    clearSession();

    expect(getSession()).toEqual({ token: '', username: '', role: '' });
  });
});

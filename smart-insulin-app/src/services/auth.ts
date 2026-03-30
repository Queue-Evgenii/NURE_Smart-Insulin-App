import { ref } from 'vue';
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from './token';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const isAuthenticated = ref(false);

export async function initAuth(): Promise<void> {
  const token = await getAccessToken();
  isAuthenticated.value = !!token;
}

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

interface AuthError {
  message: string;
}

export async function login(email: string, password: string): Promise<{ ok: true } | { ok: false; error: string }> {
  try {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    if (!res.ok) {
      const text = await res.text();
      let msg = 'Login failed';
      try {
        const body = JSON.parse(text) as AuthError;
        msg = body.message || msg;
      } catch { /* use default */ }
      return { ok: false, error: msg };
    }

    const data: TokenResponse = await res.json();
    await setTokens(data.accessToken, data.refreshToken);
    isAuthenticated.value = true;
    return { ok: true };
  } catch {
    return { ok: false, error: 'Network error. Please try again.' };
  }
}

export async function register(
  email: string,
  password: string,
  fullName: string,
  diabetesType?: number,
): Promise<{ ok: true } | { ok: false; error: string }> {
  try {
    const body: Record<string, unknown> = { email, password, fullName };
    if (diabetesType != null) body.diabetesType = diabetesType;

    const res = await fetch(`${API_BASE}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const text = await res.text();
      let msg = 'Registration failed';
      try {
        const parsed = JSON.parse(text) as AuthError;
        msg = parsed.message || msg;
      } catch { /* use default */ }
      return { ok: false, error: msg };
    }

    const data: TokenResponse = await res.json();
    await setTokens(data.accessToken, data.refreshToken);
    isAuthenticated.value = true;
    return { ok: true };
  } catch {
    return { ok: false, error: 'Network error. Please try again.' };
  }
}

export async function logout(): Promise<void> {
  try {
    const refreshToken = await getRefreshToken();
    if (refreshToken) {
      await fetch(`${API_BASE}/api/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });
    }
  } catch { /* best effort */ }

  await clearTokens();
  isAuthenticated.value = false;
}

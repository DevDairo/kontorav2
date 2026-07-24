const AUTH_TOKEN_KEY = "kontora.auth.token";

function getSessionStorage() {
  if (typeof window === "undefined") {
    return null;
  }

  return window.sessionStorage;
}

export function readAuthToken() {
  try {
    return getSessionStorage()?.getItem(AUTH_TOKEN_KEY) ?? null;
  } catch {
    return null;
  }
}

export function saveAuthToken(token: string) {
  getSessionStorage()?.setItem(AUTH_TOKEN_KEY, token);
}

export function clearAuthToken() {
  getSessionStorage()?.removeItem(AUTH_TOKEN_KEY);
}

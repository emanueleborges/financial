const ACCESS_KEY = "fh_access_token";
const REFRESH_KEY = "fh_refresh_token";

export type JwtPayload = {
  sub: string;
  userId?: string;
  email?: string;
  type?: string;
  exp?: number;
};

export function saveTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(ACCESS_KEY, accessToken);
  localStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(ACCESS_KEY);
}

export function decodeJwt(token: string): JwtPayload | null {
  try {
    const part = token.split(".")[1];
    if (!part) return null;
    const json = atob(part.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

/** Sessão baseada em CPF/CNPJ (JWT sub). */
export function getSession(): {
  token: string;
  document: string;
  userId?: string;
  email?: string;
} | null {
  const token = getAccessToken();
  if (!token) return null;
  const payload = decodeJwt(token);
  if (!payload?.sub) return null;
  if (payload.exp && payload.exp * 1000 < Date.now()) {
    clearTokens();
    return null;
  }
  return {
    token,
    document: payload.sub,
    userId: payload.userId,
    email: payload.email,
  };
}

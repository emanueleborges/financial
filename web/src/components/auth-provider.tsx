"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { clearTokens, getSession, saveTokens } from "@/lib/auth";
import { login as apiLogin } from "@/lib/api";

type Session = {
  token: string;
  document: string;
  userId?: string;
  email?: string;
};

type AuthContextValue = {
  session: Session | null;
  ready: boolean;
  login: (document: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    setSession(getSession());
    setReady(true);
  }, []);

  const login = useCallback(async (document: string, password: string) => {
    const res = await apiLogin(document, password);
    saveTokens(res.accessToken, res.refreshToken);
    setSession(getSession());
  }, []);

  const logout = useCallback(() => {
    clearTokens();
    setSession(null);
  }, []);

  const value = useMemo(
    () => ({ session, ready, login, logout }),
    [session, ready, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth deve ser usado dentro de AuthProvider");
  return ctx;
}

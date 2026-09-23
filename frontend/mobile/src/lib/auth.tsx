import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, type AuthResponse } from "./api";
import { promptBiometric } from "./biometrics";
import { clearSession, loadSession, loadUnlockSecrets, saveSession, type Session } from "./session";

type AuthContextValue = {
  session: Session | null;
  ready: boolean;
  loginWithPassword: (document: string, password: string, enrollBiometric?: boolean) => Promise<void>;
  loginWithBiometric: () => Promise<void>;
  loginWithFace: (document: string, password?: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

async function persist(auth: AuthResponse, document: string, enrollBiometric: boolean) {
  let biometricEnabled = false;
  if (enrollBiometric) {
    biometricEnabled = await promptBiometric("Confirme Face ID ou digital para esta conta");
  }
  await saveSession(auth, document, biometricEnabled);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    void loadSession().then((s) => {
      setSession(s);
      setReady(true);
    });
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      ready,
      async loginWithPassword(document, password, enrollBiometric = false) {
        const digits = document.replace(/\D/g, "");
        const auth = await api.login(digits, password);
        await persist(auth, digits, enrollBiometric);
        setSession(await loadSession());
      },
      async loginWithBiometric() {
        const current = await loadSession();
        if (!current?.biometricEnabled || !current.refreshToken) {
          throw new Error("Biometria ainda não foi cadastrada nesta conta neste aparelho.");
        }
        const ok = await promptBiometric("Entre com Face ID ou impressão digital");
        if (!ok) throw new Error("Biometria cancelada ou não reconhecida.");
        const auth = await api.refresh(current.refreshToken);
        await saveSession(auth, current.document, true);
        setSession(await loadSession());
      },
      async loginWithFace(document, password) {
        const digits = document.replace(/\D/g, "");
        const unlock = await loadUnlockSecrets();
        if (unlock?.refreshToken && unlock.document === digits) {
          const auth = await api.refresh(unlock.refreshToken);
          await saveSession(auth, digits, true);
          setSession(await loadSession());
          return;
        }
        if (!password) {
          throw new Error("Rosto reconhecido. Informe a senha só neste primeiro acesso.");
        }
        const auth = await api.login(digits, password);
        await saveSession(auth, digits, true);
        setSession(await loadSession());
      },
      async logout() {
        await clearSession();
        setSession(null);
      },
    }),
    [session, ready]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth fora do AuthProvider");
  return ctx;
}

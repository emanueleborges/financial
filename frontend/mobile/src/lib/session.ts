import * as SecureStore from "expo-secure-store";
import type { AuthResponse } from "./api";

const ACCESS = "fh.accessToken";
const REFRESH = "fh.refreshToken";
const DOCUMENT = "fh.document";
const BIOMETRIC = "fh.biometricEnabled";
const REMEMBERED = "fh.rememberedDocuments";

export type Session = {
  token: string;
  refreshToken: string;
  document: string;
  biometricEnabled: boolean;
};

export async function loadSession(): Promise<Session | null> {
  const [token, refreshToken, document, biometric] = await Promise.all([
    SecureStore.getItemAsync(ACCESS),
    SecureStore.getItemAsync(REFRESH),
    SecureStore.getItemAsync(DOCUMENT),
    SecureStore.getItemAsync(BIOMETRIC),
  ]);
  if (!token || !refreshToken || !document) return null;
  return { token, refreshToken, document, biometricEnabled: biometric === "1" };
}

export async function saveSession(auth: AuthResponse, document: string, biometricEnabled: boolean) {
  const digits = document.replace(/\D/g, "");
  await Promise.all([
    SecureStore.setItemAsync(ACCESS, auth.accessToken),
    SecureStore.setItemAsync(REFRESH, auth.refreshToken),
    SecureStore.setItemAsync(DOCUMENT, digits),
    SecureStore.setItemAsync(BIOMETRIC, biometricEnabled ? "1" : "0"),
    rememberDocument(digits),
  ]);
}

export async function clearSession() {
  await SecureStore.deleteItemAsync(ACCESS);
}

export async function loadUnlockSecrets() {
  const [refreshToken, document] = await Promise.all([
    SecureStore.getItemAsync(REFRESH),
    SecureStore.getItemAsync(DOCUMENT),
  ]);
  if (!refreshToken || !document) return null;
  return { refreshToken, document };
}

export async function hasBiometricEnrollment() {
  return (await SecureStore.getItemAsync(BIOMETRIC)) === "1";
}

export async function storedRefreshToken() {
  return SecureStore.getItemAsync(REFRESH);
}

export async function rememberDocument(document: string) {
  const digits = document.replace(/\D/g, "");
  if (!digits) return;
  const current = await listRememberedDocuments();
  const next = [digits, ...current.filter((item) => item !== digits)];
  await SecureStore.setItemAsync(REMEMBERED, JSON.stringify(next));
}

export async function listRememberedDocuments(): Promise<string[]> {
  const raw = await SecureStore.getItemAsync(REMEMBERED);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as unknown;
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === "string") : [];
  } catch {
    return [];
  }
}

export async function lastUsedDocument() {
  return SecureStore.getItemAsync(DOCUMENT);
}

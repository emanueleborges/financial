import Constants from "expo-constants";
import { Platform } from "react-native";

function extractLanHost(raw?: string | null): string | null {
  if (!raw) return null;
  const host = raw.replace(/^[a-z]+:\/\//, "").split("/")[0]?.split(":")[0];
  if (!host || host === "localhost" || host === "127.0.0.1" || host === "10.0.2.2") {
    return null;
  }
  return host;
}

function metroLanHost(): string | null {
  const expoGo = Constants.expoGoConfig as { debuggerHost?: string } | null;
  return (
    extractLanHost(Constants.expoConfig?.hostUri) ||
    extractLanHost(expoGo?.debuggerHost) ||
    extractLanHost(Constants.linkingUri)
  );
}

function isAndroidEmulator() {
  if (Platform.OS !== "android") return false;
  const c = Platform.constants as {
    Brand?: string;
    Manufacturer?: string;
    Model?: string;
    Fingerprint?: string;
  };
  const blob = `${c.Brand ?? ""} ${c.Manufacturer ?? ""} ${c.Model ?? ""} ${c.Fingerprint ?? ""}`.toLowerCase();
  return (
    blob.includes("generic") ||
    blob.includes("sdk") ||
    blob.includes("emulator") ||
    blob.includes("goldfish") ||
    blob.includes("ranchu") ||
    blob.includes("gphone")
  );
}

function resolveApiUrl() {
  if (process.env.EXPO_PUBLIC_API_URL) {
    return process.env.EXPO_PUBLIC_API_URL;
  }
  if (isAndroidEmulator()) {
    return "http://10.0.2.2:8080";
  }
  const lan = metroLanHost();
  if (lan) {
    return `http://${lan}:8080`;
  }
  if (Platform.OS === "android") {
    return "http://10.0.2.2:8080";
  }
  return "http://localhost:8080";
}

export const API_URL = resolveApiUrl();

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
};

export type UserResponse = {
  name: string;
  email: string;
  document: string;
  balance: number;
  dailyLimit: number;
};

export type BalanceResponse = { document: string; balance: number };

export type TransactionResponse = {
  id: string;
  payerDocument: string | null;
  payeeDocument: string | null;
  payerName: string | null;
  payeeName: string | null;
  amount: number;
  status: string;
  type: string;
  createdAt: string;
};

export type StatementResponse = {
  currentBalance: number;
  entries: { transaction: TransactionResponse; signedAmount: number; balanceAfter: number | null }[];
};

export class ApiClientError extends Error {
  status: number;
  fields: Record<string, string>;
  constructor(status: number, message: string, fields: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.fields = fields;
  }
}

async function request<T>(path: string, options: RequestInit & { token?: string } = {}): Promise<T> {
  const { token, headers, ...rest } = options;
  const res = await fetch(`${API_URL}${path}`, {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  });
  if (!res.ok) {
    let message = res.statusText;
    let fields: Record<string, string> = {};
    try {
      const body = await res.json();
      message = body.message || message;
      fields = body.fields ?? {};
    } catch {
      /* ignore */
    }
    throw new ApiClientError(res.status, message, fields);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  createUser: (body: {
    name: string;
    email: string;
    document: string;
    password: string;
    initialBalance: number;
  }) => request<UserResponse>("/api/v1/users", { method: "POST", body: JSON.stringify(body) }),

  login: (document: string, password: string) =>
    request<AuthResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ document, password }),
    }),

  refresh: (refreshToken: string) =>
    request<AuthResponse>("/api/v1/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    }),

  getUser: (document: string, token: string) =>
    request<UserResponse>(`/api/v1/users/${document}`, { token }),

  getBalance: (document: string, token: string) =>
    request<BalanceResponse>(`/api/v1/users/${document}/balance`, { token }),

  listTransactions: (document: string, token: string) =>
    request<StatementResponse>(`/api/v1/users/${document}/transactions?limit=50`, { token }),

  transfer: (
    body: { payerDocument: string; payeeDocument: string; amount: number; password: string },
    token: string,
    idempotencyKey: string
  ) =>
    request<TransactionResponse>("/api/v1/transactions", {
      method: "POST",
      token,
      headers: { "Idempotency-Key": idempotencyKey },
      body: JSON.stringify(body),
    }),

  reverse: (transactionId: string, reason: string, token: string) =>
    request<TransactionResponse>("/api/v1/transactions/reverse", {
      method: "POST",
      token,
      body: JSON.stringify({ transactionId, reason }),
    }),
};

export type ApiError = {
  code: string;
  message: string;
  timestamp?: string;
  path?: string;
  fields?: Record<string, string> | null;
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
};

export type UserResponse = {
  id: string;
  name: string;
  email: string;
  document: string;
  balance: number;
  status: string;
  dailyLimit: number;
  createdAt: string;
};

export type BalanceResponse = {
  document: string;
  balance: number;
  version: number;
};

export type TransactionResponse = {
  id: string;
  payerId: string;
  payeeId: string;
  payerDocument: string | null;
  payeeDocument: string | null;
  payerName: string | null;
  payeeName: string | null;
  amount: number;
  status: string;
  type: string;
  failureReason: string | null;
  originalTxId: string | null;
  createdAt: string;
  completedAt: string | null;
};

export type StatementEntryResponse = {
  transaction: TransactionResponse;
  signedAmount: number;
  balanceAfter: number | null;
};

export type StatementResponse = {
  document: string;
  currentBalance: number;
  entries: StatementEntryResponse[];
};

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiClientError extends Error {
  code: string;
  status: number;
  fields: Record<string, string>;

  constructor(status: number, error: ApiError) {
    super(error.message || "Erro na API");
    this.code = error.code || "UNKNOWN";
    this.status = status;
    this.fields = error.fields ?? {};
  }
}

async function parseError(res: Response): Promise<never> {
  let body: ApiError = { code: "HTTP_ERROR", message: res.statusText };
  try {
    body = await res.json();
  } catch {
    /* ignore */
  }
  throw new ApiClientError(res.status, body);
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit & { token?: string } = {}
): Promise<T> {
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
    await parseError(res);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return res.json() as Promise<T>;
}

export function createUser(body: {
  name: string;
  email: string;
  document: string;
  password: string;
  initialBalance: number;
}) {
  return apiFetch<UserResponse>("/api/v1/users", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function login(document: string, password: string) {
  return apiFetch<AuthResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ document, password }),
  });
}

export function getUser(document: string, token: string) {
  return apiFetch<UserResponse>(`/api/v1/users/${document}`, { token });
}

export function getBalance(document: string, token: string) {
  return apiFetch<BalanceResponse>(`/api/v1/users/${document}/balance`, { token });
}

export function listUserTransactions(document: string, token: string, limit = 50) {
  return apiFetch<StatementResponse>(
    `/api/v1/users/${document}/transactions?limit=${limit}`,
    { token }
  );
}

export function transfer(
  body: {
    payerDocument: string;
    payeeDocument: string;
    amount: number;
  },
  token: string,
  idempotencyKey: string
) {
  return apiFetch<TransactionResponse>("/api/v1/transactions", {
    method: "POST",
    token,
    headers: { "Idempotency-Key": idempotencyKey },
    body: JSON.stringify(body),
  });
}

export function getTransaction(id: string, token: string) {
  return apiFetch<TransactionResponse>(`/api/v1/transactions/${id}`, { token });
}

export function reverseTransaction(
  body: { transactionId: string; reason: string },
  token: string
) {
  return apiFetch<TransactionResponse>("/api/v1/transactions/reverse", {
    method: "POST",
    token,
    body: JSON.stringify(body),
  });
}

export async function downloadReceipt(transactionId: string, token: string): Promise<Blob> {
  const res = await fetch(`${API_URL}/api/v1/transactions/${transactionId}/receipt`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!res.ok) {
    let body: ApiError = { code: "HTTP_ERROR", message: res.statusText };
    try {
      body = await res.json();
    } catch {
      /* ignore */
    }
    throw new ApiClientError(res.status, body);
  }

  return res.blob();
}

export async function downloadStatementPdf(
  document: string,
  token: string,
  limit = 50
): Promise<Blob> {
  const res = await fetch(
    `${API_URL}/api/v1/users/${document}/transactions/export?limit=${limit}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  if (!res.ok) {
    let body: ApiError = { code: "HTTP_ERROR", message: res.statusText };
    try {
      body = await res.json();
    } catch {
      /* ignore */
    }
    throw new ApiClientError(res.status, body);
  }

  return res.blob();
}

export function triggerBlobDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

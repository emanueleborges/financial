export type ApiError = {
  code: string;
  message: string;
  timestamp?: string;
  path?: string;
  fields?: Record<string, string> | null;
};

export class ApiClientError extends Error {
  code: string;
  status: number;
  fields: Record<string, string>;

  constructor(status: number, error: ApiError) {
    super(error.message || 'Erro na API');
    this.name = 'ApiClientError';
    this.code = error.code || 'UNKNOWN';
    this.status = status;
    this.fields = error.fields ?? {};
  }
}

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

export type FavoritePayee = {
  document: string;
  name: string;
  savedAt: string;
};

export type FavoritesResponse = {
  document: string;
  favorites: FavoritePayee[];
};

export type NotificationEntry = {
  id: string;
  eventId: string;
  email: string;
  document: string;
  message: string;
  createdAt: string;
};

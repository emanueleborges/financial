import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ApiClientError,
  ApiError,
  BalanceResponse,
  FavoritePayee,
  FavoritesResponse,
  NotificationEntry,
  StatementResponse,
  TransactionResponse,
  UserResponse,
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  createUser(body: {
    name: string;
    email: string;
    document: string;
    password: string;
    initialBalance: number;
  }) {
    return this.post<UserResponse>('/api/v1/users', body);
  }

  getUser(document: string) {
    return this.get<UserResponse>(`/api/v1/users/${document}`);
  }

  getBalance(document: string) {
    return this.get<BalanceResponse>(`/api/v1/users/${document}/balance`);
  }

  listTransactions(document: string, limit = 50) {
    return this.get<StatementResponse>(`/api/v1/users/${document}/transactions?limit=${limit}`);
  }

  transfer(body: { payerDocument: string; payeeDocument: string; amount: number }, idempotencyKey: string) {
    return this.post<TransactionResponse>('/api/v1/transactions', body, {
      'Idempotency-Key': idempotencyKey,
    });
  }

  reverse(body: { transactionId: string; reason: string }) {
    return this.post<TransactionResponse>('/api/v1/transactions/reverse', body);
  }

  listFavorites(document: string) {
    return this.get<FavoritesResponse>(`/api/v1/users/${document}/favorites`);
  }

  addFavorite(owner: string, payee: { document: string; name: string }) {
    return this.post<FavoritesResponse>(`/api/v1/users/${owner}/favorites`, payee);
  }

  removeFavorite(owner: string, payeeDocument: string) {
    return this.delete<FavoritesResponse>(`/api/v1/users/${owner}/favorites/${payeeDocument}`);
  }

  listNotifications() {
    return firstValueFrom(
      this.http.get<{ document: string; entries: NotificationEntry[] }>(
        `${environment.notificationUrl}/api/v1/notifications`
      )
    );
  }

  async downloadReceipt(id: string): Promise<Blob> {
    return this.blob(`/api/v1/transactions/${id}/receipt`);
  }

  async downloadStatement(document: string): Promise<Blob> {
    return this.blob(`/api/v1/users/${document}/transactions/export?limit=50`);
  }

  triggerDownload(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  localFavorites(owner: string): FavoritePayee[] {
    try {
      const raw = localStorage.getItem(`fh_favorites_${owner}`);
      return raw ? (JSON.parse(raw) as FavoritePayee[]) : [];
    } catch {
      return [];
    }
  }

  saveLocalFavorites(owner: string, items: FavoritePayee[]) {
    localStorage.setItem(`fh_favorites_${owner}`, JSON.stringify(items));
  }

  private get<T>(path: string) {
    return firstValueFrom(this.http.get<T>(`${environment.apiUrl}${path}`)).catch(this.rethrow);
  }

  private post<T>(path: string, body: unknown, headers?: Record<string, string>) {
    return firstValueFrom(
      this.http.post<T>(`${environment.apiUrl}${path}`, body, {
        headers: new HttpHeaders(headers ?? {}),
      })
    ).catch(this.rethrow);
  }

  private delete<T>(path: string) {
    return firstValueFrom(this.http.delete<T>(`${environment.apiUrl}${path}`)).catch(this.rethrow);
  }

  private async blob(path: string): Promise<Blob> {
    try {
      return await firstValueFrom(
        this.http.get(`${environment.apiUrl}${path}`, { responseType: 'blob' })
      );
    } catch (err) {
      this.rethrow(err);
    }
  }

  private rethrow(err: unknown): never {
    if (err instanceof HttpErrorResponse) {
      const body = (err.error || {}) as ApiError;
      throw new ApiClientError(err.status, {
        code: body.code || 'HTTP_ERROR',
        message: body.message || err.statusText,
        fields: body.fields,
      });
    }
    throw err;
  }
}

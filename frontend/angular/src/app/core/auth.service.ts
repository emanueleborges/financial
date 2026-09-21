import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse } from './models';

const ACCESS_KEY = 'fh_access_token';
const REFRESH_KEY = 'fh_refresh_token';

export type Session = {
  token: string;
  document: string;
  userId?: string;
  email?: string;
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly session = signal<Session | null>(this.readSession());
  readonly ready = signal(true);

  constructor(private readonly http: HttpClient) {}

  async login(document: string, password: string): Promise<void> {
    const res = await firstValueFrom(
      this.http.post<AuthResponse>(`${environment.apiUrl}/api/v1/auth/login`, {
        document,
        password,
      })
    );
    localStorage.setItem(ACCESS_KEY, res.accessToken);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    this.session.set(this.readSession());
  }

  logout() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    this.session.set(null);
  }

  private readSession(): Session | null {
    if (typeof localStorage === 'undefined') return null;
    const token = localStorage.getItem(ACCESS_KEY);
    if (!token) return null;
    try {
      const json = atob(token.split('.')[1].replaceAll('-', '+').replaceAll('_', '/'));
      const payload = JSON.parse(json) as { sub?: string; userId?: string; email?: string; exp?: number };
      if (!payload.sub) return null;
      if (payload.exp && payload.exp * 1000 < Date.now()) {
        localStorage.removeItem(ACCESS_KEY);
        localStorage.removeItem(REFRESH_KEY);
        return null;
      }
      return { token, document: payload.sub, userId: payload.userId, email: payload.email };
    } catch {
      return null;
    }
  }
}

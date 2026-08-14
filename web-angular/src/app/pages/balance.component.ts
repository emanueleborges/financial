import { Component, inject, OnInit, signal } from '@angular/core';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { BalanceResponse, NotificationEntry, UserResponse } from '../core/models';
import { formatBRL, formatDateTime } from '../core/format';
import { FormErrorComponent } from '../shared/form-error.component';

@Component({
  selector: 'fh-balance',
  imports: [FormErrorComponent],
  template: `
    <div>
      <div style="display:flex; flex-wrap:wrap; justify-content:space-between; gap:1rem; align-items:flex-end;">
        <div>
          <p class="kicker">Sua conta</p>
          <h1 class="display" style="font-size: 2.25rem; margin: 0.5rem 0 0;">{{ user()?.name ?? '…' }}</h1>
        </div>
        <button class="btn btn-ghost" type="button" (click)="load()">Atualizar</button>
      </div>
      <fh-form-error [error]="error()" (dismiss)="error.set(null)" />
      @if (loading() && !user()) {
        <p class="muted">Carregando saldo…</p>
      } @else {
        <section class="card" style="margin-top: 2rem;">
          <p class="kicker">Saldo disponível</p>
          <p class="money display" style="font-size: 1.75rem; margin: 0.25rem 0 0;">
            {{ balance() ? formatBRL(balance()!.balance) : '—' }}
          </p>
          <dl style="display:grid; gap:1rem; margin-top:1.25rem; font-size:0.875rem;" class="muted">
            <div>
              <dt>CPF / CNPJ</dt>
              <dd class="mono" style="color: var(--foam); margin: 0.25rem 0 0;">{{ user()?.document }}</dd>
            </div>
            <div>
              <dt>E-mail</dt>
              <dd style="color: var(--foam); margin: 0.25rem 0 0;">{{ user()?.email }}</dd>
            </div>
            <div>
              <dt>Limite diário</dt>
              <dd style="color: var(--foam); margin: 0.25rem 0 0;">{{ user() ? formatBRL(user()!.dailyLimit) : '—' }}</dd>
            </div>
          </dl>
        </section>
      }
      @if (notes().length) {
        <section style="margin-top: 2rem;">
          <p class="kicker">Notificações</p>
          <ul class="list-reset" style="margin-top: 0.75rem; display:grid; gap:0.5rem;">
            @for (note of notes(); track note.id) {
              <li class="card" style="padding: 0.85rem 1rem;">
                <p style="margin:0; font-size:0.875rem;">{{ note.message }}</p>
                <p class="muted" style="margin:0.35rem 0 0; font-size:0.75rem;">{{ formatDateTime(note.createdAt) }}</p>
              </li>
            }
          </ul>
        </section>
      }
    </div>
  `,
})
export class BalanceComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  user = signal<UserResponse | null>(null);
  balance = signal<BalanceResponse | null>(null);
  notes = signal<NotificationEntry[]>([]);
  error = signal<unknown>(null);
  loading = signal(true);
  formatBRL = formatBRL;
  formatDateTime = formatDateTime;

  ngOnInit() {
    void this.load();
  }

  async load() {
    const session = this.auth.session();
    if (!session) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      const [user, balance] = await Promise.all([
        this.api.getUser(session.document),
        this.api.getBalance(session.document),
      ]);
      this.user.set(user);
      this.balance.set(balance);
      try {
        const inbox = await this.api.listNotifications();
        this.notes.set(inbox.entries ?? []);
      } catch {
        this.notes.set([]);
      }
    } catch (err) {
      this.error.set(err);
    } finally {
      this.loading.set(false);
    }
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { FavoritePayee, StatementEntryResponse, TransactionResponse } from '../core/models';
import { formatBRL, formatDateTime, formatDateTimeShort, formatSigned } from '../core/format';
import { FormErrorComponent } from '../shared/form-error.component';

@Component({
  selector: 'fh-transactions',
  imports: [FormsModule, FormErrorComponent],
  template: `
    <div>
      <div style="display:flex; flex-wrap:wrap; justify-content:space-between; gap:1rem; align-items:flex-end;">
        <div>
          <p class="kicker">Conta</p>
          <h1 class="display" style="font-size: 2.25rem; margin: 0.5rem 0 0;">Extrato</h1>
          <p class="muted">Movimentações com saldo após cada lançamento.</p>
        </div>
        <div class="row">
          <button class="btn btn-ghost" type="button" [disabled]="downloadingStatement() || !entries().length" (click)="downloadStatement()">
            {{ downloadingStatement() ? 'Gerando PDF…' : 'Exportar extrato PDF' }}
          </button>
          <button class="btn btn-ghost" type="button" (click)="load()">Atualizar</button>
        </div>
      </div>

      <section class="card" style="margin: 1.5rem 0; display:flex; justify-content:space-between; align-items:baseline;">
        <div>
          <p class="kicker">Saldo atual</p>
          <p class="money display" style="font-size:1.5rem; margin:0.25rem 0 0;">
            {{ currentBalance() == null ? '—' : formatBRL(currentBalance()!) }}
          </p>
        </div>
        <p class="mono muted" style="font-size:0.7rem;">{{ sessionDocument() }}</p>
      </section>

      <fh-form-error [error]="error()" (dismiss)="error.set(null)" />

      @if (loading() && !entries().length) {
        <p class="muted">Carregando extrato…</p>
      } @else if (!entries().length) {
        <p class="card" style="text-align:center; padding:2rem;">Nenhuma movimentação ainda. Faça uma transferência para ver o extrato aqui.</p>
      } @else {
        <div class="grid-tx">
          <div class="card" style="padding:0; overflow:hidden;">
            <div class="tx-row tx-head">
              <span>Movimentação</span><span style="text-align:right;">Valor</span>
              <span style="text-align:right;">Saldo</span><span style="text-align:right;">Quando</span>
            </div>
            @for (entry of entries(); track entry.transaction.id) {
              <button type="button" class="tx-row" [class.active]="entry.transaction.id === selectedId()" (click)="selectedId.set(entry.transaction.id)">
                <div>
                  <p style="margin:0; font-size:0.875rem;">{{ movementLabel(entry.transaction) }}</p>
                  <p class="muted" style="margin:0.2rem 0 0; font-size:0.7rem;">{{ entry.transaction.type }} · {{ entry.transaction.status }}</p>
                </div>
                <p style="margin:0; text-align:right;" [style.color]="entry.signedAmount < 0 ? '#fecaca' : '#a7f3d0'">{{ formatSigned(entry.signedAmount) }}</p>
                <p class="mono" style="margin:0; text-align:right; font-size:0.875rem;">{{ entry.balanceAfter == null ? '—' : formatBRL(entry.balanceAfter) }}</p>
                <p class="muted" style="margin:0; text-align:right; font-size:0.7rem;">{{ formatDateTimeShort(entry.transaction.createdAt) }}</p>
              </button>
            }
          </div>
          @if (selected(); as tx) {
            <div class="card">
              <p class="kicker">{{ tx.status }}</p>
              <p style="margin:0.4rem 0 0;">{{ formatSigned(selectedEntry()!.signedAmount) }}</p>
              <p class="muted">Pagador: {{ tx.payerName }} · {{ tx.payerDocument }}</p>
              <p class="muted">Recebedor: {{ tx.payeeName }} · {{ tx.payeeDocument }}</p>
              <p class="muted">{{ formatDateTime(tx.createdAt) }}</p>
              <p class="mono muted" style="font-size:0.7rem; word-break:break-all;">{{ tx.id }}</p>
              <div style="margin-top:1rem; display:grid; gap:0.5rem;">
                <button class="btn btn-ghost" type="button" (click)="downloadReceipt()" [disabled]="downloadingReceipt()">
                  {{ downloadingReceipt() ? 'Gerando PDF…' : 'Exportar comprovante PDF' }}
                </button>
                @if (canFavorite()) {
                  <button class="btn btn-ghost" type="button" (click)="toggleFavorite()">
                    {{ counterpartyFavorited() ? 'Remover dos favoritos' : 'Favoritar contraparte' }}
                  </button>
                }
              </div>
              @if (canReverse()) {
                <label class="field" style="margin-top:1rem;">
                  <span class="label">Motivo do estorno</span>
                  <input class="input" [(ngModel)]="reason" name="reason" />
                </label>
                <button class="btn btn-ghost" type="button" (click)="onReverse()" [disabled]="reversing()">
                  {{ reversing() ? 'Estornando…' : 'Estornar transferência' }}
                </button>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class TransactionsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  entries = signal<StatementEntryResponse[]>([]);
  currentBalance = signal<number | null>(null);
  favorites = signal<FavoritePayee[]>([]);
  selectedId = signal<string | null>(null);
  reason = 'Estorno solicitado pelo usuário';
  error = signal<unknown>(null);
  loading = signal(true);
  reversing = signal(false);
  downloadingReceipt = signal(false);
  downloadingStatement = signal(false);

  formatBRL = formatBRL;
  formatSigned = formatSigned;
  formatDateTime = formatDateTime;
  formatDateTimeShort = formatDateTimeShort;

  ngOnInit() {
    void this.load();
  }

  sessionDocument() {
    return this.auth.session()?.document ?? '';
  }

  selectedEntry() {
    return this.entries().find((e) => e.transaction.id === this.selectedId()) ?? null;
  }

  selected() {
    return this.selectedEntry()?.transaction ?? null;
  }

  canReverse() {
    const tx = this.selected();
    return tx?.type === 'TRANSFER' && tx.status === 'COMPLETED' && tx.payerDocument === this.sessionDocument();
  }

  counterparty() {
    const tx = this.selected();
    const doc = this.sessionDocument();
    if (!tx) return null;
    return tx.payerDocument === doc
      ? { document: tx.payeeDocument, name: tx.payeeName }
      : { document: tx.payerDocument, name: tx.payerName };
  }

  canFavorite() {
    const cp = this.counterparty();
    return Boolean(cp?.document) && cp?.document !== this.sessionDocument();
  }

  counterpartyFavorited() {
    const cp = this.counterparty();
    return Boolean(cp?.document && this.favorites().some((f) => f.document === cp.document));
  }

  movementLabel(tx: TransactionResponse) {
    const outbound = tx.payerDocument === this.sessionDocument();
    if (tx.type === 'REVERSAL') {
      return outbound ? `Estorno para ${tx.payeeName ?? '—'}` : `Estorno de ${tx.payerName ?? '—'}`;
    }
    return outbound ? `Transferência para ${tx.payeeName ?? '—'}` : `Transferência de ${tx.payerName ?? '—'}`;
  }

  async load() {
    const session = this.auth.session();
    if (!session) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      const statement = await this.api.listTransactions(session.document);
      this.entries.set(statement.entries);
      this.currentBalance.set(statement.currentBalance);
      this.selectedId.set(statement.entries[0]?.transaction.id ?? null);
      try {
        const fav = await this.api.listFavorites(session.document);
        this.favorites.set(fav.favorites);
      } catch {
        this.favorites.set(this.api.localFavorites(session.document));
      }
    } catch (err) {
      this.error.set(err);
      this.entries.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  async onReverse() {
    const tx = this.selected();
    if (!tx) return;
    this.reversing.set(true);
    try {
      const reversal = await this.api.reverse({ transactionId: tx.id, reason: this.reason });
      await this.load();
      this.selectedId.set(reversal.id);
    } catch (err) {
      this.error.set(err);
    } finally {
      this.reversing.set(false);
    }
  }

  async toggleFavorite() {
    const session = this.auth.session();
    const cp = this.counterparty();
    if (!session || !cp?.document) return;
    try {
      const res = this.counterpartyFavorited()
        ? await this.api.removeFavorite(session.document, cp.document)
        : await this.api.addFavorite(session.document, { document: cp.document, name: cp.name ?? cp.document });
      this.favorites.set(res.favorites);
    } catch (err) {
      this.error.set(err);
    }
  }

  async downloadReceipt() {
    const tx = this.selected();
    if (!tx) return;
    this.downloadingReceipt.set(true);
    try {
      const blob = await this.api.downloadReceipt(tx.id);
      this.api.triggerDownload(blob, `comprovante-${tx.id}.pdf`);
    } catch (err) {
      this.error.set(err);
    } finally {
      this.downloadingReceipt.set(false);
    }
  }

  async downloadStatement() {
    const session = this.auth.session();
    if (!session) return;
    this.downloadingStatement.set(true);
    try {
      const blob = await this.api.downloadStatement(session.document);
      this.api.triggerDownload(blob, `extrato-${session.document}.pdf`);
    } catch (err) {
      this.error.set(err);
    } finally {
      this.downloadingStatement.set(false);
    }
  }
}

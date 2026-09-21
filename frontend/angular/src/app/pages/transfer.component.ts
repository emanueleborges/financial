import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { ApiClientError, FavoritePayee, TransactionResponse, UserResponse } from '../core/models';
import {
  documentKind,
  formatBRL,
  isValidDocumentLength,
  onlyDigits,
  parseBRLInput,
} from '../core/format';
import { FormErrorComponent } from '../shared/form-error.component';
import { MoneyInputComponent } from '../shared/money-input.component';

type PayeeLookup =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'found'; user: UserResponse }
  | { status: 'not_found'; document: string }
  | { status: 'self' }
  | { status: 'error'; message: string };

@Component({
  selector: 'fh-transfer',
  imports: [FormsModule, FormErrorComponent, MoneyInputComponent],
  template: `
    <div style="max-width: 32rem;">
      <p class="kicker">P2P</p>
      <h1 class="display" style="font-size: 2.25rem; margin: 0.5rem 0 0;">Transferir</h1>
      <p class="muted">Selecione um CPF/CNPJ favorito ou informe um novo. Limite diário: R$ 5.000,00.</p>

      <div class="row" style="margin: 1.5rem 0;">
        <button type="button" class="btn btn-ghost" [class.active]="mode() === 'favorite'" [disabled]="!favorites().length" (click)="useFavorites()">
          Favoritos {{ favorites().length ? '(' + favorites().length + ')' : '' }}
        </button>
        <button type="button" class="btn btn-ghost" [class.active]="mode() === 'new'" (click)="switchToNew()">Novo CPF / CNPJ</button>
      </div>

      @if (mode() === 'favorite') {
        <div style="margin-bottom: 1.5rem;">
          <p class="kicker">Recebedores favoritos</p>
          <ul class="list-reset" style="margin-top: 0.5rem; display:grid; gap:0.5rem;">
            @for (item of favorites(); track item.document) {
              <li>
                <button type="button" class="fav-btn" [class.active]="onlyDigits(payeeDocument) === item.document" (click)="selectFavorite(item)">
                  <p class="display" style="margin:0; font-size:1.1rem;">{{ item.name }}</p>
                  <p class="mono muted" style="margin:0.2rem 0 0; font-size:0.75rem;">{{ documentKind(item.document) }} {{ item.document }}</p>
                </button>
              </li>
            }
          </ul>
        </div>
      }

      <form (ngSubmit)="onSubmit()" novalidate>
        @if (mode() === 'new') {
          <label class="field">
            <span class="label">CPF / CNPJ do recebedor</span>
            <input class="input" [class.error]="!!payeeError()" [(ngModel)]="payeeDocument" name="payeeDocument" (ngModelChange)="onPayeeChange()" required inputmode="numeric" maxlength="18" />
            @if (payeeError()) { <p class="field-error">{{ payeeError() }}</p> }
          </label>
        }
        @if (lookup().status === 'loading') { <p class="muted">Identificando recebedor…</p> }
        @if (lookup().status === 'found') {
          <div class="ok-box" style="margin-bottom: 1.25rem; display:flex; justify-content:space-between; gap:0.75rem;">
            <div>
              <p class="kicker">Recebedor identificado</p>
              <p class="display" style="margin:0.25rem 0 0; font-size:1.15rem;">{{ foundUser()!.name }}</p>
              <p class="mono muted" style="margin:0.2rem 0 0; font-size:0.75rem;">{{ documentKind(foundUser()!.document) }} {{ foundUser()!.document }}</p>
            </div>
            <button type="button" class="btn btn-ghost" style="padding: 0.5rem 0.75rem; font-size:0.75rem;" (click)="toggleFavorite()">
              {{ favorited() ? 'Remover favorito' : 'Favoritar' }}
            </button>
          </div>
        }
        <label class="field">
          <span class="label">Valor</span>
          <fh-money-input [value]="amountMasked" [error]="!!fieldErrors()['amount']" (valueChange)="onMoney($event)" />
          @if (fieldErrors()['amount']) { <p class="field-error">{{ fieldErrors()['amount'] }}</p> }
        </label>
        <fh-form-error [error]="error()" (dismiss)="error.set(null)" />
        <button class="btn btn-primary" type="submit" [disabled]="loading() || lookup().status !== 'found'">
          {{ loading() ? 'Enviando…' : 'Confirmar transferência' }}
        </button>
      </form>

      @if (result(); as tx) {
        <div class="success-box" style="margin-top: 1.5rem;">
          <p style="color: var(--amber); margin:0;">Transferência {{ tx.status }}</p>
          <p class="display" style="font-size:1.75rem; margin:0.5rem 0 0;">{{ formatBRL(tx.amount) }}</p>
          <p class="muted">Para {{ tx.payeeName }} · {{ tx.payeeDocument }}</p>
          <p class="mono muted" style="font-size:0.75rem; word-break: break-all;">TX: {{ tx.id }}</p>
        </div>
      }
    </div>
  `,
})
export class TransferComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  favorites = signal<FavoritePayee[]>([]);
  mode = signal<'favorite' | 'new'>('new');
  payeeDocument = '';
  amountMasked = '';
  amountValue: number | null = null;
  error = signal<unknown>(null);
  fieldErrors = signal<Record<string, string>>({});
  result = signal<TransactionResponse | null>(null);
  loading = signal(false);
  lookup = signal<PayeeLookup>({ status: 'idle' });
  private lookupTimer?: number;

  formatBRL = formatBRL;
  documentKind = documentKind;
  onlyDigits = onlyDigits;

  ngOnInit() {
    void this.loadFavorites();
  }

  foundUser() {
    const lookup = this.lookup();
    return lookup.status === 'found' ? lookup.user : null;
  }

  favorited() {
    const user = this.foundUser();
    const session = this.auth.session();
    if (!user || !session) return false;
    return this.favorites().some((item) => item.document === user.document);
  }

  payeeError() {
    const fields = this.fieldErrors();
    const lookup = this.lookup();
    if (fields['payeeDocument']) return fields['payeeDocument'];
    if (lookup.status === 'not_found') return `${documentKind(lookup.document)} não cadastrado`;
    if (lookup.status === 'self') return 'Não é possível transferir para o próprio CPF/CNPJ';
    if (lookup.status === 'error') return lookup.message;
    return undefined;
  }

  async loadFavorites() {
    const session = this.auth.session();
    if (!session) return;
    try {
      const res = await this.api.listFavorites(session.document);
      this.favorites.set(res.favorites);
      this.api.saveLocalFavorites(session.document, res.favorites);
    } catch {
      this.favorites.set(this.api.localFavorites(session.document));
    }
    if (this.favorites().length) {
      this.mode.set('favorite');
      this.payeeDocument = this.favorites()[0].document;
      this.scheduleLookup();
    }
  }

  useFavorites() {
    if (!this.favorites().length) return;
    this.mode.set('favorite');
    this.payeeDocument = this.favorites()[0].document;
    this.scheduleLookup();
  }

  switchToNew() {
    this.mode.set('new');
    this.payeeDocument = '';
    this.lookup.set({ status: 'idle' });
  }

  selectFavorite(item: FavoritePayee) {
    this.mode.set('favorite');
    this.payeeDocument = item.document;
    this.scheduleLookup();
  }

  onPayeeChange() {
    this.scheduleLookup();
  }

  onMoney(event: { masked: string; numeric: number | null }) {
    this.amountMasked = event.masked;
    this.amountValue = event.numeric;
  }

  scheduleLookup() {
    const session = this.auth.session();
    if (!session) return;
    window.clearTimeout(this.lookupTimer);
    const digits = onlyDigits(this.payeeDocument);
    if (!isValidDocumentLength(digits)) {
      this.lookup.set({ status: 'idle' });
      return;
    }
    if (digits === session.document) {
      this.lookup.set({ status: 'self' });
      return;
    }
    this.lookupTimer = window.setTimeout(async () => {
      this.lookup.set({ status: 'loading' });
      try {
        const user = await this.api.getUser(digits);
        this.lookup.set({ status: 'found', user });
      } catch (err) {
        if (err instanceof ApiClientError && err.status === 404) {
          this.lookup.set({ status: 'not_found', document: digits });
        } else {
          this.lookup.set({
            status: 'error',
            message: err instanceof Error ? err.message : 'Falha ao consultar recebedor',
          });
        }
      }
    }, 400);
  }

  async toggleFavorite() {
    const session = this.auth.session();
    const user = this.foundUser();
    if (!session || !user) return;
    try {
      const res = this.favorited()
        ? await this.api.removeFavorite(session.document, user.document)
        : await this.api.addFavorite(session.document, { document: user.document, name: user.name });
      this.favorites.set(res.favorites);
      this.api.saveLocalFavorites(session.document, res.favorites);
    } catch {
      /* fallback local only */
    }
  }

  async onSubmit() {
    const session = this.auth.session();
    if (!session) return;
    const digits = onlyDigits(this.payeeDocument);
    const amount = this.amountValue ?? parseBRLInput(this.amountMasked);
    const local: Record<string, string> = {};
    const lookup = this.lookup();
    if (!isValidDocumentLength(digits)) local['payeeDocument'] = 'Informe um CPF (11 dígitos) ou CNPJ (14 dígitos)';
    else if (lookup.status !== 'found') local['payeeDocument'] = 'Aguarde a identificação do recebedor';
    if (amount == null || amount <= 0) local['amount'] = 'Valor deve ser maior que zero';
    this.fieldErrors.set(local);
    if (Object.keys(local).length) {
      this.error.set(new ApiClientError(400, { code: 'VALIDATION_ERROR', message: 'Um ou mais campos são inválidos', fields: local }));
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    try {
      const tx = await this.api.transfer(
        { payerDocument: session.document, payeeDocument: digits, amount: amount as number },
        crypto.randomUUID()
      );
      this.result.set(tx);
      this.amountMasked = '';
      this.amountValue = null;
    } catch (err) {
      this.error.set(err);
      if (err instanceof ApiClientError) this.fieldErrors.set(err.fields);
    } finally {
      this.loading.set(false);
    }
  }
}

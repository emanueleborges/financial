import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { ApiClientError } from '../core/models';
import { onlyDigits, parseBRLInput } from '../core/format';
import { FormErrorComponent } from '../shared/form-error.component';
import { MoneyInputComponent } from '../shared/money-input.component';

@Component({
  selector: 'fh-register',
  imports: [FormsModule, RouterLink, FormErrorComponent, MoneyInputComponent],
  template: `
    <div class="page">
      <div class="bg-glow"></div>
      <div class="auth-wrap">
        <a routerLink="/" class="display" style="font-size: 1.5rem; margin-bottom: 2.5rem;">Financial Hub</a>
        <h1 class="display" style="font-size: 2.25rem; margin: 0;">Criar conta</h1>
        <p class="muted">Cadastro com saldo inicial. CPF (11 dígitos) ou CNPJ (14 dígitos).</p>
        <form (ngSubmit)="onSubmit()" novalidate style="margin-top: 2rem;">
          <label class="field">
            <span class="label">Nome</span>
            <input class="input" [class.error]="!!fieldErrors()['name']" [(ngModel)]="name" name="name" required />
            @if (fieldErrors()['name']) { <p class="field-error">{{ fieldErrors()['name'] }}</p> }
          </label>
          <label class="field">
            <span class="label">E-mail</span>
            <input class="input" type="email" [class.error]="!!fieldErrors()['email']" [(ngModel)]="email" name="email" required />
            @if (fieldErrors()['email']) { <p class="field-error">{{ fieldErrors()['email'] }}</p> }
          </label>
          <label class="field">
            <span class="label">CPF / CNPJ</span>
            <input class="input" [class.error]="!!fieldErrors()['document']" [(ngModel)]="document" name="document" required inputmode="numeric" maxlength="18" />
            @if (fieldErrors()['document']) { <p class="field-error">{{ fieldErrors()['document'] }}</p> }
          </label>
          <label class="field">
            <span class="label">Senha</span>
            <input class="input" type="password" [class.error]="!!fieldErrors()['password']" [(ngModel)]="password" name="password" required minlength="6" />
            @if (fieldErrors()['password']) { <p class="field-error">{{ fieldErrors()['password'] }}</p> }
          </label>
          <label class="field">
            <span class="label">Saldo inicial</span>
            <fh-money-input [value]="balanceMasked" [error]="!!fieldErrors()['initialBalance']" (valueChange)="onMoney($event)" />
            @if (fieldErrors()['initialBalance']) { <p class="field-error">{{ fieldErrors()['initialBalance'] }}</p> }
          </label>
          <fh-form-error [error]="error()" (dismiss)="error.set(null)" />
          <div class="row">
            <button class="btn btn-primary" type="submit" [disabled]="loading()">{{ loading() ? 'Criando…' : 'Criar conta' }}</button>
            <a routerLink="/login" class="btn btn-ghost">Já tenho conta</a>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class RegisterComponent {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  name = '';
  email = '';
  document = '';
  password = '';
  balanceMasked = '1.000,00';
  balanceValue: number | null = 1000;
  error = signal<unknown>(null);
  fieldErrors = signal<Record<string, string>>({});
  loading = signal(false);

  onMoney(event: { masked: string; numeric: number | null }) {
    this.balanceMasked = event.masked;
    this.balanceValue = event.numeric;
  }

  async onSubmit() {
    const local: Record<string, string> = {};
    if (this.name.trim().length < 2) local['name'] = 'Nome deve ter pelo menos 2 caracteres';
    if (!this.email.includes('@')) local['email'] = 'E-mail inválido';
    const digits = onlyDigits(this.document);
    if (digits.length !== 11 && digits.length !== 14) {
      local['document'] = 'CPF deve ter 11 dígitos ou CNPJ 14 dígitos';
    }
    if (this.password.length < 6) local['password'] = 'Senha deve ter pelo menos 6 caracteres';
    const balance = this.balanceValue ?? parseBRLInput(this.balanceMasked);
    if (balance == null || balance < 0) local['initialBalance'] = 'Saldo inicial não pode ser negativo';
    this.fieldErrors.set(local);
    if (Object.keys(local).length) {
      this.error.set(new ApiClientError(400, { code: 'VALIDATION_ERROR', message: 'Um ou mais campos são inválidos', fields: local }));
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    try {
      await this.api.createUser({
        name: this.name.trim(),
        email: this.email.trim(),
        document: digits,
        password: this.password,
        initialBalance: balance as number,
      });
      await this.auth.login(digits, this.password);
      await this.router.navigateByUrl('/app');
    } catch (err) {
      this.error.set(err);
      if (err instanceof ApiClientError) this.fieldErrors.set(err.fields);
    } finally {
      this.loading.set(false);
    }
  }
}

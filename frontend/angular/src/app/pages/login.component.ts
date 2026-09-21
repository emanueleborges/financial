import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { FormErrorComponent } from '../shared/form-error.component';

@Component({
  selector: 'fh-login',
  imports: [FormsModule, RouterLink, FormErrorComponent],
  template: `
    <div class="page">
      <div class="bg-glow"></div>
      <div class="auth-wrap">
        <a routerLink="/" class="display" style="font-size: 1.5rem; margin-bottom: 2.5rem;">Financial Hub</a>
        <h1 class="display" style="font-size: 2.25rem; margin: 0;">Entrar</h1>
        <p class="muted">Acesse com seu CPF ou CNPJ.</p>
        <form (ngSubmit)="onSubmit()" style="margin-top: 2rem;">
          <label class="field">
            <span class="label">CPF / CNPJ</span>
            <input class="input" [(ngModel)]="document" name="document" required inputmode="numeric" maxlength="18" autocomplete="username" />
          </label>
          <label class="field">
            <span class="label">Senha</span>
            <input class="input" type="password" [(ngModel)]="password" name="password" required minlength="6" autocomplete="current-password" />
          </label>
          <fh-form-error [error]="error()" (dismiss)="error.set(null)" />
          <div class="row">
            <button class="btn btn-primary" type="submit" [disabled]="loading()">{{ loading() ? 'Entrando…' : 'Entrar' }}</button>
            <a routerLink="/register" class="btn btn-ghost">Criar conta</a>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  document = '';
  password = '';
  error = signal<unknown>(null);
  loading = signal(false);

  async onSubmit() {
    this.error.set(null);
    this.loading.set(true);
    try {
      await this.auth.login(this.document.replaceAll(/\D/g, ''), this.password);
      await this.router.navigateByUrl('/app');
    } catch (err) {
      this.error.set(err);
    } finally {
      this.loading.set(false);
    }
  }
}

import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'fh-app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="page">
      <div class="bg-glow"></div>
      <header class="shell-header">
        <div class="shell-bar">
          <a routerLink="/app" class="display" style="font-size: 1.5rem;">Financial Hub</a>
          <nav class="nav">
            <a routerLink="/app" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Saldo</a>
            <a routerLink="/app/transfer" routerLinkActive="active">Transferir</a>
            <a routerLink="/app/transactions" routerLinkActive="active">Extrato</a>
            <button type="button" (click)="logout()">Sair</button>
          </nav>
        </div>
      </header>
      <main class="shell-main">
        <router-outlet />
      </main>
    </div>
  `,
})
export class AppShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout() {
    this.auth.logout();
    void this.router.navigateByUrl('/');
  }
}

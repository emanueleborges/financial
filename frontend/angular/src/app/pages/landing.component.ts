import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'fh-landing',
  imports: [RouterLink],
  template: `
    <div class="landing">
      <div class="landing-bg"></div>
      <main class="landing-main">
        <p class="display" style="font-size: clamp(3rem, 8vw, 6rem); margin: 0 0 1rem; letter-spacing: -0.03em;">
          Financial Hub
        </p>
        <h1 class="muted" style="max-width: 36rem; font-weight: 400; font-size: 1.15rem;">
          Transferências P2P instantâneas com consistência financeira e rastreabilidade total.
        </h1>
        <div class="row" style="margin-top: 2.5rem;">
          <a routerLink="/login" class="btn btn-primary">Entrar</a>
          <a routerLink="/register" class="btn btn-ghost">Criar conta</a>
        </div>
      </main>
    </div>
  `,
})
export class LandingComponent {}

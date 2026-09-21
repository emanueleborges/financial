import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/landing.component').then((m) => m.LandingComponent),
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/app-shell.component').then((m) => m.AppShellComponent),
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/balance.component').then((m) => m.BalanceComponent),
      },
      {
        path: 'transfer',
        loadComponent: () =>
          import('./pages/transfer.component').then((m) => m.TransferComponent),
      },
      {
        path: 'transactions',
        loadComponent: () =>
          import('./pages/transactions.component').then((m) => m.TransactionsComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];

import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth.guard';
import { RepoListComponent } from './features/repos/repo-list.component';
import { AnalyticsDashboardComponent } from './features/analytics/analytics-dashboard.component';
import { LoginComponent } from './pages/login/login.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'repos',
    component: RepoListComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'repos/:id/analytics',
    component: AnalyticsDashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    // Public shareable profile + badge embed — no auth guard (PRD 4.10).
    path: 'u/:username',
    loadComponent: () =>
      import('./features/profile/public-profile.component').then(
        (m) => m.PublicProfileComponent
      )
  },
  { path: 'dashboard', pathMatch: 'full', redirectTo: 'repos' },
  {
    // Public marketing landing page — no auth guard.
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./pages/landing/landing.component').then((m) => m.LandingComponent)
  },
  { path: '**', redirectTo: 'repos' }
];

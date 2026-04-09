import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth.guard';
import { RepoListComponent } from './features/repos/repo-list.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { LoginComponent } from './pages/login/login.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'repos',
    component: RepoListComponent,
    canActivate: [AuthGuard]
  },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' }
];

import { Component, OnInit } from '@angular/core';
import { AsyncPipe, NgIf } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { ApiService } from './core/api';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [NgIf, AsyncPipe, RouterOutlet],
  template: `
    <header class="app-header">
      <h1>Committr</h1>
      <div *ngIf="authService.currentUser$ | async as user" class="user">
        <img [src]="user.avatarUrl" alt="" width="32" height="32" />
        {{ user.username }}
        <button type="button" (click)="logout()">Log out</button>
      </div>
    </header>
    <p *ngIf="status">Backend status: {{ status }}</p>
    <router-outlet />
  `,
  styles: `
    :host {
      display: block;
      font-family: system-ui, sans-serif;
    }
    .app-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 1rem 1.5rem;
    }
    .user {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .user img {
      border-radius: 50%;
    }
    .user button {
      cursor: pointer;
    }
  `
})
export class AppComponent implements OnInit {
  status: string = '';

  constructor(
    private api: ApiService,
    private router: Router,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.getCurrentUser().subscribe({
      next: (user) => this.authService.currentUser$.next(user),
      error: () => this.authService.currentUser$.next(null)
    });

    this.api.getHealth().subscribe({
      next: (res) => (this.status = res.status),
      error: () => (this.status = 'error')
    });
  }

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.authService.currentUser$.next(null);
      this.router.navigate(['/login']);
    });
  }
}

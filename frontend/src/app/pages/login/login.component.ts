import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="relative flex min-h-dvh flex-col items-center justify-center overflow-hidden px-6">
      <!-- backdrop: faint grid + accent glow -->
      <div class="pointer-events-none absolute inset-0"
           style="background-image: linear-gradient(rgba(255,255,255,0.025) 1px, transparent 1px),
                  linear-gradient(90deg, rgba(255,255,255,0.025) 1px, transparent 1px);
                  background-size: 56px 56px;"></div>
      <div class="pointer-events-none absolute left-1/2 top-0 h-[480px] w-[720px] -translate-x-1/2 -translate-y-1/3 rounded-full opacity-25 blur-3xl"
           style="background: radial-gradient(closest-side, #5e6ad2, transparent);"></div>

      <div class="login-card card relative flex w-full max-w-sm flex-col items-center bg-surface/80 px-8 py-10 text-center shadow-2xl shadow-black/50 backdrop-blur">
        <span class="mb-5 flex h-12 w-12 items-center justify-center rounded-xl border border-edge-strong bg-surface-2">
          <svg width="24" height="24" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <circle cx="8" cy="8" r="3" stroke="#7c86ff" stroke-width="1.6"/>
            <path d="M8 0v3.5M8 12.5V16" stroke="#7c86ff" stroke-width="1.6"/>
          </svg>
        </span>

        <h1 class="m-0 text-xl font-semibold tracking-tight">Sign in to Committr</h1>
        <p class="mb-7 mt-2 text-sm text-ink-mute">
          Use your GitHub account to continue.
        </p>

        <button type="button" (click)="login()" class="btn-primary w-full px-4 py-2.5 text-[15px]">
          <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/>
          </svg>
          Continue with GitHub
        </button>

        <p class="mb-0 mt-4 text-xs text-ink-faint">
          Read-only access · we never write to your repos
        </p>
      </div>

      <a routerLink="/" class="relative mt-6 text-xs text-ink-faint no-underline transition-colors hover:text-ink-mute">
        ← Back to home
      </a>
    </main>
  `,
  styles: [`
    .login-card {
      animation: card-pop 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
    }
    @keyframes card-pop {
      from { opacity: 0; transform: translateY(8px) scale(0.98); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
    @media (prefers-reduced-motion: reduce) {
      .login-card { animation: none; }
    }
  `]
})
export class LoginComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.getCurrentUser().subscribe({
      next: () => this.router.navigate(['/repos']),
      error: () => {}
    });
  }

  login(): void {
    this.authService.login();
  }
}

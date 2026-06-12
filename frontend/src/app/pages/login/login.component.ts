import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

/**
 * True only for the first render after a full page load — re-navigating back
 * to /login within the SPA session skips the intro animation.
 */
let introPending = true;

@Component({
  selector: 'app-login',
  standalone: true,
  template: `
    <main class="relative flex min-h-dvh flex-col items-center justify-center overflow-hidden px-6"
          [class.play-intro]="playIntro">
      <!-- backdrop: faint grid + accent glow -->
      <div class="pointer-events-none absolute inset-0"
           style="background-image: linear-gradient(rgba(255,255,255,0.025) 1px, transparent 1px),
                  linear-gradient(90deg, rgba(255,255,255,0.025) 1px, transparent 1px);
                  background-size: 56px 56px;"></div>
      <div class="intro-glow pointer-events-none absolute left-1/2 top-0 h-[480px] w-[720px] -translate-x-1/2 -translate-y-1/3 rounded-full opacity-25 blur-3xl"
           style="background: radial-gradient(closest-side, #5e6ad2, transparent);"></div>

      <div class="relative flex w-full max-w-sm flex-col items-center text-center">
        <span class="intro-logo mb-6 flex h-12 w-12 items-center justify-center rounded-xl border border-edge-strong bg-surface-2 shadow-lg shadow-black/40">
          <svg width="24" height="24" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <circle cx="8" cy="8" r="3" stroke="#7c86ff" stroke-width="1.6"/>
            <path d="M8 0v3.5M8 12.5V16" stroke="#7c86ff" stroke-width="1.6"/>
          </svg>
        </span>

        <h1 class="intro-wordmark m-0 text-3xl font-semibold tracking-tight">Committr</h1>
        <p class="intro-rise mb-8 mt-3 text-[15px] leading-relaxed text-ink-mute" style="--intro-delay: 0.55s">
          Proof of what you actually wrote — lines authored, language trends,
          coding hours, and PR velocity across your repos.
        </p>

        <button type="button" (click)="login()"
                class="intro-rise btn-primary w-full px-4 py-2.5 text-[15px]" style="--intro-delay: 0.7s">
          <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/>
          </svg>
          Continue with GitHub
        </button>

        <p class="intro-rise mt-4 text-xs text-ink-faint" style="--intro-delay: 0.85s">
          Read-only access · we never write to your repos
        </p>

        <div class="intro-rise mt-12 flex flex-wrap items-center justify-center gap-x-3 gap-y-2 font-mono text-[11px] text-ink-faint"
             style="--intro-delay: 1s">
          <span>lines authored</span><span class="text-edge-strong">·</span>
          <span>language trends</span><span class="text-edge-strong">·</span>
          <span>coding hours</span><span class="text-edge-strong">·</span>
          <span>pr velocity</span><span class="text-edge-strong">·</span>
          <span>commit streaks</span>
        </div>
      </div>
    </main>
  `,
  styles: [`
    /* Entrance sequence (~1.3s total), gated behind .play-intro so it only
       runs on the first page load. Elements keep their final state otherwise. */
    .play-intro .intro-logo {
      animation: intro-pop 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
    }
    .play-intro .intro-wordmark {
      animation: intro-rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) 0.3s both;
    }
    .play-intro .intro-rise {
      animation: intro-rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) var(--intro-delay, 0.55s) both;
    }
    .play-intro .intro-glow {
      animation: intro-glow 0.9s ease-out both;
    }

    @keyframes intro-pop {
      from { opacity: 0; transform: scale(0.6); }
      to { opacity: 1; transform: scale(1); }
    }
    @keyframes intro-rise {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
    @keyframes intro-glow {
      from { opacity: 0; }
      to { opacity: 0.25; }
    }

    @media (prefers-reduced-motion: reduce) {
      .play-intro .intro-logo,
      .play-intro .intro-wordmark,
      .play-intro .intro-rise,
      .play-intro .intro-glow {
        animation: none;
      }
    }
  `]
})
export class LoginComponent implements OnInit {
  readonly playIntro = introPending;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    introPending = false;
    this.authService.getCurrentUser().subscribe({
      next: () => this.router.navigate(['/repos']),
      error: () => {}
    });
  }

  login(): void {
    this.authService.login();
  }
}

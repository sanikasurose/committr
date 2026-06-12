import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * True only for the first render after a full page load — re-navigating back
 * to the landing page within the SPA session skips the hero entrance.
 */
let introPending = true;

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main [class.play-intro]="playIntro">
      <!-- ============ Hero ============ -->
      <section class="relative flex min-h-[calc(100dvh-3.5rem)] flex-col items-center justify-center overflow-hidden px-6">
        <div class="bg-grid pointer-events-none absolute inset-0"></div>
        <div class="glow glow-a pointer-events-none absolute left-1/2 top-0 h-[520px] w-[780px] -translate-x-1/2 -translate-y-1/3 rounded-full blur-3xl"></div>
        <div class="glow glow-b pointer-events-none absolute bottom-0 right-0 h-[360px] w-[520px] translate-x-1/4 translate-y-1/3 rounded-full blur-3xl"></div>

        <div class="relative flex w-full max-w-3xl flex-col items-center text-center">
          <span class="intro-logo mb-8 flex h-14 w-14 items-center justify-center rounded-2xl border border-edge-strong bg-surface-2 shadow-lg shadow-black/40">
            <svg width="28" height="28" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <circle cx="8" cy="8" r="3" stroke="#7c86ff" stroke-width="1.6"/>
              <path d="M8 0v3.5M8 12.5V16" stroke="#7c86ff" stroke-width="1.6"/>
            </svg>
          </span>

          <h1 class="intro-rise m-0 text-4xl font-semibold leading-tight tracking-tight sm:text-6xl" style="--intro-delay: 0.25s">
            Proof of what you<br />actually wrote.
          </h1>
          <p class="intro-rise mx-auto mb-10 mt-6 max-w-xl text-base leading-relaxed text-ink-mute sm:text-lg" style="--intro-delay: 0.45s">
            GitHub counts commits. Committr surfaces the metrics that matter —
            lines authored, language shifts, coding-hour patterns, and PR velocity.
          </p>

          <a routerLink="/login" class="intro-rise btn-primary px-6 py-3 text-base" style="--intro-delay: 0.65s">
            <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/>
            </svg>
            Continue with GitHub
          </a>
          <p class="intro-rise mt-4 text-xs text-ink-faint" style="--intro-delay: 0.85s">
            Read-only access · we never write to your repos
          </p>
        </div>
      </section>

      <!-- ============ Feature bento ============ -->
      <section class="mx-auto max-w-6xl px-4 py-24 sm:px-6">
        <p class="m-0 text-center font-mono text-[11px] uppercase tracking-[0.2em] text-accent">why committr</p>
        <h2 class="mx-auto mb-12 mt-3 max-w-xl text-center text-3xl font-semibold tracking-tight">
          Impact metrics GitHub never shows you
        </h2>

        <div class="grid gap-4 md:grid-cols-2">
          <div class="card group p-6 transition-colors hover:border-edge-strong">
            <span class="mb-4 flex h-9 w-9 items-center justify-center rounded-lg border border-edge bg-surface-2 text-accent">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path d="M8 1.5A6.5 6.5 0 1 1 1.5 8H8V1.5Z" stroke="currentColor" stroke-width="1.4"/>
                <path d="M6 1A6 6 0 0 0 1 6h5V1Z" fill="currentColor" opacity="0.45"/>
              </svg>
            </span>
            <h3 class="m-0 text-base font-semibold">Team contribution breakdown</h3>
            <p class="mb-4 mt-2 text-sm leading-relaxed text-ink-mute">
              Commit counts lie in group projects. Committr splits every repo by
              <em>lines actually written</em>, so you can prove your share in interviews.
            </p>
            <p class="m-0 font-mono text-xs text-ink-faint">alice <span class="text-ok">62%</span> · bob <span class="text-warn">27%</span> · carol <span class="text-danger">11%</span></p>
          </div>

          <div class="card group p-6 transition-colors hover:border-edge-strong">
            <span class="mb-4 flex h-9 w-9 items-center justify-center rounded-lg border border-edge bg-surface-2 text-accent">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path d="M1.5 12.5L6 8l3 3 5.5-6.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M11 4.5h3.5V8" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </span>
            <h3 class="m-0 text-base font-semibold">Language trajectory</h3>
            <p class="mb-4 mt-2 text-sm leading-relaxed text-ink-mute">
              A weekly distribution of commits by language reveals your shift over
              months — not a static pie chart of whatever's in the repo today.
            </p>
            <p class="m-0 font-mono text-xs text-ink-faint">2025 <span class="text-ink-mute">Python 71%</span> → 2026 <span class="text-accent">TypeScript 64%</span></p>
          </div>

          <div class="card group p-6 transition-colors hover:border-edge-strong">
            <span class="mb-4 flex h-9 w-9 items-center justify-center rounded-lg border border-edge bg-surface-2 text-accent">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.4"/>
                <path d="M8 4.5V8l2.5 1.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
              </svg>
            </span>
            <h3 class="m-0 text-base font-semibold">Time-of-day heatmap</h3>
            <p class="mb-4 mt-2 text-sm leading-relaxed text-ink-mute">
              Commits bucketed by hour show when you actually code — useful for
              demonstrating timezone-aware collaboration habits.
            </p>
            <p class="m-0 font-mono text-xs text-ink-faint">peak hours <span class="text-ink-mute">21:00–01:00</span> · <span class="text-ok">214</span> commits</p>
          </div>

          <div class="card group p-6 transition-colors hover:border-edge-strong">
            <span class="mb-4 flex h-9 w-9 items-center justify-center rounded-lg border border-edge bg-surface-2 text-accent">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path d="M8 1.5c.5 2.5-1.5 3.5-1.5 5.5a1.5 1.5 0 0 0 3 0c1.5 1 2.5 2.4 2.5 4a4 4 0 1 1-8 0c0-3 2-4.5 2.5-7C7 3 7.5 2 8 1.5Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
              </svg>
            </span>
            <h3 class="m-0 text-base font-semibold">Streaks &amp; PR velocity</h3>
            <p class="mb-4 mt-2 text-sm leading-relaxed text-ink-mute">
              Consistency and shipping speed in two numbers: your longest unbroken
              commit streak and the average time from open to merge.
            </p>
            <p class="m-0 font-mono text-xs text-ink-faint"><span class="text-warn">21w</span> streak · merges in <span class="text-ok">1.4d</span> avg</p>
          </div>
        </div>
      </section>

      <!-- ============ How it works ============ -->
      <section class="border-y border-edge bg-surface/40">
        <div class="mx-auto max-w-6xl px-4 py-24 sm:px-6">
          <p class="m-0 text-center font-mono text-[11px] uppercase tracking-[0.2em] text-accent">how it works</p>
          <h2 class="mx-auto mb-14 mt-3 max-w-xl text-center text-3xl font-semibold tracking-tight">
            Three steps to your impact story
          </h2>

          <div class="grid gap-10 md:grid-cols-3 md:gap-6">
            <div class="flex flex-col items-center text-center">
              <span class="mb-4 flex h-11 w-11 items-center justify-center rounded-xl border border-edge-strong bg-surface-2 text-accent">
                <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                  <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/>
                </svg>
              </span>
              <p class="m-0 font-mono text-[11px] text-ink-faint">01</p>
              <h3 class="mb-0 mt-1 text-base font-semibold">Connect GitHub</h3>
              <p class="mb-0 mt-2 max-w-[26ch] text-sm leading-relaxed text-ink-mute">
                One-click OAuth. Read-only scope, tokens encrypted at rest.
              </p>
            </div>

            <div class="flex flex-col items-center text-center">
              <span class="mb-4 flex h-11 w-11 items-center justify-center rounded-xl border border-edge-strong bg-surface-2 text-accent">
                <svg width="18" height="18" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path d="M2 3.5A1.5 1.5 0 0 1 3.5 2h3l1.5 2h4.5A1.5 1.5 0 0 1 14 5.5v6A1.5 1.5 0 0 1 12.5 13h-9A1.5 1.5 0 0 1 2 11.5v-8Z" stroke="currentColor" stroke-width="1.4"/>
                  <path d="M8 6.5v4M6 8.5h4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                </svg>
              </span>
              <p class="m-0 font-mono text-[11px] text-ink-faint">02</p>
              <h3 class="mb-0 mt-1 text-base font-semibold">Add repos</h3>
              <p class="mb-0 mt-2 max-w-[26ch] text-sm leading-relaxed text-ink-mute">
                Track any repo you can read. Ingestion starts immediately and re-syncs on a schedule.
              </p>
            </div>

            <div class="flex flex-col items-center text-center">
              <span class="mb-4 flex h-11 w-11 items-center justify-center rounded-xl border border-edge-strong bg-surface-2 text-accent">
                <svg width="18" height="18" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path d="M2 13.5h12" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                  <path d="M3.5 10.5v3M7 7.5v6M10.5 9v4.5M14 5v8.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                </svg>
              </span>
              <p class="m-0 font-mono text-[11px] text-ink-faint">03</p>
              <h3 class="mb-0 mt-1 text-base font-semibold">See your impact</h3>
              <p class="mb-0 mt-2 max-w-[26ch] text-sm leading-relaxed text-ink-mute">
                Five charts, KPI cards, and a contributor breakdown — per repo, from pre-aggregated snapshots.
              </p>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ Badge / public profile teaser ============ -->
      <section class="mx-auto max-w-6xl px-4 py-24 sm:px-6">
        <div class="grid items-center gap-12 lg:grid-cols-2">
          <div>
            <p class="m-0 font-mono text-[11px] uppercase tracking-[0.2em] text-accent">share it</p>
            <h2 class="mb-0 mt-3 text-3xl font-semibold tracking-tight">
              A badge that proves it, anywhere
            </h2>
            <p class="mb-8 mt-4 max-w-md text-[15px] leading-relaxed text-ink-mute">
              Every Committr account gets a public profile URL and an embeddable SVG
              badge for your GitHub README — lines shipped, longest streak, top
              language, and team share, refreshed automatically from the latest snapshot.
            </p>
            <a routerLink="/login" class="btn-secondary px-5 py-2.5">
              Get your badge
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
                <path d="M2.5 6h7M6.5 3l3 3-3 3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </a>
          </div>

          <!-- mock badge -->
          <div class="flex items-center justify-center rounded-2xl border border-edge p-10"
               style="background-image: radial-gradient(rgba(255,255,255,0.04) 1px, transparent 1px); background-size: 18px 18px;">
            <div class="card w-full max-w-sm bg-surface-2 p-5 shadow-2xl shadow-black/50" aria-label="Example Committr badge">
              <div class="flex items-center gap-2">
                <span class="flex h-5 w-5 items-center justify-center rounded bg-accent-strong">
                  <svg width="10" height="10" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <circle cx="8" cy="8" r="3" stroke="white" stroke-width="1.8"/>
                    <path d="M8 0v3.5M8 12.5V16" stroke="white" stroke-width="1.8"/>
                  </svg>
                </span>
                <span class="text-sm font-semibold">committr</span>
                <span class="ml-auto font-mono text-[10px] text-ink-faint">@octocat</span>
              </div>
              <div class="mt-4 grid grid-cols-2 gap-3">
                <div>
                  <p class="stat-num m-0 text-lg text-ok">128,419</p>
                  <p class="m-0 font-mono text-[10px] text-ink-faint">lines shipped</p>
                </div>
                <div>
                  <p class="stat-num m-0 text-lg text-warn">21w</p>
                  <p class="m-0 font-mono text-[10px] text-ink-faint">longest streak</p>
                </div>
                <div>
                  <p class="stat-num m-0 text-lg text-accent">TypeScript</p>
                  <p class="m-0 font-mono text-[10px] text-ink-faint">top language</p>
                </div>
                <div>
                  <p class="stat-num m-0 text-lg">62%</p>
                  <p class="m-0 font-mono text-[10px] text-ink-faint">team share</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ Bottom CTA ============ -->
      <section class="relative overflow-hidden border-t border-edge">
        <div class="glow glow-a pointer-events-none absolute left-1/2 top-1/2 h-[400px] w-[700px] -translate-x-1/2 -translate-y-1/2 rounded-full blur-3xl"></div>
        <div class="relative mx-auto flex max-w-3xl flex-col items-center px-6 py-28 text-center">
          <h2 class="m-0 text-3xl font-semibold tracking-tight sm:text-5xl">
            Your commits tell a story.<br />Committr tells it right.
          </h2>
          <a routerLink="/login" class="btn-primary mt-10 px-6 py-3 text-base">
            <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/>
            </svg>
            Continue with GitHub
          </a>
          <p class="mb-0 mt-12 font-mono text-[11px] text-ink-faint">
            Angular · Spring Boot · PostgreSQL · Redis · built by Sanika Surose
          </p>
        </div>
      </section>
    </main>
  `,
  styles: [`
    .bg-grid {
      background-image:
        linear-gradient(rgba(255, 255, 255, 0.025) 1px, transparent 1px),
        linear-gradient(90deg, rgba(255, 255, 255, 0.025) 1px, transparent 1px);
      background-size: 56px 56px;
      animation: grid-drift 60s linear infinite;
    }
    .glow {
      background: radial-gradient(closest-side, #5e6ad2, transparent);
      opacity: 0.22;
      animation: glow-breathe 9s ease-in-out infinite;
    }
    .glow-b {
      background: radial-gradient(closest-side, #34d399, transparent);
      opacity: 0.08;
      animation-name: glow-breathe-dim;
      animation-delay: -4.5s;
    }

    @keyframes grid-drift {
      from { background-position: 0 0; }
      to { background-position: 56px 56px; }
    }
    @keyframes glow-breathe {
      0%, 100% { opacity: 0.22; }
      50% { opacity: 0.34; }
    }
    @keyframes glow-breathe-dim {
      0%, 100% { opacity: 0.08; }
      50% { opacity: 0.15; }
    }

    /* Hero entrance — plays once per full page load. */
    .play-intro .intro-logo {
      animation: intro-pop 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
    }
    .play-intro .intro-rise {
      animation: intro-rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) var(--intro-delay, 0.25s) both;
    }
    @keyframes intro-pop {
      from { opacity: 0; transform: scale(0.6); }
      to { opacity: 1; transform: scale(1); }
    }
    @keyframes intro-rise {
      from { opacity: 0; transform: translateY(12px); }
      to { opacity: 1; transform: translateY(0); }
    }

    @media (prefers-reduced-motion: reduce) {
      .bg-grid, .glow,
      .play-intro .intro-logo, .play-intro .intro-rise {
        animation: none;
      }
    }
  `]
})
export class LandingComponent {
  readonly playIntro = introPending;

  constructor() {
    introPending = false;
  }
}

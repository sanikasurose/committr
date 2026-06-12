import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { apiUrl } from '../../core/api-origin';

/**
 * Public shareable profile (PRD 4.10) — no auth required. Stats are rendered
 * by the public badge SVG endpoint, which serves the last cached snapshot
 * (lines shipped, streak, top language, team share).
 */
@Component({
  selector: 'app-public-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <div class="mb-10 flex flex-wrap items-center gap-3">
        <h1 class="m-0 font-mono text-2xl font-semibold tracking-tight">&#64;{{ username() }}</h1>
        <span class="chip">public profile</span>
      </div>

      <!-- Badge preview -->
      <section class="card overflow-hidden">
        <div class="border-b border-edge px-6 py-4">
          <h2 class="m-0 text-sm font-semibold">Impact badge</h2>
          <p class="mb-0 mt-0.5 text-xs text-ink-faint">
            Lines shipped, commit streak, top language, and team share — from the latest snapshot, refreshed every ingestion run.
          </p>
        </div>
        <div class="flex items-center justify-center px-6 py-10"
             style="background-image: radial-gradient(rgba(255,255,255,0.04) 1px, transparent 1px); background-size: 18px 18px;">
          @if (!badgeFailed()) {
            <img [src]="badgeUrl()" alt="Committr impact badge for {{ username() }}"
                 (error)="badgeFailed.set(true)" class="max-w-full" />
          } @else {
            <div class="text-center">
              <p class="m-0 text-sm font-medium text-ink">No badge data yet</p>
              <p class="mb-0 mt-1.5 text-sm text-ink-mute">
                Stats appear after this user signs in and their first repo snapshot completes.
              </p>
            </div>
          }
        </div>
      </section>

      <!-- Embed snippets -->
      <section id="embed" class="card mt-4 overflow-hidden">
        <div class="border-b border-edge px-6 py-4">
          <h2 class="m-0 text-sm font-semibold">Embed in your README</h2>
          <p class="mb-0 mt-0.5 text-xs text-ink-faint">
            Paste into any GitHub README — the badge links back to this profile.
          </p>
        </div>

        <div class="space-y-4 px-6 py-5">
          <div>
            <div class="mb-1.5 flex items-center justify-between">
              <span class="font-mono text-[11px] uppercase tracking-wider text-ink-faint">markdown</span>
              <button type="button" (click)="copy(markdownSnippet(), 'md')" class="btn-ghost px-2 py-1 text-xs">
                {{ copied() === 'md' ? 'Copied ✓' : 'Copy' }}
              </button>
            </div>
            <pre class="m-0 overflow-x-auto rounded-lg border border-edge bg-bg px-4 py-3 font-mono text-xs leading-relaxed text-ink-mute">{{ markdownSnippet() }}</pre>
          </div>

          <div>
            <div class="mb-1.5 flex items-center justify-between">
              <span class="font-mono text-[11px] uppercase tracking-wider text-ink-faint">html</span>
              <button type="button" (click)="copy(htmlSnippet(), 'html')" class="btn-ghost px-2 py-1 text-xs">
                {{ copied() === 'html' ? 'Copied ✓' : 'Copy' }}
              </button>
            </div>
            <pre class="m-0 overflow-x-auto rounded-lg border border-edge bg-bg px-4 py-3 font-mono text-xs leading-relaxed text-ink-mute">{{ htmlSnippet() }}</pre>
          </div>
        </div>
      </section>

      <p class="mt-6 text-center font-mono text-[11px] text-ink-faint">
        stats reflect the last scheduled snapshot · powered by Committr
      </p>
    </main>
  `
})
export class PublicProfileComponent {
  private readonly route = inject(ActivatedRoute);

  readonly username = toSignal(
    this.route.paramMap.pipe(map((p) => p.get('username') ?? '')),
    { initialValue: '' }
  );
  readonly badgeFailed = signal(false);
  readonly copied = signal<'md' | 'html' | null>(null);

  readonly badgeUrl = computed(() => apiUrl(`/api/badge/${this.username()}`));
  private readonly profileUrl = computed(() => `${window.location.origin}/u/${this.username()}`);
  private readonly absoluteBadgeUrl = computed(() => {
    const url = this.badgeUrl();
    return url.startsWith('http') ? url : `${window.location.origin}${url}`;
  });

  readonly markdownSnippet = computed(
    () => `[![Committr stats](${this.absoluteBadgeUrl()})](${this.profileUrl()})`
  );
  readonly htmlSnippet = computed(
    () => `<a href="${this.profileUrl()}"><img src="${this.absoluteBadgeUrl()}" alt="Committr stats" /></a>`
  );

  copy(text: string, which: 'md' | 'html'): void {
    navigator.clipboard.writeText(text).then(() => {
      this.copied.set(which);
      setTimeout(() => this.copied.set(null), 1500);
    });
  }
}

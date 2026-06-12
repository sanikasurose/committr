import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { RepoService, Repository } from '../../services/repo.service';
import { AddRepoComponent } from './add-repo.component';

const INGESTION_STATUSES = [
  'Fetching commits…',
  'Calculating contributor stats…',
  'Aggregating weekly snapshots…',
  'Almost there…'
];

@Component({
  selector: 'app-repo-list',
  standalone: true,
  imports: [SlicePipe, RouterLink, AddRepoComponent],
  template: `
    <main class="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <div class="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="m-0 text-2xl font-semibold tracking-tight">Repositories</h1>
          <p class="mb-0 mt-1 text-sm text-ink-mute">
            Track a repo to start ingesting commits, contributors, and PR history.
          </p>
        </div>
        @if (repos.length > 0) {
          <span class="chip">{{ repos.length }} tracked</span>
        }
      </div>

      <app-add-repo
        [adding]="adding"
        [resetKey]="addSuccessKey"
        (repoAddRequested)="onAddRequested($event)"
      />

      @if (error) {
        <div class="mt-6 rounded-lg border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger" role="alert">
          {{ error }}
        </div>
      }

      @if (loading && repos.length === 0 && !pendingAdd) {
        <div class="mt-6 grid gap-3">
          @for (i of [0, 1, 2]; track i) {
            <div class="card h-[76px] animate-pulse bg-surface-2"></div>
          }
        </div>
      }

      <div class="mt-6 grid gap-3">
        <!-- Ingestion in progress: pending card with cycling status -->
        @if (pendingAdd) {
          <div class="pending-card card relative overflow-hidden px-5 py-4" role="status" aria-live="polite">
            <div class="flex flex-wrap items-center gap-4">
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2.5">
                  <span class="font-mono text-sm font-medium text-ink">{{ pendingAdd }}</span>
                  <span class="chip">ingesting</span>
                </div>
                <p class="mb-0 mt-1.5 flex items-center gap-2 font-mono text-[11px] text-ink-mute">
                  <svg class="spinner shrink-0" width="12" height="12" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="2" opacity="0.25"/>
                    <path d="M8 1.5A6.5 6.5 0 0 1 14.5 8" stroke="#7c86ff" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                  <span [class]="statusIndex % 2 === 0 ? 'status-fade-a' : 'status-fade-b'">
                    {{ statusMessage }}
                  </span>
                </p>
              </div>
            </div>
            <div class="shimmer pointer-events-none absolute inset-0"></div>
          </div>
        }

        <!-- Ingestion failed: inline error card with retry -->
        @if (failedAdd) {
          <div class="card border-danger/30 px-5 py-4" role="alert">
            <div class="flex flex-wrap items-center gap-4">
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2.5">
                  <span class="font-mono text-sm font-medium text-ink">{{ failedAdd.fullName }}</span>
                  <span class="chip border-danger/40 text-danger">failed</span>
                </div>
                <p class="mb-0 mt-1 text-xs text-danger">{{ failedAdd.message }}</p>
              </div>
              <div class="flex items-center gap-2">
                <button type="button" (click)="retryAdd()" class="btn-secondary px-3 py-1.5">
                  <svg width="12" height="12" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path d="M13.5 8a5.5 5.5 0 1 1-1.6-3.9M13.5 1.5v3h-3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  Retry
                </button>
                <button type="button" (click)="dismissFailed()" class="btn-ghost px-3 py-1.5">Dismiss</button>
              </div>
            </div>
          </div>
        }

        @if (!loading || repos.length > 0) {
          @for (repo of repos; track repo.id) {
            <div class="card group flex flex-wrap items-center gap-4 px-5 py-4 transition-colors hover:border-edge-strong"
                 [class.card-enter]="repo.id === enteringId">
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2.5">
                  <a [routerLink]="['/repos', repo.id, 'analytics']"
                     class="truncate font-mono text-sm font-medium text-ink no-underline hover:text-accent">
                    {{ repo.fullName }}
                  </a>
                  <span class="chip">{{ repo.isPrivate ? 'private' : 'public' }}</span>
                </div>
                <p class="mb-0 mt-1 font-mono text-[11px] text-ink-faint">
                  added {{ repo.createdAt | slice: 0 : 10 }} · synced {{ repo.updatedAt | slice: 0 : 10 }}
                </p>
              </div>

              <div class="flex items-center gap-2">
                <a [routerLink]="['/repos', repo.id, 'analytics']" class="btn-secondary px-3 py-1.5">
                  Analytics
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
                    <path d="M2.5 9.5L9.5 2.5M9.5 2.5H4M9.5 2.5V8" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                  </svg>
                </a>
                <a [href]="repo.htmlUrl" target="_blank" rel="noopener noreferrer"
                   class="btn-ghost p-2" title="Open on GitHub">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                    <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/>
                  </svg>
                </a>
                <button type="button" (click)="onDelete(repo.id)"
                        [disabled]="deletingId === repo.id"
                        [attr.aria-busy]="deletingId === repo.id"
                        class="btn-ghost p-2 hover:!text-danger" title="Stop tracking">
                  @if (deletingId === repo.id) {
                    <span class="font-mono text-[11px]">…</span>
                  } @else {
                    <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                      <path d="M2 4h11M5.5 4V2.5h4V4M6 7v4M9 7v4M3.5 4l.7 8.2c.05.45.43.8.9.8h4.8c.47 0 .85-.35.9-.8L11.5 4"
                            stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
                    </svg>
                  }
                </button>
              </div>
            </div>
          }
        }
      </div>

      @if (loading && repos.length > 0) {
        <p class="mt-3 font-mono text-[11px] text-ink-faint">updating…</p>
      }

      @if (!loading && repos.length === 0 && !error && !pendingAdd && !failedAdd) {
        <div class="card mt-6 flex flex-col items-center px-6 py-16 text-center">
          <svg width="36" height="36" viewBox="0 0 16 16" fill="none" class="mb-4 opacity-40" aria-hidden="true">
            <circle cx="8" cy="8" r="3" stroke="#9595a0" stroke-width="1.4"/>
            <path d="M8 0v3.5M8 12.5V16" stroke="#9595a0" stroke-width="1.4"/>
          </svg>
          <p class="m-0 text-sm font-medium text-ink">No repositories tracked yet</p>
          <p class="mb-0 mt-1.5 max-w-xs text-sm text-ink-mute">
            Add one above using <span class="font-mono text-xs text-ink">owner/repo</span> —
            ingestion starts automatically.
          </p>
        </div>
      }
    </main>
  `,
  styles: [`
    .pending-card {
      animation: pending-pulse 2s ease-in-out infinite;
    }
    @keyframes pending-pulse {
      0%, 100% { border-color: var(--color-edge); }
      50% { border-color: color-mix(in srgb, var(--color-accent) 45%, var(--color-edge)); }
    }

    .shimmer {
      background: linear-gradient(100deg, transparent 30%, rgba(124, 134, 255, 0.05) 50%, transparent 70%);
      background-size: 200% 100%;
      animation: shimmer-sweep 2.2s linear infinite;
    }
    @keyframes shimmer-sweep {
      from { background-position: 200% 0; }
      to { background-position: -200% 0; }
    }

    .spinner {
      animation: spin 0.9s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    /* Two identical fades on alternating classes so the animation restarts
       each time the status message advances. */
    .status-fade-a { animation: status-fade-a 0.4s ease-out both; }
    .status-fade-b { animation: status-fade-b 0.4s ease-out both; }
    @keyframes status-fade-a {
      from { opacity: 0; transform: translateY(3px); }
      to { opacity: 1; transform: translateY(0); }
    }
    @keyframes status-fade-b {
      from { opacity: 0; transform: translateY(3px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .card-enter {
      animation: card-enter 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
    }
    @keyframes card-enter {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    @media (prefers-reduced-motion: reduce) {
      .pending-card, .shimmer, .status-fade-a, .status-fade-b, .card-enter {
        animation: none;
      }
    }
  `]
})
export class RepoListComponent implements OnInit, OnDestroy {
  repos: Repository[] = [];
  loading = false;
  error: string | null = null;
  adding = false;
  /** Increment after a successful add so the child form can reset. */
  addSuccessKey = 0;
  deletingId: number | null = null;

  /** Full name of the repo currently being ingested, if any. */
  pendingAdd: string | null = null;
  statusIndex = 0;
  /** Last failed add, rendered as an inline error card with retry. */
  failedAdd: { fullName: string; message: string } | null = null;
  /** Id of the repo whose card should animate in after a successful add. */
  enteringId: number | null = null;

  private statusTimer: ReturnType<typeof setInterval> | null = null;
  private loadId = 0;

  private readonly repoService = inject(RepoService);
  private readonly cdr = inject(ChangeDetectorRef);

  get statusMessage(): string {
    return INGESTION_STATUSES[this.statusIndex];
  }

  ngOnInit(): void {
    this.loadRepos();
  }

  ngOnDestroy(): void {
    this.stopStatusCycle();
  }

  loadRepos(): void {
    this.loading = true;
    this.error = null;
    this.cdr.markForCheck();
    const id = ++this.loadId;
    this.repoService.getRepos().subscribe({
      next: (data) => {
        if (id !== this.loadId) return;
        this.repos = Array.isArray(data) ? data : [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        if (id !== this.loadId) return;
        this.error = this.extractErrorMessage(err) || 'Failed to load repositories';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  onAddRequested(fullName: string): void {
    this.adding = true;
    this.failedAdd = null;
    this.pendingAdd = fullName;
    this.startStatusCycle();
    this.cdr.markForCheck();
    this.repoService.addRepo(fullName).subscribe({
      next: (repo) => {
        this.adding = false;
        this.pendingAdd = null;
        this.stopStatusCycle();
        this.enteringId = repo?.id ?? null;
        this.addSuccessKey++;
        this.cdr.markForCheck();
        this.loadRepos();
      },
      error: (err) => {
        this.adding = false;
        this.pendingAdd = null;
        this.stopStatusCycle();
        this.failedAdd = {
          fullName,
          message: this.extractErrorMessage(err) || 'Ingestion failed — check the repo name and try again.'
        };
        this.cdr.markForCheck();
      }
    });
  }

  retryAdd(): void {
    const failed = this.failedAdd;
    if (failed) this.onAddRequested(failed.fullName);
  }

  dismissFailed(): void {
    this.failedAdd = null;
    this.cdr.markForCheck();
  }

  onDelete(id: number): void {
    this.deletingId = id;
    this.error = null;
    this.cdr.markForCheck();
    this.repoService.deleteRepo(id).subscribe({
      next: () => {
        this.deletingId = null;
        this.cdr.markForCheck();
        this.loadRepos();
      },
      error: (err) => {
        this.deletingId = null;
        this.error = this.extractErrorMessage(err) || 'Failed to delete repository';
        this.cdr.markForCheck();
      }
    });
  }

  private startStatusCycle(): void {
    this.stopStatusCycle();
    this.statusIndex = 0;
    this.statusTimer = setInterval(() => {
      if (this.statusIndex < INGESTION_STATUSES.length - 1) {
        this.statusIndex++;
        this.cdr.markForCheck();
      }
    }, 2500);
  }

  private stopStatusCycle(): void {
    if (this.statusTimer !== null) {
      clearInterval(this.statusTimer);
      this.statusTimer = null;
    }
  }

  private extractErrorMessage(err: unknown): string | null {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 0) {
        return 'Cannot reach API (network/CORS). Check that the backend is running on port 8080 and try a hard refresh.';
      }
      if (err.status === 401) {
        return 'Not signed in from this origin — open the app at http://localhost:4200, log in again, then reload /repos.';
      }
      if (typeof err.error === 'string' && err.error.length) {
        return err.error.length < 400 ? err.error : `${err.error.slice(0, 200)}…`;
      }
    }
    if (err && typeof err === 'object' && 'error' in err) {
      const body = (err as { error?: unknown }).error;
      if (body && typeof body === 'object' && 'message' in body) {
        const m = (body as { message?: unknown }).message;
        if (typeof m === 'string') return m;
      }
      if (typeof body === 'string' && body.length) return body;
    }
    if (err && typeof err === 'object' && 'message' in err) {
      const m = (err as { message?: unknown }).message;
      if (typeof m === 'string') return m;
    }
    return null;
  }
}

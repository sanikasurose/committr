import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  AnalyticsService,
  CodingHours,
  CommitFrequency,
  ContributorShare,
  LanguageTrend,
  PrVelocity
} from '../../services/analytics.service';
import { RepoService } from '../../services/repo.service';
import { CHART_PALETTE } from '../../core/chart-theme';
import { TeamShareChartComponent } from './charts/team-share-chart.component';
import { LanguageTrendChartComponent } from './charts/language-trend-chart.component';
import { CodingHoursChartComponent } from './charts/coding-hours-chart.component';
import { CommitFrequencyChartComponent } from './charts/commit-frequency-chart.component';
import { PrVelocityChartComponent } from './charts/pr-velocity-chart.component';

type SortKey = 'login' | 'commitCount' | 'linesAdded' | 'linesDeleted' | 'sharePercent';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    DecimalPipe,
    RouterLink,
    TeamShareChartComponent,
    LanguageTrendChartComponent,
    CodingHoursChartComponent,
    CommitFrequencyChartComponent,
    PrVelocityChartComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <a routerLink="/repos"
         class="inline-flex items-center gap-1.5 text-sm text-ink-mute no-underline transition-colors hover:text-ink">
        <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
          <path d="M7.5 2.5L4 6l3.5 3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        Repositories
      </a>

      <div class="mb-8 mt-3 flex flex-wrap items-center gap-3">
        <h1 class="m-0 font-mono text-xl font-semibold tracking-tight">{{ repoName }}</h1>
        <span class="chip">analytics</span>
      </div>

      @if (error) {
        <div class="rounded-lg border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger" role="alert">
          {{ error }}
        </div>
      }

      @if (loading) {
        <div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
          @for (i of [0, 1, 2, 3]; track i) {
            <div class="card h-[104px] animate-pulse bg-surface-2"></div>
          }
        </div>
        <div class="mt-4 grid gap-4 lg:grid-cols-2">
          @for (i of [0, 1, 2, 3]; track i) {
            <div class="card h-72 animate-pulse bg-surface-2"></div>
          }
        </div>
      }

      @if (!loading && !error) {
        <!-- KPI row -->
        <div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <div class="card px-5 py-4">
            <p class="m-0 text-xs font-medium text-ink-mute">Week streak 🔥</p>
            <p class="stat-num mb-0 mt-2 text-2xl">{{ currentStreak }}<span class="text-sm text-ink-faint">w</span></p>
            <p class="mb-0 mt-1 font-mono text-[11px] text-ink-faint">longest {{ longestStreak }}w</p>
          </div>
          <div class="card px-5 py-4">
            <p class="m-0 text-xs font-medium text-ink-mute">PR velocity</p>
            <p class="stat-num mb-0 mt-2 text-2xl">{{ avgMergeLabel }}</p>
            <p class="mb-0 mt-1 font-mono text-[11px] text-ink-faint">avg time to merge</p>
          </div>
          <div class="card px-5 py-4">
            <p class="m-0 text-xs font-medium text-ink-mute">Lines written</p>
            <p class="stat-num mb-0 mt-2 text-2xl text-ok">+{{ fmt(totalLinesAdded) }}</p>
            <p class="mb-0 mt-1 font-mono text-[11px] text-ink-faint">−{{ fmt(totalLinesDeleted) }} deleted</p>
          </div>
          <div class="card px-5 py-4">
            <p class="m-0 text-xs font-medium text-ink-mute">Commits</p>
            <p class="stat-num mb-0 mt-2 text-2xl">{{ fmt(totalCommits) }}</p>
            <p class="mb-0 mt-1 font-mono text-[11px] text-ink-faint">{{ contributors.length }} contributors</p>
          </div>
        </div>

        <!-- Charts grid -->
        <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-12">
          <section class="card px-5 py-4 lg:col-span-8">
            <h3 class="m-0 text-sm font-semibold">Commit frequency</h3>
            <p class="mb-3 mt-0.5 text-xs text-ink-faint">Commits and line churn per week</p>
            <app-commit-frequency-chart [data]="commitFrequency" />
          </section>
          <section class="card px-5 py-4 lg:col-span-4">
            <h3 class="m-0 text-sm font-semibold">Team share</h3>
            <p class="mb-3 mt-0.5 text-xs text-ink-faint">Who actually wrote the code</p>
            <app-team-share-chart [data]="contributors" />
          </section>
          <section class="card px-5 py-4 lg:col-span-6">
            <h3 class="m-0 text-sm font-semibold">Language trend</h3>
            <p class="mb-3 mt-0.5 text-xs text-ink-faint">Weekly distribution of commits by language</p>
            <app-language-trend-chart [data]="languageTrend" />
          </section>
          <section class="card px-5 py-4 lg:col-span-6">
            <h3 class="m-0 text-sm font-semibold">Coding hours</h3>
            <p class="mb-3 mt-0.5 text-xs text-ink-faint">When commits actually happen, by hour of day</p>
            <app-coding-hours-chart [data]="codingHours" />
          </section>
          <section class="card px-5 py-4 lg:col-span-12">
            <h3 class="m-0 text-sm font-semibold">PR velocity trend</h3>
            <p class="mb-3 mt-0.5 text-xs text-ink-faint">Average hours from open to merge, per week</p>
            <app-pr-velocity-chart [data]="prVelocity" />
          </section>
        </div>

        <!-- Contributor table -->
        <section class="card mt-4 overflow-hidden">
          <div class="border-b border-edge px-5 py-4">
            <h3 class="m-0 text-sm font-semibold">Contributors</h3>
            <p class="mb-0 mt-0.5 text-xs text-ink-faint">Sorted by {{ sortLabel }} — click a column to re-sort</p>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full border-collapse text-sm">
              <thead>
                <tr class="text-left font-mono text-[11px] uppercase tracking-wider text-ink-faint">
                  <th class="cursor-pointer px-5 py-3 font-medium hover:text-ink" (click)="sortBy('login')">Contributor</th>
                  <th class="cursor-pointer px-5 py-3 text-right font-medium hover:text-ink" (click)="sortBy('commitCount')">Commits</th>
                  <th class="cursor-pointer px-5 py-3 text-right font-medium hover:text-ink" (click)="sortBy('linesAdded')">Added</th>
                  <th class="cursor-pointer px-5 py-3 text-right font-medium hover:text-ink" (click)="sortBy('linesDeleted')">Deleted</th>
                  <th class="cursor-pointer px-5 py-3 font-medium hover:text-ink" (click)="sortBy('sharePercent')">Share</th>
                </tr>
              </thead>
              <tbody>
                @for (c of sortedContributors; track c.login; let i = $index) {
                  <tr class="border-t border-edge transition-colors hover:bg-surface-2">
                    <td class="px-5 py-3">
                      <span class="flex items-center gap-2.5">
                        <img [src]="c.avatarUrl" alt="" width="24" height="24" class="rounded-full" />
                        <span class="font-medium">{{ c.login }}</span>
                      </span>
                    </td>
                    <td class="px-5 py-3 text-right font-mono tabular-nums">{{ fmt(c.commitCount) }}</td>
                    <td class="px-5 py-3 text-right font-mono tabular-nums text-ok">+{{ fmt(c.linesAdded) }}</td>
                    <td class="px-5 py-3 text-right font-mono tabular-nums text-danger">−{{ fmt(c.linesDeleted) }}</td>
                    <td class="px-5 py-3">
                      <span class="flex items-center gap-3">
                        <span class="h-1.5 w-24 overflow-hidden rounded-full bg-surface-3">
                          <span class="block h-full rounded-full"
                                [style.width.%]="c.sharePercent"
                                [style.background]="palette[i % palette.length]"></span>
                        </span>
                        <span class="font-mono text-xs tabular-nums text-ink-mute">{{ c.sharePercent | number: '1.0-1' }}%</span>
                      </span>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="px-5 py-8 text-center text-ink-faint">No contributor data yet</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }
    </main>
  `
})
export class AnalyticsDashboardComponent implements OnInit {
  repoId = 0;
  repoName = '';
  loading = true;
  error: string | null = null;

  contributors: ContributorShare[] = [];
  languageTrend: LanguageTrend[] = [];
  codingHours: CodingHours | null = null;
  commitFrequency: CommitFrequency[] = [];
  prVelocity: PrVelocity[] = [];

  sortKey: SortKey = 'sharePercent';
  sortDesc = true;
  readonly palette = CHART_PALETTE;

  private readonly route = inject(ActivatedRoute);
  private readonly analytics = inject(AnalyticsService);
  private readonly repoService = inject(RepoService);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.repoId = idParam ? Number(idParam) : 0;
    this.repoName = `Repository #${this.repoId}`;
    if (!this.repoId) {
      this.error = 'Invalid repository id';
      this.loading = false;
      return;
    }

    this.repoService.getRepos().subscribe({
      next: (repos) => {
        const repo = repos.find((r) => r.id === this.repoId);
        if (repo) {
          this.repoName = repo.fullName;
          this.cdr.markForCheck();
        }
      },
      error: () => {}
    });

    forkJoin({
      contributors: this.analytics.getContributors(this.repoId),
      languageTrend: this.analytics.getLanguageTrend(this.repoId),
      codingHours: this.analytics.getCodingHours(this.repoId),
      commitFrequency: this.analytics.getCommitFrequency(this.repoId),
      prVelocity: this.analytics.getPrVelocity(this.repoId)
    }).subscribe({
      next: (r) => {
        this.contributors = r.contributors;
        this.languageTrend = r.languageTrend;
        this.codingHours = r.codingHours;
        this.commitFrequency = r.commitFrequency;
        this.prVelocity = r.prVelocity;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load analytics';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  // KPI derivations — all client-side from snapshot-backed endpoints.

  /** Consecutive weeks with at least one commit, ending at the latest week. */
  get currentStreak(): number {
    const weeks = this.sortedWeeks();
    let streak = 0;
    for (let i = weeks.length - 1; i >= 0 && weeks[i].commitCount > 0; i--) streak++;
    return streak;
  }

  get longestStreak(): number {
    let longest = 0;
    let run = 0;
    for (const w of this.sortedWeeks()) {
      run = w.commitCount > 0 ? run + 1 : 0;
      if (run > longest) longest = run;
    }
    return longest;
  }

  get avgMergeLabel(): string {
    const vals = this.prVelocity.map((p) => p.avgMergeHours).filter((h) => h > 0);
    if (vals.length === 0) return '—';
    const hours = vals.reduce((a, b) => a + b, 0) / vals.length;
    return hours < 48 ? `${hours.toFixed(1)}h` : `${(hours / 24).toFixed(1)}d`;
  }

  get totalLinesAdded(): number {
    return this.contributors.reduce((sum, c) => sum + c.linesAdded, 0);
  }

  get totalLinesDeleted(): number {
    return this.contributors.reduce((sum, c) => sum + c.linesDeleted, 0);
  }

  get totalCommits(): number {
    return this.contributors.reduce((sum, c) => sum + c.commitCount, 0);
  }

  get sortedContributors(): ContributorShare[] {
    const key = this.sortKey;
    const dir = this.sortDesc ? -1 : 1;
    return [...this.contributors].sort((a, b) => {
      const av = a[key];
      const bv = b[key];
      if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv) * dir;
      return ((av as number) - (bv as number)) * dir;
    });
  }

  get sortLabel(): string {
    const labels: Record<SortKey, string> = {
      login: 'name',
      commitCount: 'commits',
      linesAdded: 'lines added',
      linesDeleted: 'lines deleted',
      sharePercent: 'share'
    };
    return labels[this.sortKey];
  }

  sortBy(key: SortKey): void {
    if (this.sortKey === key) {
      this.sortDesc = !this.sortDesc;
    } else {
      this.sortKey = key;
      this.sortDesc = key !== 'login';
    }
  }

  fmt(n: number): string {
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 10_000) return `${(n / 1000).toFixed(0)}k`;
    if (n >= 1000) return `${(n / 1000).toFixed(1)}k`;
    return `${n}`;
  }

  private sortedWeeks(): CommitFrequency[] {
    return [...this.commitFrequency].sort((a, b) => a.weekStart.localeCompare(b.weekStart));
  }
}

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { NgIf } from '@angular/common';
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
import { TeamShareChartComponent } from './charts/team-share-chart.component';
import { LanguageTrendChartComponent } from './charts/language-trend-chart.component';
import { CodingHoursChartComponent } from './charts/coding-hours-chart.component';
import { CommitFrequencyChartComponent } from './charts/commit-frequency-chart.component';
import { PrVelocityChartComponent } from './charts/pr-velocity-chart.component';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    NgIf,
    RouterLink,
    TeamShareChartComponent,
    LanguageTrendChartComponent,
    CodingHoursChartComponent,
    CommitFrequencyChartComponent,
    PrVelocityChartComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a routerLink="/repos">← Back to repositories</a>
    <h1>Analytics — repo #{{ repoId }}</h1>

    <p *ngIf="loading" class="status">Loading analytics…</p>
    <div *ngIf="error" class="error-banner" role="alert">{{ error }}</div>

    <div *ngIf="!loading && !error" class="grid">
      <section><app-team-share-chart [data]="contributors"></app-team-share-chart></section>
      <section><app-commit-frequency-chart [data]="commitFrequency"></app-commit-frequency-chart></section>
      <section><app-language-trend-chart [data]="languageTrend"></app-language-trend-chart></section>
      <section><app-coding-hours-chart [data]="codingHours"></app-coding-hours-chart></section>
      <section><app-pr-velocity-chart [data]="prVelocity"></app-pr-velocity-chart></section>
    </div>
  `,
  styles: [`
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
      gap: 1.5rem;
      margin-top: 1.5rem;
    }
    section {
      padding: 1rem;
      border-radius: 8px;
      background: var(--card-bg, #f8fafc);
    }
    .status { margin: 1rem 0; }
    .error-banner {
      margin: 1rem 0;
      padding: 0.5rem 0.75rem;
      border-radius: 6px;
      background: color-mix(in srgb, #ef4444 12%, transparent);
      color: #b91c1c;
    }
  `]
})
export class AnalyticsDashboardComponent implements OnInit {
  repoId = 0;
  loading = true;
  error: string | null = null;

  contributors: ContributorShare[] = [];
  languageTrend: LanguageTrend[] = [];
  codingHours: CodingHours | null = null;
  commitFrequency: CommitFrequency[] = [];
  prVelocity: PrVelocity[] = [];

  private readonly route = inject(ActivatedRoute);
  private readonly analytics = inject(AnalyticsService);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.repoId = idParam ? Number(idParam) : 0;
    if (!this.repoId) {
      this.error = 'Invalid repository id';
      this.loading = false;
      return;
    }

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
}

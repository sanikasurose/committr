import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { CommitFrequency } from '../../../services/analytics.service';
import { withAlpha } from '../../../core/chart-theme';

const COMMITS = '#818cf8';
const ADDED = '#34d399';
const DELETED = '#f87171';

@Component({
  selector: 'app-commit-frequency-chart',
  standalone: true,
  imports: [BaseChartDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative h-64">
      <canvas baseChart [data]="chartData" [options]="chartOptions" type="line"></canvas>
    </div>
  `,
  styles: [`:host { display: block; }`]
})
export class CommitFrequencyChartComponent implements OnChanges {
  @Input() data: CommitFrequency[] = [];

  chartData: ChartData<'line'> = { labels: [], datasets: [] };
  chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    scales: {
      x: { grid: { display: false } },
      y: { beginAtZero: true, position: 'left', title: { display: true, text: 'Commits' } },
      y1: { beginAtZero: true, position: 'right', grid: { drawOnChartArea: false }, title: { display: true, text: 'Lines' } }
    }
  };

  ngOnChanges(): void {
    this.chartData = {
      labels: this.data.map(d => d.weekStart),
      datasets: [
        {
          label: 'Commits',
          data: this.data.map(d => d.commitCount),
          borderColor: COMMITS,
          backgroundColor: withAlpha(COMMITS, 0.15),
          pointBackgroundColor: COMMITS,
          fill: true,
          yAxisID: 'y',
          tension: 0.35,
          borderWidth: 2,
          pointRadius: 0,
          pointHoverRadius: 4
        },
        {
          label: 'Lines added',
          data: this.data.map(d => d.linesAdded),
          borderColor: ADDED,
          backgroundColor: ADDED,
          yAxisID: 'y1',
          tension: 0.35,
          borderWidth: 1.5,
          pointRadius: 0,
          pointHoverRadius: 4
        },
        {
          label: 'Lines deleted',
          data: this.data.map(d => d.linesDeleted),
          borderColor: DELETED,
          backgroundColor: DELETED,
          yAxisID: 'y1',
          tension: 0.35,
          borderWidth: 1.5,
          pointRadius: 0,
          pointHoverRadius: 4
        }
      ]
    };
  }
}

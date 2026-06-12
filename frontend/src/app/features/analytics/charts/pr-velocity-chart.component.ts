import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { PrVelocity } from '../../../services/analytics.service';
import { withAlpha } from '../../../core/chart-theme';

const VIOLET = '#a78bfa';

@Component({
  selector: 'app-pr-velocity-chart',
  standalone: true,
  imports: [BaseChartDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative h-56">
      <canvas baseChart [data]="chartData" [options]="chartOptions" type="line"></canvas>
    </div>
  `,
  styles: [`:host { display: block; }`]
})
export class PrVelocityChartComponent implements OnChanges {
  @Input() data: PrVelocity[] = [];

  chartData: ChartData<'line'> = { labels: [], datasets: [] };
  chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { display: false } },
      y: { beginAtZero: true, title: { display: true, text: 'Avg merge time (hours)' } }
    }
  };

  ngOnChanges(): void {
    this.chartData = {
      labels: this.data.map(d => d.weekStart),
      datasets: [{
        label: 'Avg merge time (hours)',
        data: this.data.map(d => d.avgMergeHours),
        borderColor: VIOLET,
        backgroundColor: withAlpha(VIOLET, 0.15),
        pointBackgroundColor: VIOLET,
        fill: true,
        tension: 0.35,
        borderWidth: 2,
        pointRadius: 2,
        pointHoverRadius: 5
      }]
    };
  }
}

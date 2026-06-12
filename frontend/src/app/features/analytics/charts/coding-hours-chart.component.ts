import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { CodingHours } from '../../../services/analytics.service';
import { withAlpha } from '../../../core/chart-theme';

const CYAN = '#22d3ee';

@Component({
  selector: 'app-coding-hours-chart',
  standalone: true,
  imports: [BaseChartDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative h-64">
      <canvas baseChart [data]="chartData" [options]="chartOptions" type="bar"></canvas>
    </div>
  `,
  styles: [`:host { display: block; }`]
})
export class CodingHoursChartComponent implements OnChanges {
  @Input() data: CodingHours | null = null;

  chartData: ChartData<'bar'> = { labels: [], datasets: [] };
  chartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        title: { display: true, text: 'Hour of day' },
        grid: { display: false },
        ticks: { maxRotation: 0, autoSkipPadding: 12 }
      },
      y: { beginAtZero: true, title: { display: true, text: 'Commits' } }
    }
  };

  ngOnChanges(): void {
    const hours = this.data?.hours ?? new Array(24).fill(0);
    const max = Math.max(...hours, 1);
    this.chartData = {
      labels: Array.from({ length: 24 }, (_, i) => `${i}:00`),
      datasets: [{
        label: 'Commits',
        data: hours,
        // intensity-scaled bars: busiest hours glow brightest
        backgroundColor: hours.map((h: number) => withAlpha(CYAN, 0.25 + 0.75 * (h / max))),
        hoverBackgroundColor: CYAN,
        borderRadius: 3,
        borderSkipped: false
      }]
    };
  }
}

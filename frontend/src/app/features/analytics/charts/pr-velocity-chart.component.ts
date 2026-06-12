import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { PrVelocity } from '../../../services/analytics.service';

@Component({
  selector: 'app-pr-velocity-chart',
  standalone: true,
  imports: [BaseChartDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h3>PR velocity</h3>
    <canvas baseChart [data]="chartData" [options]="chartOptions" type="line"></canvas>
  `,
  styles: [`:host { display: block; }`]
})
export class PrVelocityChartComponent implements OnChanges {
  @Input() data: PrVelocity[] = [];

  chartData: ChartData<'line'> = { labels: [], datasets: [] };
  chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    scales: {
      y: { beginAtZero: true, title: { display: true, text: 'Avg merge time (hours)' } }
    }
  };

  ngOnChanges(): void {
    this.chartData = {
      labels: this.data.map(d => d.weekStart),
      datasets: [{
        label: 'Avg merge time (hours)',
        data: this.data.map(d => d.avgMergeHours),
        borderColor: '#8b5cf6',
        backgroundColor: '#8b5cf6',
        tension: 0.3
      }]
    };
  }
}

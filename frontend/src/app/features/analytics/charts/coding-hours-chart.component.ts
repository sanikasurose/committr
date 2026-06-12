import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { CodingHours } from '../../../services/analytics.service';

@Component({
  selector: 'app-coding-hours-chart',
  standalone: true,
  imports: [BaseChartDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h3>Coding hours</h3>
    <canvas baseChart [data]="chartData" [options]="chartOptions" type="bar"></canvas>
  `,
  styles: [`:host { display: block; }`]
})
export class CodingHoursChartComponent implements OnChanges {
  @Input() data: CodingHours | null = null;

  chartData: ChartData<'bar'> = { labels: [], datasets: [] };
  chartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    plugins: { legend: { display: false } },
    scales: {
      x: { title: { display: true, text: 'Hour of day' } },
      y: { beginAtZero: true, title: { display: true, text: 'Commits' } }
    }
  };

  ngOnChanges(): void {
    const hours = this.data?.hours ?? new Array(24).fill(0);
    this.chartData = {
      labels: Array.from({ length: 24 }, (_, i) => `${i}:00`),
      datasets: [{ label: 'Commits', data: hours, backgroundColor: '#3b82f6' }]
    };
  }
}

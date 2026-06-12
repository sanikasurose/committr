import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { ContributorShare } from '../../../services/analytics.service';

@Component({
  selector: 'app-team-share-chart',
  standalone: true,
  imports: [BaseChartDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h3>Team share</h3>
    <canvas baseChart [data]="chartData" [options]="chartOptions" type="doughnut"></canvas>
  `,
  styles: [`:host { display: block; max-width: 480px; }`]
})
export class TeamShareChartComponent implements OnChanges {
  @Input() data: ContributorShare[] = [];

  chartData: ChartData<'doughnut'> = { labels: [], datasets: [{ data: [] }] };
  chartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    plugins: { legend: { position: 'right' } }
  };

  ngOnChanges(): void {
    this.chartData = {
      labels: this.data.map(c => c.login),
      datasets: [{
        data: this.data.map(c => c.commitCount),
        backgroundColor: [
          '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
          '#06b6d4', '#ec4899', '#84cc16', '#f97316', '#6366f1'
        ]
      }]
    };
  }
}

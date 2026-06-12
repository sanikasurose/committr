import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { ContributorShare } from '../../../services/analytics.service';
import { CHART_PALETTE } from '../../../core/chart-theme';

@Component({
  selector: 'app-team-share-chart',
  standalone: true,
  imports: [BaseChartDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative h-64">
      <canvas baseChart [data]="chartData" [options]="chartOptions" type="doughnut"></canvas>
    </div>
  `,
  styles: [`:host { display: block; }`]
})
export class TeamShareChartComponent implements OnChanges {
  @Input() data: ContributorShare[] = [];

  chartData: ChartData<'doughnut'> = { labels: [], datasets: [{ data: [] }] };
  chartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '72%',
    plugins: {
      legend: { position: 'bottom' }
    }
  };

  ngOnChanges(): void {
    this.chartData = {
      labels: this.data.map(c => c.login),
      datasets: [{
        data: this.data.map(c => c.commitCount),
        backgroundColor: CHART_PALETTE,
        borderColor: '#121215',
        borderWidth: 2,
        hoverOffset: 6
      }]
    };
  }
}

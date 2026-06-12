import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { CommitFrequency } from '../../../services/analytics.service';

@Component({
  selector: 'app-commit-frequency-chart',
  standalone: true,
  imports: [BaseChartDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h3>Commit frequency</h3>
    <canvas baseChart [data]="chartData" [options]="chartOptions" type="line"></canvas>
  `,
  styles: [`:host { display: block; }`]
})
export class CommitFrequencyChartComponent implements OnChanges {
  @Input() data: CommitFrequency[] = [];

  chartData: ChartData<'line'> = { labels: [], datasets: [] };
  chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    interaction: { mode: 'index', intersect: false },
    scales: {
      y: { beginAtZero: true, position: 'left', title: { display: true, text: 'Commits' } },
      y1: { beginAtZero: true, position: 'right', grid: { drawOnChartArea: false }, title: { display: true, text: 'Lines' } }
    }
  };

  ngOnChanges(): void {
    this.chartData = {
      labels: this.data.map(d => d.weekStart),
      datasets: [
        { label: 'Commits', data: this.data.map(d => d.commitCount), borderColor: '#3b82f6', backgroundColor: '#3b82f6', yAxisID: 'y', tension: 0.3 },
        { label: 'Lines added', data: this.data.map(d => d.linesAdded), borderColor: '#10b981', backgroundColor: '#10b981', yAxisID: 'y1', tension: 0.3 },
        { label: 'Lines deleted', data: this.data.map(d => d.linesDeleted), borderColor: '#ef4444', backgroundColor: '#ef4444', yAxisID: 'y1', tension: 0.3 }
      ]
    };
  }
}

import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { LanguageTrend } from '../../../services/analytics.service';

const PALETTE = [
  '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
  '#06b6d4', '#ec4899', '#84cc16', '#f97316', '#6366f1'
];

@Component({
  selector: 'app-language-trend-chart',
  standalone: true,
  imports: [BaseChartDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h3>Language trend</h3>
    <canvas baseChart [data]="chartData" [options]="chartOptions" type="bar"></canvas>
  `,
  styles: [`:host { display: block; }`]
})
export class LanguageTrendChartComponent implements OnChanges {
  @Input() data: LanguageTrend[] = [];

  chartData: ChartData<'bar'> = { labels: [], datasets: [] };
  chartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    scales: { x: { stacked: true }, y: { stacked: true, beginAtZero: true } }
  };

  ngOnChanges(): void {
    const languages = new Set<string>();
    for (const week of this.data) {
      for (const lang of Object.keys(week.languageDistribution || {})) languages.add(lang);
    }
    const langList = Array.from(languages);

    this.chartData = {
      labels: this.data.map(w => w.weekStart),
      datasets: langList.map((lang, i) => ({
        label: lang,
        data: this.data.map(w => w.languageDistribution?.[lang] ?? 0),
        backgroundColor: PALETTE[i % PALETTE.length]
      }))
    };
  }
}

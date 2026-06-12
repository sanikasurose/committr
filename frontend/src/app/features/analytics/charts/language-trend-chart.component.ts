import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { LanguageTrend } from '../../../services/analytics.service';
import { CHART_PALETTE } from '../../../core/chart-theme';

@Component({
  selector: 'app-language-trend-chart',
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
export class LanguageTrendChartComponent implements OnChanges {
  @Input() data: LanguageTrend[] = [];

  chartData: ChartData<'bar'> = { labels: [], datasets: [] };
  chartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: { stacked: true, grid: { display: false } },
      y: { stacked: true, beginAtZero: true }
    }
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
        backgroundColor: CHART_PALETTE[i % CHART_PALETTE.length],
        borderRadius: 3,
        borderSkipped: false,
        maxBarThickness: 28
      }))
    };
  }
}

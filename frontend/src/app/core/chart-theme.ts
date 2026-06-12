import { Chart, registerables } from 'chart.js';

/** Vibrant series palette tuned for the dark UI. */
export const CHART_PALETTE = [
  '#818cf8', // indigo
  '#34d399', // emerald
  '#fbbf24', // amber
  '#f87171', // red
  '#a78bfa', // violet
  '#22d3ee', // cyan
  '#f472b6', // pink
  '#a3e635', // lime
  '#fb923c', // orange
  '#60a5fa'  // blue
];

export function withAlpha(hex: string, alpha: number): string {
  const a = Math.round(alpha * 255).toString(16).padStart(2, '0');
  return `${hex}${a}`;
}

const GRID = 'rgba(255, 255, 255, 0.05)';
const TICK = '#9595a0';
const FONT = "'Inter', ui-sans-serif, system-ui, sans-serif";
const MONO = "'JetBrains Mono', ui-monospace, monospace";

/** Global chart.js defaults for the dark theme. Applied once at bootstrap. */
export function applyChartDarkTheme(): void {
  // ng2-charts registers plugins lazily on first chart render; register now so
  // plugin defaults (legend, tooltip) exist before we mutate them.
  Chart.register(...registerables);

  Chart.defaults.color = TICK;
  Chart.defaults.borderColor = GRID;
  Chart.defaults.font.family = FONT;
  Chart.defaults.font.size = 11;

  Chart.defaults.plugins.legend.labels.boxWidth = 8;
  Chart.defaults.plugins.legend.labels.boxHeight = 8;
  Chart.defaults.plugins.legend.labels.usePointStyle = true;
  Chart.defaults.plugins.legend.labels.pointStyle = 'circle';
  Chart.defaults.plugins.legend.labels.padding = 14;

  Chart.defaults.plugins.tooltip.backgroundColor = '#1f1f26';
  Chart.defaults.plugins.tooltip.titleColor = '#ededf0';
  Chart.defaults.plugins.tooltip.bodyColor = '#9595a0';
  Chart.defaults.plugins.tooltip.borderColor = '#34343d';
  Chart.defaults.plugins.tooltip.borderWidth = 1;
  Chart.defaults.plugins.tooltip.padding = 10;
  Chart.defaults.plugins.tooltip.cornerRadius = 8;
  Chart.defaults.plugins.tooltip.bodyFont = { family: MONO, size: 11 };
  Chart.defaults.plugins.tooltip.displayColors = true;
  Chart.defaults.plugins.tooltip.boxWidth = 8;
  Chart.defaults.plugins.tooltip.boxHeight = 8;
  Chart.defaults.plugins.tooltip.boxPadding = 4;
  Chart.defaults.plugins.tooltip.usePointStyle = true;
}

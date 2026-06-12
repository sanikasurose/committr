import { ApplicationConfig, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { routes } from './app.routes';
import { applyChartDarkTheme } from './core/chart-theme';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    provideRouter(routes),
    provideCharts(withDefaultRegisterables()),
    // Must run after provideCharts so chart.js plugin defaults exist.
    provideAppInitializer(() => applyChartDarkTheme())
  ]
};

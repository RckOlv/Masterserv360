import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './interceptors/auth.interceptor';
import localeEsAr from '@angular/common/locales/es-AR';
import { registerLocaleData } from '@angular/common';
import { provideCharts } from 'ng2-charts';

// Registrar el locale para fechas en español
registerLocaleData(localeEsAr);

// URL base global (opcional, si la usas en otros lados)
export const API_URL = 'https://masterserv-backend.onrender.com/api';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    
    // Configuración de Gráficos (ng2-charts)
    provideCharts(),
    
    // Configuración Regional (Español Argentina)
    { provide: LOCALE_ID, useValue: 'es-AR' }
  ]
};
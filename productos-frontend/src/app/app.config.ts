import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './interceptors/auth.interceptor';

// --- Mentor: INICIO DE LA CORRECCIÓN ---
import { provideCharts } from 'ng2-charts'; // 1. Quitar 'withDefaultInteractions'
// --- Mentor: FIN DE LA CORRECCIÓN ---

// URL base del backend (si querés usarla en el servicio también)
export const API_URL = 'http://localhost:8080';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),

    // --- Mentor: INICIO DE LA CORRECCIÓN ---
    provideCharts() // 2. Llamar a la función sin argumentos
    // --- Mentor: FIN DE LA CORRECCIÓN ---
  ]
};
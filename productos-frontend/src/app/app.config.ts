import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
// Importa las funciones necesarias
import { provideHttpClient, withInterceptors } from '@angular/common/http';
// Importa TU interceptor funcional
import { authInterceptor } from './interceptors/auth.interceptor'; 

// Define la constante API_URL aquí si no la tienes en otro lado
export const API_URL = 'http://localhost:8080'; // La URL de tu backend

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }), // Esto está bien
    provideRouter(routes),                                // Esto está bien

    // -- ESTA ES LA FORMA CORRECTA --
    // Provee HttpClient y registra los interceptores funcionales
    provideHttpClient(withInterceptors([
      authInterceptor // Simplemente pasas la función
    ]))
  ]
};
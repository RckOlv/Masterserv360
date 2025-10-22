import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';
import { AuthInterceptor } from './app/interceptors/auth.interceptor';

bootstrapApplication(App, {
  providers: [
    // ✅ Registrar el interceptor
    provideHttpClient(withInterceptors([ (req, next) => {
      const token = localStorage.getItem('token');
      if (token) {
        const authReq = req.clone({
          setHeaders: { Authorization: `Bearer ${token}` }
        });
        return next(authReq);
      }
      return next(req);
    }])),

    // ✅ Proveer rutas
    provideRouter(routes)
  ]
}).catch(err => console.error(err));

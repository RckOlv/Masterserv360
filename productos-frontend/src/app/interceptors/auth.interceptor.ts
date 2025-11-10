import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http'; // <-- Importar HttpErrorResponse
import { inject } from '@angular/core';
import { Router } from '@angular/router'; // <-- ¡IMPORTAR ROUTER!
import { catchError, throwError } from 'rxjs'; // <-- ¡IMPORTAR OPERADORES RxJS!
import { AuthService } from '../service/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  
  // --- Inyectamos los servicios ---
  const authService = inject(AuthService);
  const router = inject(Router); // <-- ¡Inyectar Router!
  
  // --- Leemos los datos ---
  const token = authService.getToken();
  const apiUrlBase = authService.getApiUrlBase();
  const isApiUrl = req.url.startsWith(apiUrlBase);
  const isAuthRoute = req.url.includes('/api/auth/login') || 
                      req.url.includes('/api/auth/register');

  let request = req;

  // 1. Clonar y añadir el token (si es necesario)
  if (token && isApiUrl && !isAuthRoute) { 
    // (Tu lógica de logueo aquí...)
    console.log(' -> Agregando Authorization Header');
    request = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  } else {
    console.log(' -> No se agrega Authorization');
  }

  // 2. Enviar la petición (clonada o original) y AHORA CAPTURAR ERRORES
  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      
      // 3. --- ¡AQUÍ ESTÁ LA NUEVA LÓGICA! ---
      // Si el backend nos dice que el token es malo (401) o no tenemos permisos (403)
      // Y NO es un error de login (porque si es de login, solo mostramos "credenciales inválidas")
      if ((error.status === 401 || error.status === 403) && !isAuthRoute) {
        
        // Es un error de autenticación (TOKEN EXPIRADO o inválido)
        console.error('Interceptor: Token expirado o inválido. Cerrando sesión.', error.message);
        
        // 4. Limpiamos la sesión (borra el token) y redirigimos al login
        authService.logout(); 
      }
      
      // 5. Relanzamos el error para que el servicio que lo llamó lo maneje
      return throwError(() => error); 
    })
  );
};
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../service/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();
  const apiUrlBase = authService.getApiUrlBase();
  const isApiUrl = req.url.startsWith(apiUrlBase);

  console.log(`[Interceptor] Petición a: ${req.url}`);
  console.log(` -> ¿Es API URL?: ${isApiUrl}`);
  console.log(` -> ¿Hay token?: ${!!token}`);

  if (token && isApiUrl) {
    console.log(' -> Agregando Authorization Header');
    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(cloned);
  }

  console.log(' -> No se agrega Authorization');
    return next(req);
};

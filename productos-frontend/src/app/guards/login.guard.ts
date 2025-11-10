import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../service/auth.service';

/**
 * Previene que un usuario LOGUEADO acceda a las páginas de Login/Registro.
 */
export const LoginGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasToken()) {
    // ¡Usuario ya logueado! Redirigir a su "home"
    if (authService.hasRole('ROLE_ADMIN') || authService.hasRole('ROLE_VENDEDOR')) {
      router.navigate(['/pos/dashboard']);
    } else if (authService.hasRole('ROLE_CLIENTE')) {
      router.navigate(['/portal/catalogo']);
    } else {
      router.navigate(['/']); // Fallback
    }
    return false; // No puede activar la ruta de login
  }
  
  return true; // No está logueado, SÍ puede ver el login
};
import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../service/auth.service';
import { mostrarToast } from '../utils/toast'; // (Asumo que tienes tu toast)

/**
 * Este es un Guardia de Rol inteligente (CanActivateFn).
 * Se configura en app.routes.ts y lee el array 'data.roles' de la ruta.
 */
export const AuthGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. Obtener los roles requeridos para esta ruta desde app.routes.ts
  const rolesRequeridos: string[] = route.data['roles'] || [];

  // 2. Verificar si el usuario está logueado
  if (!authService.hasToken()) {
    mostrarToast('Debe iniciar sesión para acceder', 'warning');
    router.navigate(['/login']);
    return false;
  }

  // 3. Si se requieren roles, verificar si el usuario los tiene
  if (rolesRequeridos.length > 0) {
    
    // Usamos 'some' para ver si el usuario tiene AL MENOS UNO de los roles requeridos
    const tieneRol = rolesRequeridos.some(rol => authService.hasRole(rol));

    if (tieneRol) {
      return true; // ¡Acceso concedido!
    } else {
      // El usuario está logueado, pero no tiene el rol correcto
      mostrarToast('No tiene permisos para acceder a esta sección', 'danger');
      // Lo enviamos al dashboard (o a donde corresponda)
      router.navigate(['/dashboard']); // O '/portal' si es un cliente
      return false;
    }
  }

  // 4. Si la ruta no requiere roles (solo estar logueado), se permite
  return true; 
};
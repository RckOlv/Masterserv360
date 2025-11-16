import { Directive, Input, TemplateRef, ViewContainerRef, inject } from '@angular/core';
import { AuthService } from '../service/auth.service'; // Asegúrate de que esta ruta sea correcta

@Directive({
  selector: '[appHasPermission]', // Usaremos *appHasPermission="'PERMISO'"
  standalone: true
})
export class HasPermissionDirective {

  private authService = inject(AuthService);
  private templateRef = inject(TemplateRef<any>);
  private viewContainer = inject(ViewContainerRef);

  private currentPermissions: string[] = [];

  constructor() {
    // Nos suscribimos a los permisos del AuthService
    // Si los permisos cambian (ej. al hacer logout), la directiva se re-evaluará
    this.authService.currentUserPermissions$.subscribe(permissions => {
      this.currentPermissions = permissions;
      this.updateView(); // Re-evaluar la vista cuando los permisos cambien
    });
  }

  @Input('appHasPermission')
  set permissions(requiredPermissions: string | string[] | null) {
    this.requiredPermissions = requiredPermissions ? (Array.isArray(requiredPermissions) ? requiredPermissions : [requiredPermissions]) : [];
    this.updateView(); // Re-evaluar la vista cuando el Input cambie
  }

  private requiredPermissions: string[] = [];
  private isViewVisible = false;

  private updateView(): void {
    // Si no se requieren permisos, no mostramos nada (por seguridad)
    if (this.requiredPermissions.length === 0) {
      if (this.isViewVisible) {
        this.viewContainer.clear();
        this.isViewVisible = false;
      }
      return;
    }

    // Verificamos si el usuario tiene AL MENOS UNO de los permisos requeridos
    const hasPermission = this.requiredPermissions.some(p => this.currentPermissions.includes(p));

    if (hasPermission && !this.isViewVisible) {
      // Si tiene permiso y no se está mostrando, la creamos
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.isViewVisible = true;
    } else if (!hasPermission && this.isViewVisible) {
      // Si NO tiene permiso y se está mostrando, la borramos
      this.viewContainer.clear();
      this.isViewVisible = false;
    }
  }
}
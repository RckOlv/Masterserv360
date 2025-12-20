import { Component, inject, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { HasPermissionDirective } from '../../directives/has-permission.directive';
import { Observable, of } from 'rxjs'; 

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, HasPermissionDirective],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent implements OnInit {

  private authService = inject(AuthService);
  private router = inject(Router);

  // --- VARIABLES DEL USUARIO (Observables para el pipe | async) ---
  // Las inicializamos vacías para evitar errores de "undefined"
  public userName$: Observable<string> = of('Usuario');
  public userRoleLabel$: Observable<string> = of('Invitado');

  // --- ESTADOS DE LOS MENÚS ---
  public isUserMenuOpen = false;
  public isMobileMenuOpen = false; // <--- Lo nuevo para el modo celular

  // --- SECCIONES DEL ACORDEÓN ---
  public sections: { [key: string]: boolean } = {
    general: true,
    inventario: false,
    compras: false,
    admin: false
  };

  ngOnInit(): void {
    // LÓGICA SEGURA: Usamos el mismo método que arregló el formulario de pedidos
    const tokenData = this.authService.getDecodedToken();

    if (tokenData) {
      // 1. Construimos el nombre
      const nombre = tokenData.nombre || '';
      const apellido = tokenData.apellido || '';
      const nombreCompleto = `${nombre} ${apellido}`.trim() || 'Usuario Sistema';
      
      // 2. Construimos el rol
      let rol = 'INVITADO';
      if (tokenData.roles && Array.isArray(tokenData.roles) && tokenData.roles.length > 0) {
         // Quitamos el prefijo ROLE_ para que se vea bonito (ej: ADMIN en vez de ROLE_ADMIN)
         rol = tokenData.roles[0].replace('ROLE_', '');
      }

      // 3. Asignamos como Observables (usando 'of') para que el HTML lo lea sin cambios
      this.userName$ = of(nombreCompleto);
      this.userRoleLabel$ = of(rol);
    }
  }

  // --- MÉTODOS DEL MENÚ ---

  toggleUserMenu() {
    this.isUserMenuOpen = !this.isUserMenuOpen;
  }

  toggleSection(section: string) {
    // Si quieres que funcione como acordeón estricto (cierra los otros al abrir uno):
     Object.keys(this.sections).forEach(key => {
       if (key !== section) this.sections[key] = false;
     });
    // Alterna el estado de la sección clickeada
    this.sections[section] = !this.sections[section];
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  // --- MÉTODOS RESPONSIVOS (MOBILE) ---
  
  toggleMobileMenu() {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu() {
    this.isMobileMenuOpen = false;
  }

  // Detectar clicks fuera o en enlaces para cerrar el menú en celular
  @HostListener('document:click', ['$event'])
  onClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    // Si la pantalla es pequeña (<= 992px) y se hizo clic en un enlace (<a>)
    if (window.innerWidth <= 992 && target.closest('a')) {
       this.closeMobileMenu();
    }
  }
}
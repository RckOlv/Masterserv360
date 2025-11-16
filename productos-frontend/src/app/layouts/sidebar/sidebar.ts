import { Component, inject } from '@angular/core'; 
import { RouterModule, Router } from '@angular/router'; 
import { CommonModule, NgIf, NgFor } from '@angular/common';
import { AuthService } from '../../service/auth.service'; 

// --- Mentor: INICIO DE LA MODIFICACIÓN ---
// 1. Importar la nueva directiva de permisos
import { HasPermissionDirective } from '../../directives/has-permission.directive';
// --- Mentor: FIN DE LA MODIFICACIÓN ---

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    RouterModule, 
    NgIf, 
    NgFor, 
    CommonModule,
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 2. Añadir la directiva a los imports del componente
    HasPermissionDirective
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
  ], 
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent {
  sidebarToggled = false;

  // --- ¡INYECCIÓN DE SERVICIOS! ---
  private authService = inject(AuthService); // Se mantiene para el logout
  private router = inject(Router);
  
  // --- Mentor: INICIO DE LA MODIFICACIÓN ---
  // 3. 'isAdmin' se elimina por completo
  // public isAdmin = this.authService.hasRole('ROLE_ADMIN');
  // --- Mentor: FIN DE LA MODIFICACIÓN ---

  toggleSidebar() {
    this.sidebarToggled = !this.sidebarToggled;
  }

  // --- ¡NUEVO MÉTODO DE LOGOUT! ---
  logout(): void {
    this.authService.logout();
    // (El authService.logout() ya redirige a /login)
    this.router.navigate(['/login']); 
  }
}
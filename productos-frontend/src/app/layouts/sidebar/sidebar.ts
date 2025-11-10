import { Component, inject } from '@angular/core'; // <-- Importar inject
import { RouterModule, Router } from '@angular/router'; // <-- Importar Router
import { CommonModule, NgIf, NgFor } from '@angular/common';
import { AuthService } from '../../service/auth.service'; // <-- ¡IMPORTAR AUTHSERVICE!

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterModule, NgIf, NgFor, CommonModule], // <-- Añadir CommonModule
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent {
  sidebarToggled = false;

  // --- ¡INYECCIÓN DE SERVICIOS! ---
  private authService = inject(AuthService);
  private router = inject(Router);
  
  // --- ¡LÓGICA PARA SABER EL ROL! ---
  public isAdmin = this.authService.hasRole('ROLE_ADMIN');

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
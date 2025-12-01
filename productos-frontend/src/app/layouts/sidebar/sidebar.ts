import { Component, OnInit, inject } from '@angular/core'; 
import { RouterModule, Router } from '@angular/router'; 
import { CommonModule } from '@angular/common';
import { AuthService } from '../../service/auth.service'; 
import { HasPermissionDirective } from '../../directives/has-permission.directive';
import { BehaviorSubject } from 'rxjs';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    RouterModule,  
    CommonModule,
    HasPermissionDirective
  ], 
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent implements OnInit {
  
  private authService = inject(AuthService);
  private router = inject(Router);

  // Datos del Usuario
  public userName$ = new BehaviorSubject<string>('Usuario');
  public userRoleLabel$ = new BehaviorSubject<string>('');

  // MENTOR: Estado del menú de usuario (Perfil/Logout)
  public isUserMenuOpen = false;

  // Estado de las secciones del menú principal
  public sections: { [key: string]: boolean } = {
    general: true,
    inventario: false,
    compras: false,
    admin: false
  };
  
  ngOnInit(): void {
    const token = this.authService.getDecodedToken();
    if (token) {
        const nombreDisplay = (token.nombre && token.apellido) 
            ? `${token.nombre} ${token.apellido}` 
            : token.sub;
        this.userName$.next(nombreDisplay);

        if (token.roles && token.roles.length > 0) {
            const role = token.roles[0];
            if (role === 'ROLE_ADMIN') this.userRoleLabel$.next('Administrador');
            else if (role === 'ROLE_VENDEDOR') this.userRoleLabel$.next('Vendedor');
            else this.userRoleLabel$.next('Usuario');
        }
    }
  }

  toggleSection(sectionName: string): void {
    this.sections[sectionName] = !this.sections[sectionName];
  }

  // MENTOR: Nuevo método para alternar el menú de perfil
  toggleUserMenu(): void {
    this.isUserMenuOpen = !this.isUserMenuOpen;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']); 
  }
}
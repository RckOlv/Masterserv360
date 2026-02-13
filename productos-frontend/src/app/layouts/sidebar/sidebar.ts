import { Component, inject, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { HasPermissionDirective } from '../../directives/has-permission.directive';
import { Observable, of, Subscription, interval } from 'rxjs'; 
import { switchMap } from 'rxjs/operators';
// ✅ IMPORTAR SERVICIO
import { AlertaService, Alerta } from '../../service/alerta.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, HasPermissionDirective],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent implements OnInit, OnDestroy {

  private authService = inject(AuthService);
  private alertaService = inject(AlertaService); // ✅ INYECCIÓN
  private router = inject(Router);

  // --- VARIABLES DEL USUARIO ---
  public userName$: Observable<string> = of('Usuario');
  public userRoleLabel$: Observable<string> = of('Invitado');

  // --- ESTADOS DE LOS MENÚS ---
  public isUserMenuOpen = false;
  public isMobileMenuOpen = false;

  // --- VARIABLES DE ALERTAS (NUEVO) ---
  public alertas: Alerta[] = [];
  public showNotifications = false;
  private pollingSub?: Subscription;

  // --- SECCIONES DEL ACORDEÓN ---
  public sections: { [key: string]: boolean } = {
    general: true,
    inventario: false,
    compras: false,
    admin: false
  };

  ngOnInit(): void {
    // 1. Cargar Datos del Usuario
    const tokenData = this.authService.getDecodedToken();

    if (tokenData) {
      const nombre = tokenData.nombre || '';
      const apellido = tokenData.apellido || '';
      const nombreCompleto = `${nombre} ${apellido}`.trim() || 'Usuario Sistema';
      
      let rol = 'INVITADO';
      if (tokenData.roles && Array.isArray(tokenData.roles) && tokenData.roles.length > 0) {
         rol = tokenData.roles[0].replace('ROLE_', '');
      }

      this.userName$ = of(nombreCompleto);
      this.userRoleLabel$ = of(rol);
    }

    // 2. INICIAR POLLING DE ALERTAS (Cada 15 seg)
    this.cargarAlertas();
    this.pollingSub = interval(15000)
      .pipe(switchMap(() => this.alertaService.getNoLeidas()))
      .subscribe({
        next: (data) => this.alertas = data,
        error: (e) => console.error('Error polling alertas:', e)
      });
  }

  ngOnDestroy(): void {
    // Evitar fugas de memoria
    this.pollingSub?.unsubscribe();
  }

  // --- MÉTODOS ALERTAS ---

  cargarAlertas() {
    this.alertaService.getNoLeidas().subscribe(data => this.alertas = data);
  }

  toggleNotifications() {
    this.showNotifications = !this.showNotifications;
    // Si abrimos notificaciones, cerramos menú usuario para no encimar
    if (this.showNotifications) this.isUserMenuOpen = false;
  }

  clickAlerta(alerta: Alerta) {
    // Marcamos como leída en backend
    this.alertaService.marcarLeida(alerta.id).subscribe(() => {
      
      // COMENTAMOS ESTA LÍNEA PARA QUE NO DESAPAREZCA AL INSTANTE:
      // this.alertas = this.alertas.filter(a => a.id !== alerta.id);
      
      // Cerramos el menú
      this.showNotifications = false;
      
      // Navegamos (si corresponde)
      if (alerta.urlDestino) {
        this.router.navigateByUrl(alerta.urlDestino);
      }
    });
  }

  limpiarTodas() {
    this.alertaService.marcarTodasLeidas().subscribe(() => {
      this.alertas = [];
      this.showNotifications = false;
    });
  }

  // --- MÉTODOS DEL MENÚ USUARIO ---

  toggleUserMenu() {
    this.isUserMenuOpen = !this.isUserMenuOpen;
    // Si abrimos menú usuario, cerramos notificaciones
    if (this.isUserMenuOpen) this.showNotifications = false;
  }

  toggleSection(section: string) {
     Object.keys(this.sections).forEach(key => {
       if (key !== section) this.sections[key] = false;
     });
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

  @HostListener('document:click', ['$event'])
  onClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    
    // Cerrar menú móvil al hacer click en enlace
    if (window.innerWidth <= 992 && target.closest('a')) {
       this.closeMobileMenu();
    }

    // Cerrar dropdowns si clickeo afuera
    if (!target.closest('.user-info') && !target.closest('.notification-wrapper')) {
        this.isUserMenuOpen = false;
        this.showNotifications = false;
    }
  }
}
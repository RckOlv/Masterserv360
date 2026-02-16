import { Component, inject, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../service/auth.service';
import { HasPermissionDirective } from '../../directives/has-permission.directive';
import { Observable, of, Subscription, interval } from 'rxjs'; 
import { switchMap } from 'rxjs/operators';
import { AlertaService, Alerta } from '../../service/alerta.service';
import { environment } from '../../../environments/environment';
// ✅ IMPORTAR EL SERVICIO DE CONFIGURACIÓN CORRECTO
import { ConfiguracionService } from '../../service/configuracion.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, HasPermissionDirective],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent implements OnInit, OnDestroy {

  private authService = inject(AuthService);
  private alertaService = inject(AlertaService);
  private router = inject(Router);
  private http = inject(HttpClient);
  // ✅ INYECTAMOS EL SERVICIO COMPARTIDO
  private configService = inject(ConfiguracionService);

  // --- VARIABLES DEL USUARIO ---
  public userName$: Observable<string> = of('Usuario');
  public userRoleLabel$: Observable<string> = of('Invitado');

  // --- VARIABLES DE CONFIGURACIÓN (LOGO) ---
  // ✅ Iniciamos con el logo por defecto
  public logoUrl: string = '/images/Masterserv_Header2.png'; 

  // --- ESTADOS DE LOS MENÚS ---
  public isUserMenuOpen = false;
  public isMobileMenuOpen = false;

  // --- VARIABLES DE ALERTAS ---
  public alertas: Alerta[] = [];
  public showNotifications = false;
  private pollingSub?: Subscription;
  private logoSub?: Subscription; // Para desuscribirnos del logo

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

    // 2. ✅ SUSCRIPCIÓN AL LOGO EN TIEMPO REAL
    // Nos conectamos a la "Radio" del servicio. 
    // Si cambia el logo en otra pantalla, esto se actualiza solo.
    this.logoSub = this.configService.logo$.subscribe(url => {
      if (url && url.trim() !== '') {
        this.logoUrl = url;
      } else {
        // Si viene vacío, volvemos al default por seguridad
        this.logoUrl = '/images/Masterserv_Header2.png';
      }
    });

    // 3. INICIAR POLLING DE ALERTAS (Cada 15 seg)
    this.cargarAlertas();
    this.pollingSub = interval(15000)
      .pipe(switchMap(() => this.alertaService.getNoLeidas()))
      .subscribe({
        next: (data) => this.alertas = data,
        error: (e) => console.error('Error polling alertas:', e)
      });
  }
  

  ngOnDestroy(): void {
    this.pollingSub?.unsubscribe();
    this.logoSub?.unsubscribe(); // Limpiamos la suscripción del logo
  }

  // --- MÉTODOS ALERTAS ---

  cargarAlertas() {
    this.alertaService.getNoLeidas().subscribe(data => this.alertas = data);
  }

  toggleNotifications() {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications) this.isUserMenuOpen = false;
  }

  clickAlerta(alerta: Alerta) {
    this.alertaService.marcarLeida(alerta.id).subscribe(() => {
      this.showNotifications = false;
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
    
    if (window.innerWidth <= 992 && target.closest('a')) {
       this.closeMobileMenu();
    }

    if (!target.closest('.user-info') && !target.closest('.notification-wrapper')) {
        this.isUserMenuOpen = false;
        this.showNotifications = false;
    }
  }
}
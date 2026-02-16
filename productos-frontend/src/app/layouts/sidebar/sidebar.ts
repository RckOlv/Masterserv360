import { Component, inject, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http'; // ✅ IMPORTAR HTTP
import { AuthService } from '../../service/auth.service';
import { HasPermissionDirective } from '../../directives/has-permission.directive';
import { Observable, of, Subscription, interval } from 'rxjs'; 
import { switchMap } from 'rxjs/operators';
import { AlertaService, Alerta } from '../../service/alerta.service';
import { environment } from '../../../environments/environment'; // ✅ Asegurate de tener esto

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
  private http = inject(HttpClient); // ✅ INYECCIÓN DE HTTP

  // --- VARIABLES DEL USUARIO ---
  public userName$: Observable<string> = of('Usuario');
  public userRoleLabel$: Observable<string> = of('Invitado');

  // --- VARIABLES DE CONFIGURACIÓN (LOGO) ---
  // ✅ Iniciamos con el logo por defecto por si falla la API
  public logoUrl: string = '/images/Masterserv_Header2.png'; 

  // --- ESTADOS DE LOS MENÚS ---
  public isUserMenuOpen = false;
  public isMobileMenuOpen = false;

  // --- VARIABLES DE ALERTAS ---
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

    // 2. ✅ CARGAR LOGO DE LA EMPRESA
    this.obtenerConfiguracionEmpresa();

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
  }

  // --- ✅ MÉTODO PARA OBTENER EL LOGO ---
  obtenerConfiguracionEmpresa() {
    // Llamamos al endpoint público que creamos en Java
    this.http.get<any>(`${environment.apiUrl}/configuracion/publica`).subscribe({
      next: (config) => {
        // Si viene un logoUrl y no está vacío, lo usamos
        if (config && config.logoUrl && config.logoUrl.trim() !== '') {
          this.logoUrl = config.logoUrl;
        }
      },
      error: (err) => {
        console.warn('⚠️ No se pudo cargar la config de empresa, usando logo default.', err);
      }
    });
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
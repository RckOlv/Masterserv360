import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { PuntosService } from '../../service/puntos.service';
import { CatalogoService } from '../../service/catalogo.service'; // <-- ¡IMPORTAR SERVICIO DE CATÁLOGO!
import { SaldoPuntosDTO } from '../../models/saldo-puntos.model';
import { Observable, BehaviorSubject, switchMap, of, catchError, map } from 'rxjs';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './public-layout.html',
  styleUrls: ['./public-layout.css']
})
export class PublicLayoutComponent implements OnInit {

  private authService = inject(AuthService);
  private puntosService = inject(PuntosService);
  private catalogoService = inject(CatalogoService); // <-- ¡INYECTAR!
  private router = inject(Router);

  public isLoggedIn$: Observable<boolean>;
  public userEmail$ = new BehaviorSubject<string | null>(null);
  public saldoPuntos$ = new BehaviorSubject<number>(0);
  public userRole$ = new BehaviorSubject<string | null>(null);

  constructor() {
    this.isLoggedIn$ = this.authService.isLoggedIn$;
  }

  ngOnInit(): void {
    // (Tu lógica de ngOnInit está perfecta, no se toca)
    this.isLoggedIn$.pipe(
      switchMap(isLoggedIn => {
        if (isLoggedIn) {
          const token = this.authService.getDecodedToken();
          if (token) {
            this.userEmail$.next(token.sub); 
            if (this.authService.hasRole('ROLE_CLIENTE')) {
              this.userRole$.next('CLIENTE');
              return this.puntosService.getMiSaldo().pipe(
                map(saldo => saldo.saldoPuntos),
                catchError(() => of(0))
              );
            } else if (this.authService.hasRole('ROLE_ADMIN')) {
              this.userRole$.next('ADMIN');
              return of(0);
            } else if (this.authService.hasRole('ROLE_VENDEDOR')) {
              this.userRole$.next('VENDEDOR');
              return of(0);
            }
          }
        }
        this.userEmail$.next(null);
        this.userRole$.next(null);
        return of(0);
      })
    ).subscribe(saldo => {
      this.saldoPuntos$.next(saldo);
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // --- ¡NUEVO MÉTODO PARA LA BARRA DE BÚSQUEDA! ---
  /**
   * Se llama cuando el usuario busca en la Navbar.
   * Guarda el término en el servicio y navega al catálogo.
   */
  onBuscar(termino: string): void {
    // 1. Guardamos el término en el servicio compartido
    this.catalogoService.setSearchTerm(termino);
    
    // 2. Aseguramos que el usuario esté en la página del catálogo
    this.router.navigate(['/portal/catalogo']);
  }
}
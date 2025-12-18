import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { PuntosService } from '../../service/puntos.service';
import { CatalogoService } from '../../service/catalogo.service'; 
import { Observable, BehaviorSubject, switchMap, of, catchError, map } from 'rxjs';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './public-layout.html',
  styleUrls: ['./public-layout.css']
})
export class PublicLayoutComponent implements OnInit {

  public authService = inject(AuthService); // Público para el HTML
  private puntosService = inject(PuntosService);
  private catalogoService = inject(CatalogoService);
  private router = inject(Router);

  public isLoggedIn$: Observable<boolean>;
  public userEmail$ = new BehaviorSubject<string | null>(null);
  public userName$ = new BehaviorSubject<string | null>(null);
  public userRoleLabel$ = new BehaviorSubject<string | null>(null);
  public saldoPuntos$ = new BehaviorSubject<number>(0);
  public userRole$ = new BehaviorSubject<string | null>(null);

  constructor() {
    this.isLoggedIn$ = this.authService.isLoggedIn$;
  }

  ngOnInit(): void {
    this.isLoggedIn$.pipe(
      switchMap(isLoggedIn => {
        if (isLoggedIn) {
          const token = this.authService.getDecodedToken();
          if (token) {
            const nombreDisplay = (token.nombre && token.apellido) 
                                ? `${token.nombre} ${token.apellido}` 
                                : token.sub;
            
            this.userEmail$.next(token.sub); 
            this.userName$.next(nombreDisplay);

            if (this.authService.hasRole('ROLE_CLIENTE')) {
              this.userRole$.next('CLIENTE');
              this.userRoleLabel$.next('Cliente');
              return this.puntosService.getMiSaldo().pipe(
                map(saldo => saldo.saldoPuntos),
                catchError(() => of(0))
              );
            } else if (this.authService.hasRole('ROLE_ADMIN')) {
              this.userRole$.next('ADMIN');
              this.userRoleLabel$.next('Administrador');
              return of(0);
            } else if (this.authService.hasRole('ROLE_VENDEDOR')) {
              this.userRole$.next('VENDEDOR');
              this.userRoleLabel$.next('Vendedor');
              return of(0);
            }
          }
        }
        // Si no está logueado, limpiamos
        this.userEmail$.next(null);
        this.userName$.next(null);
        this.userRole$.next(null);
        this.userRoleLabel$.next(null);
        return of(0);
      })
    ).subscribe(saldo => {
      this.saldoPuntos$.next(saldo);
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  onBuscar(termino: string): void {
    this.catalogoService.setSearchTerm(termino);
    this.router.navigate(['/catalogo']);
  }
}
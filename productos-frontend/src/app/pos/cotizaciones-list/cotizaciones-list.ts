import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { Router, RouterLink } from '@angular/router'; 
import { Observable, EMPTY, of } from 'rxjs'; 
import { catchError, tap } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';

// Modelos y Servicios
import { CotizacionAdminDTO } from '../../models/cotizacion-admin.model';
import { CotizacionService } from '../../service/cotizacion.service';

// Utils
import { mostrarToast } from '../../utils/toast';

// --- Mentor: INICIO DE LA MODIFICACIÓN ---
// 1. Importar la nueva directiva de permisos
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 
// --- Mentor: FIN DE LA MODIFICACIÓN ---

@Component({
  selector: 'app-cotizaciones-list',
  standalone: true,
  imports: [
    CommonModule, 
    RouterLink,
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 2. Añadir la directiva a los imports del componente
    HasPermissionDirective
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
  ], 
  templateUrl: './cotizaciones-list.html',
  styleUrls: ['./cotizaciones-list.css']
})
export default class CotizacionesListComponent implements OnInit {

  private cotizacionService = inject(CotizacionService);
  private router = inject(Router);

  public cotizaciones$: Observable<CotizacionAdminDTO[]>;
  public isLoading = true; 

  constructor() {
    this.cotizaciones$ = EMPTY; 
  }

  ngOnInit(): void {
    this.loadCotizaciones();
  }

  loadCotizaciones(): void {
    this.isLoading = true; // <-- Ponemos el loading en true al INICIAR la carga
    
    this.cotizaciones$ = this.cotizacionService.getCotizacionesRecibidas().pipe(
      tap(() => {
        this.isLoading = false; 
      }),
      catchError((err: HttpErrorResponse) => {
        this.isLoading = false; 
        const errorMsg = err.error?.message || 'Error al cargar las cotizaciones recibidas.';
        mostrarToast(errorMsg, 'danger'); 
        return of([]); 
      })
    );
  }

  verDetalle(id: number): void {
    this.router.navigate(['/pos/cotizaciones', id]); 
  }
}
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { Router, RouterLink } from '@angular/router'; 
import { Observable, EMPTY, of } from 'rxjs'; // <-- ¡IMPORTAMOS 'of'!
import { catchError, tap } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';

// Modelos y Servicios
import { CotizacionAdminDTO } from '../../models/cotizacion-admin.model';
import { CotizacionService } from '../../service/cotizacion.service';

// Utils
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-cotizaciones-list',
  standalone: true,
  imports: [CommonModule, RouterLink], 
  templateUrl: './cotizaciones-list.html',
  styleUrls: ['./cotizaciones-list.css']
})
export default class CotizacionesListComponent implements OnInit {

  private cotizacionService = inject(CotizacionService);
  private router = inject(Router);

  public cotizaciones$: Observable<CotizacionAdminDTO[]>;
  public isLoading = true; // <-- El valor inicial es 'true'

  constructor() {
    console.log('1. Constructor() - Componente Creado');
    this.cotizaciones$ = EMPTY; 
  }

  ngOnInit(): void {
    console.log('2. ngOnInit() - Componente Iniciado');
    this.loadCotizaciones();
  }

  loadCotizaciones(): void {
    console.log('3. loadCotizaciones() - Llamando al servicio...');
    // ¡YA NO ponemos isLoading = true aquí!
    
    this.cotizaciones$ = this.cotizacionService.getCotizacionesRecibidas().pipe(
      tap(() => {
        // Solo lo ponemos en 'false' cuando los datos LLEGAN
        console.log('4. pipe() - ¡Datos recibidos! Poniendo isLoading = false');
        this.isLoading = false; 
      }),
      catchError((err: HttpErrorResponse) => {
        // O también si hay un ERROR
        console.error('5. catchError() - ¡ERROR ATRAPADO!', err);
        this.isLoading = false; 
        const errorMsg = err.error?.message || 'Error al cargar las cotizaciones recibidas.';
        mostrarToast(errorMsg, 'danger'); 
        return of([]); // <-- Devolvemos un array vacío para que el | async no falle
      })
    );
    
    console.log('3b. loadCotizaciones() - Observable asignado.');
  }

  verDetalle(id: number): void {
    this.router.navigate(['/pos/cotizaciones', id]); 
  }
}
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common'; 
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, tap, startWith } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';

// Modelos y Servicios
import { CotizacionAdminDTO } from '../../models/cotizacion-admin.model';
import { ItemCotizacionAdminDTO } from '../../models/item-cotizacion-admin.model';
import { CotizacionService } from '../../service/cotizacion.service';

// Utils
import { mostrarToast } from '../../utils/toast';
import { HasPermissionDirective } from '../../directives/has-permission.directive';

// ✅ 1. IMPORTAR SWEETALERT2
import Swal from 'sweetalert2';

@Component({
  selector: 'app-cotizacion-detalle',
  standalone: true,
  imports: [
    CommonModule, 
    RouterLink,
    HasPermissionDirective 
  ], 
  providers: [CurrencyPipe], 
  templateUrl: './cotizacion-detalle.html',
  styleUrls: ['./cotizacion-detalle.css']
})
export default class CotizacionDetalleComponent implements OnInit {

  private cotizacionService = inject(CotizacionService);
  private router = inject(Router);
  private route = inject(ActivatedRoute); 
  private currencyPipe = inject(CurrencyPipe); 

  public cotizacion$: Observable<CotizacionAdminDTO | null>;
  public isLoading = true;
  public isProcessing = false;
  
  private reloadCotizacion$ = new Subject<void>();

  constructor() {
    this.cotizacion$ = EMPTY; 
  }

  ngOnInit(): void {
    this.cotizacion$ = this.reloadCotizacion$.pipe(
      startWith(undefined), 
      tap(() => this.isLoading = true),
      switchMap(() => this.route.paramMap),
      switchMap(params => {
        const id = params.get('id');
        if (!id) {
          mostrarToast('No se encontró el ID de la cotización.', 'danger');
          this.router.navigate(['/pos/cotizaciones']);
          return EMPTY;
        }
        return this.cotizacionService.getCotizacionById(Number(id));
      }),
      tap(() => this.isLoading = false),
      catchError((err: HttpErrorResponse) => {
        this.isLoading = false;
        const errorMsg = err.error?.message || 'Error al cargar la cotización.';
        mostrarToast(errorMsg, 'danger');
        this.router.navigate(['/pos/cotizaciones']); 
        return EMPTY;
      })
    );
  }

  recargarDatos(): void {
    this.reloadCotizacion$.next();
  }

  // --- MÉTODOS CON SWEETALERT2 ---

  onCancelarItem(item: ItemCotizacionAdminDTO): void {
    Swal.fire({
      title: '¿Cancelar Item?',
      text: `Vas a cancelar "${item.productoNombre}". Esta acción no se puede deshacer.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, cancelar',
      cancelButtonText: 'Volver',
      background: '#1e1e1e', color: '#ffffff' // Estilo Dark
    }).then((result) => {
      if (result.isConfirmed) {
        this.isProcessing = true;
        this.cotizacionService.cancelarItem(item.id).subscribe({
          next: () => {
            mostrarToast('Item cancelado exitosamente.', 'success');
            this.recargarDatos(); 
            this.isProcessing = false;
          },
          error: (err: HttpErrorResponse) => {
            const errorMsg = err.error?.message || 'Error al cancelar el item.';
            mostrarToast(errorMsg, 'danger');
            this.isProcessing = false;
          }
        });
      }
    });
  }

  onCancelarCotizacion(cotizacion: CotizacionAdminDTO): void {
    Swal.fire({
      title: '¿Cancelar Cotización?',
      text: `Se cancelará TODA la cotización #${cotizacion.id}.`,
      icon: 'error',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, anular todo',
      cancelButtonText: 'No, esperar',
      background: '#1e1e1e', color: '#ffffff'
    }).then((result) => {
      if (result.isConfirmed) {
        this.isProcessing = true;
        this.cotizacionService.cancelarCotizacion(cotizacion.id).subscribe({
          next: () => {
            Swal.fire({
              title: 'Cancelada',
              text: 'La cotización ha sido anulada.',
              icon: 'success',
              background: '#1e1e1e', color: '#ffffff', confirmButtonColor: '#28a745'
            });
            this.router.navigate(['/pos/cotizaciones']); 
            this.isProcessing = false;
          },
          error: (err: HttpErrorResponse) => {
            const errorMsg = err.error?.message || 'Error al cancelar la cotización.';
            mostrarToast(errorMsg, 'danger');
            this.isProcessing = false;
          }
        });
      }
    });
  }

  /**
   * Acción: Confirma la cotización y genera el Pedido.
   * ✅ AHORA USA SWEETALERT2
   */
  onConfirmarCotizacion(cotizacion: CotizacionAdminDTO): void {
    const precioFormateado = this.currencyPipe.transform(cotizacion.precioTotalOfertado, 'ARS', 'symbol', '1.2-2');

    Swal.fire({
      title: '¿Confirmar Oferta?',
      html: `
        <p>Estás a punto de aceptar esta cotización por:</p>
        <h2 style="color: #28a745; margin: 10px 0;">${precioFormateado}</h2>
        <p style="font-size: 0.9em; color: #aaa;">Se generará un <b>Pedido Formal</b> automáticamente.</p>
      `,
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#28a745', // Verde éxito
      cancelButtonColor: '#d33',
      confirmButtonText: 'Sí, Generar Pedido',
      cancelButtonText: 'Cancelar',
      background: '#1e1e1e', // Fondo oscuro
      color: '#ffffff',      // Texto blanco
      showLoaderOnConfirm: true,
      preConfirm: () => {
        // Opcional: Bloquear UI mientras carga SweetAlert
        this.isProcessing = true;
      }
    }).then((result) => {
      if (result.isConfirmed) {
        
        this.cotizacionService.confirmarCotizacion(cotizacion.id).subscribe({
          next: (response: any) => { 
            this.isProcessing = false;
            
            // Alerta de Éxito
            Swal.fire({
              title: '¡Pedido Generado!',
              text: `Se creó el Pedido #${response.pedidoId} exitosamente.`,
              icon: 'success',
              confirmButtonText: 'Ir a Pedidos',
              confirmButtonColor: '#E41E26',
              background: '#1e1e1e', color: '#ffffff'
            }).then(() => {
              this.router.navigate(['/pos/pedidos']); 
            });
          },
          error: (err: HttpErrorResponse) => {
            this.isProcessing = false;
            const errorMsg = err.error?.message || 'Error al confirmar la cotización.';
            
            // Alerta de Error
            Swal.fire({
              title: 'Error',
              text: errorMsg,
              icon: 'error',
              background: '#1e1e1e', color: '#ffffff'
            });
          }
        });
      } else {
        this.isProcessing = false; // Resetear si cancela
      }
    });
  }
}
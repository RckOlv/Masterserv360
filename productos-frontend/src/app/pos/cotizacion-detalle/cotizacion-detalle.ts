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
import { PedidoDTO } from '../../models/pedido.model'; 

// Utils
import { mostrarToast } from '../../utils/toast';

// --- Mentor: INICIO DE LA MODIFICACIÓN ---
// 1. Importar la nueva directiva de permisos
import { HasPermissionDirective } from '../../directives/has-permission.directive';
// --- Mentor: FIN DE LA MODIFICACIÓN ---

@Component({
  selector: 'app-cotizacion-detalle',
  standalone: true,
  // 2. Añadir la directiva al array de imports
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

  onCancelarItem(item: ItemCotizacionAdminDTO): void {
    if (!confirm(`¿Seguro que quieres cancelar el item "${item.productoNombre}"?`)) {
      return;
    }

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

  onCancelarCotizacion(cotizacion: CotizacionAdminDTO): void {
    if (!confirm(`¿Seguro que quieres CANCELAR TODA la cotización #${cotizacion.id} del proveedor ${cotizacion.proveedorNombre}?`)) {
      return;
    }

    this.isProcessing = true;
    this.cotizacionService.cancelarCotizacion(cotizacion.id).subscribe({
      next: () => {
        mostrarToast('Cotización cancelada.', 'warning');
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

  /**
   * Acción: Confirma la cotización y genera el Pedido.
   */
  onConfirmarCotizacion(cotizacion: CotizacionAdminDTO): void {
    const precioFormateado = this.currencyPipe.transform(cotizacion.precioTotalOfertado, 'ARS', 'symbol', '1.2-2');

    if (!confirm(`¿Confirmar esta oferta por ${precioFormateado}? Se generará un Pedido formal.`)) {
      return;
    }

    this.isProcessing = true;
    this.cotizacionService.confirmarCotizacion(cotizacion.id).subscribe({
      next: (pedidoCreado: PedidoDTO) => { 
        mostrarToast(`¡Éxito! Pedido #${pedidoCreado.id} generado correctamente.`, 'success');
        this.router.navigate(['/pos/pedidos']); 
        this.isProcessing = false;
      },
      error: (err: HttpErrorResponse) => {
        const errorMsg = err.error?.message || 'Error al confirmar la cotización.';
        mostrarToast(errorMsg, 'danger');
        this.isProcessing = false;
      }
    });
  }
}
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; // Para *ngFor, *ngIf, etc.
import { RouterModule } from '@angular/router'; // Para [routerLink]
import { PedidoService } from '../../service/pedido.service';
import { PedidoDTO } from '../../models/pedido.model';
import { Page } from '../../models/page.model';
import { mostrarToast } from '../../utils/toast';

// --- Mentor: Imports Agregados ---
// import { AuthService } from '../../service/auth.service'; // Mentor: ELIMINADO
import { HttpErrorResponse } from '@angular/common/http';
// --- Mentor: INICIO DE LA MODIFICACIÓN ---
// 1. Importar la nueva directiva de permisos
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 
// --- Mentor: FIN DE LA MODIFICACIÓN ---

@Component({
  selector: 'app-pedidos-list',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule,
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 2. Añadir la directiva a los imports del componente
    HasPermissionDirective
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
  ], 
  templateUrl: './pedidos-list.html',
  styleUrls: ['./pedidos-list.css'] // Usará el nuevo CSS
})
export default class PedidosListComponent implements OnInit {

  // Inyección de dependencias
  private pedidoService = inject(PedidoService);
  // --- Mentor: ELIMINADA la inyección de AuthService ---
  // private authService = inject(AuthService);

  // Estado del componente
  public pedidosPage: Page<PedidoDTO> | null = null;
  public currentPage = 0;
  public pageSize = 10;
  public errorMessage: string | null = null;
  public isLoading = false;
  
  // --- Mentor: INICIO DE LA MODIFICACIÓN ---
  // 3. 'isAdmin' se elimina por completo
  // public isAdmin = false; 
  // --- Mentor: FIN DE LA MODIFICACIÓN ---

  constructor() {}

  ngOnInit(): void {
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 4. Esta línea se elimina
    // this.isAdmin = this.authService.hasRole('ROLE_ADMIN');
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
    this.cargarPedidos(); // Carga inicial
  }

  /**
   * Llama al servicio para cargar los pedidos paginados
   */
  cargarPedidos(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.pedidoService.listarPedidos(this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.pedidosPage = page;
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => { 
        console.error('Error al cargar pedidos:', err);
        this.handleError(err, 'cargar');
        this.isLoading = false;
      }
    });
  }

  /**
   * Marcar un pedido como COMPLETADO (Ingresa Stock)
   */
  marcarCompletado(id: number | undefined): void {
    if (!id) return;

    if (confirm('¿Seguro que deseas marcar este pedido como COMPLETADO? Esta acción ingresará el stock de los productos al inventario.')) {
      this.isLoading = true; 
      this.pedidoService.marcarCompletado(id).subscribe({
        next: () => {
          mostrarToast('Pedido completado. Stock actualizado.', 'success');
          this.cargarPedidos(); // Recargar la lista
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error al completar pedido:', err);
          this.handleError(err, 'completar');
          this.isLoading = false;
        }
      });
    }
  }

  /**
   * Marcar un pedido como CANCELADO
   */
  marcarCancelado(id: number | undefined): void {
    if (!id) return;

    if (confirm('¿Seguro que deseas CANCELAR este pedido?')) {
      this.isLoading = true; 
      this.pedidoService.marcarCancelado(id).subscribe({
        next: () => {
          mostrarToast('Pedido cancelado.', 'warning');
          this.cargarPedidos(); // Recargar la lista
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error al cancelar pedido:', err);
          this.handleError(err, 'cancelar');
          this.isLoading = false;
        }
      });
    }
  }

  // --- Métodos de Paginación ---
  
  irAPagina(pageNumber: number): void {
    if (pageNumber >= 0 && (!this.pedidosPage || pageNumber < this.pedidosPage.totalPages)) {
      this.currentPage = pageNumber;
      this.cargarPedidos();
    }
  }

  get totalPaginas(): number {
    return this.pedidosPage ? this.pedidosPage.totalPages : 0;
  }
  
  private handleError(err: HttpErrorResponse, context: string) {
    if (err.status === 403) {
      this.errorMessage = 'Acción no permitida: No tiene permisos.';
    } else if (err.status === 500) {
      this.errorMessage = 'Ocurrió un error interno en el servidor.';
    } else {
      this.errorMessage = err.error?.message || `Error al ${context} el pedido.`;
    }
    if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
  }
}
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; // Para *ngFor, *ngIf, etc.
import { RouterModule } from '@angular/router'; // Para [routerLink]
import { PedidoService } from '../../service/pedido.service';
import { PedidoDTO } from '../../models/pedido.model';
import { Page } from '../../models/page.model';
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-pedidos-list',
  standalone: true,
  imports: [CommonModule, RouterModule], // No necesitamos formularios aquí
  templateUrl: './pedidos-list.html',
  styleUrls: ['./pedidos-list.css']
})
export default class PedidosListComponent implements OnInit {

  // Inyección de dependencias
  private pedidoService = inject(PedidoService);

  // Estado del componente
  public pedidosPage: Page<PedidoDTO> | null = null;
  public currentPage = 0;
  public pageSize = 10;
  public errorMessage: string | null = null;
  public isLoading = false;

  constructor() {}

  ngOnInit(): void {
    this.cargarPedidos(); // Carga inicial
  }

  /**
   * Llama al servicio para cargar los pedidos paginados
   */
  cargarPedidos(): void {
    this.isLoading = true;
    this.errorMessage = null;

    // TODO: El backend necesita un endpoint /filtrar para pedidos.
    // Por ahora, llamamos al GET /api/pedidos paginado que asumimos existe.
    this.pedidoService.listarPedidos(this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.pedidosPage = page;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error al cargar pedidos:', err);
        this.errorMessage = 'Error al cargar los pedidos. Revise el backend.';
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
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
        error: (err: any) => {
          console.error('Error al completar pedido:', err);
          this.errorMessage = err.error?.message || 'Error al completar el pedido.';
          if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
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
        error: (err: any) => {
          console.error('Error al cancelar pedido:', err);
          this.errorMessage = err.error?.message || 'Error al cancelar el pedido.';
          if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
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
}
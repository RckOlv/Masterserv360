import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { RouterModule } from '@angular/router'; 
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms'; // <--- IMPORTANTE: AGREGAR ESTO

import { PedidoService } from '../../service/pedido.service';
import { PedidoDTO } from '../../models/pedido.model';
import { PedidoDetallado } from '../../models/pedido-detallado.model'; 
import { Page } from '../../models/page.model';
import { mostrarToast } from '../../utils/toast';
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 
import { ProveedorService } from '../../service/proveedor.service'; // Necesario para el dropdown
import { UsuarioService } from '../../service/usuario.service'; // Opcional, si quieres filtrar por usuario

@Component({
  selector: 'app-pedidos-list',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule,
    HasPermissionDirective,
    FormsModule // <--- IMPORTAR AQUÍ
  ], 
  templateUrl: './pedidos-list.html',
  styleUrls: ['./pedidos-list.css'] 
})
export default class PedidosListComponent implements OnInit {

  private pedidoService = inject(PedidoService);
  private proveedorService = inject(ProveedorService); // Inyectamos para cargar combo

  public pedidosPage: Page<PedidoDTO> | null = null;
  public currentPage = 0;
  public pageSize = 10;
  public errorMessage: string | null = null;
  public isLoading = false;
  
  public pedidoSeleccionado: PedidoDetallado | null = null; 

  // --- FILTROS ---
  public mostrarFiltros = false;
  public filtro: any = {
      proveedorId: null,
      estado: '', // Enviamos string vacío si es "Todos"
      fechaDesde: null,
      fechaHasta: null
  };
  public proveedores: any[] = []; // Para llenar el <select>
  // ---------------

  constructor() {}

  ngOnInit(): void {
    this.cargarPedidos(); 
    this.cargarProveedores(); // Cargar lista para el filtro
  }

  cargarProveedores() {
      // Asumiendo que tienes un método para listar todos o paginado
      this.proveedorService.listarProveedores().subscribe({
          next: (data) => this.proveedores = data,
          error: () => console.error('Error cargando proveedores para filtro')
      });
  }

  cargarPedidos(): void {
    this.isLoading = true;
    this.errorMessage = null;

    // Usamos el método de filtrado siempre. Si el filtro está vacío, el backend devuelve todo.
    this.pedidoService.filtrarPedidos(this.filtro, this.currentPage, this.pageSize).subscribe({
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

  buscar(): void {
      this.currentPage = 0; // Volver a página 1
      this.cargarPedidos();
  }

  limpiarFiltros(): void {
      this.filtro = {
          proveedorId: null,
          estado: '',
          fechaDesde: null,
          fechaHasta: null
      };
      this.buscar();
  }

  toggleFiltros(): void {
      this.mostrarFiltros = !this.mostrarFiltros;
  }

  verDetalles(id: number | undefined): void {
    if (!id) return;
    this.pedidoService.obtenerDetalles(id).subscribe({
      next: (data) => this.pedidoSeleccionado = data,
      error: (err) => {
        console.error('Error al cargar detalles:', err);
        mostrarToast('No se pudieron cargar los detalles.', 'danger');
      }
    });
  }

  cerrarModal(): void {
    this.pedidoSeleccionado = null;
  }

  descargarPdf(id: number | undefined): void {
    if (!id) return;
    this.pedidoService.descargarPdf(id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url);
      },
      error: (err) => {
        console.error('Error al descargar PDF:', err);
        mostrarToast('Error al generar el PDF.', 'danger');
      }
    });
  }

  marcarCompletado(id: number | undefined): void {
    if (!id) return;
    if (confirm('¿Seguro que deseas marcar este pedido como COMPLETADO? Esta acción ingresará el stock de los productos al inventario.')) {
      this.isLoading = true; 
      this.pedidoService.marcarCompletado(id).subscribe({
        next: () => {
          mostrarToast('Pedido completado. Stock actualizado.', 'success');
          this.cargarPedidos(); 
          if (this.pedidoSeleccionado?.id === id) this.cerrarModal(); 
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error al completar pedido:', err);
          this.handleError(err, 'completar');
          this.isLoading = false;
        }
      });
    }
  }

  marcarCancelado(id: number | undefined): void {
    if (!id) return;
    if (confirm('¿Seguro que deseas CANCELAR este pedido?')) {
      this.isLoading = true; 
      this.pedidoService.marcarCancelado(id).subscribe({
        next: () => {
          mostrarToast('Pedido cancelado.', 'warning');
          this.cargarPedidos(); 
          if (this.pedidoSeleccionado?.id === id) this.cerrarModal();
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error al cancelar pedido:', err);
          this.handleError(err, 'cancelar');
          this.isLoading = false;
        }
      });
    }
  }

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
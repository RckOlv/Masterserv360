import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { RouterModule } from '@angular/router'; 
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms'; 

import { PedidoService } from '../../service/pedido.service';
import { PedidoDTO } from '../../models/pedido.model';
import { PedidoDetallado } from '../../models/pedido-detallado.model'; 
import { Page } from '../../models/page.model';
import { mostrarToast } from '../../utils/toast';
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 
import { ProveedorService } from '../../service/proveedor.service'; 
import Swal from 'sweetalert2'; 

@Component({
  selector: 'app-pedidos-list',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule,
    HasPermissionDirective,
    FormsModule,
    ReactiveFormsModule 
  ], 
  templateUrl: './pedidos-list.html',
  styleUrls: ['./pedidos-list.css'] 
})
export default class PedidosListComponent implements OnInit {

  private pedidoService = inject(PedidoService);
  private proveedorService = inject(ProveedorService); 

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
      estado: null, 
      fechaDesde: null,
      fechaHasta: null
  };
  public proveedores: any[] = []; 
  // ---------------

  constructor() {}

  ngOnInit(): void {
    this.cargarPedidos(); 
    this.cargarProveedores(); 
  }

  cargarProveedores() {
      this.proveedorService.listarProveedores().subscribe({
          next: (data: any) => this.proveedores = data,
          error: () => console.error('Error cargando proveedores para filtro')
      });
  }

  cargarPedidos(): void {
    this.isLoading = true;
    this.errorMessage = null;

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
      this.currentPage = 0; 
      this.cargarPedidos();
  }

  limpiarFiltros(): void {
      this.filtro = {
          proveedorId: null,
          estado: null, 
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

  // --- 🔥 MÉTODO COMPLETAR (CORREGIDO EL COLOR DE TEXTO) ---
  marcarCompletado(id: number | undefined): void {
    if (!id) return;

    Swal.fire({
      title: '¿Confirmar Recepción?',
      html: `
        <p>Vas a marcar el Pedido <strong>#${id}</strong> como <span class="text-success fw-bold">COMPLETADO</span>.</p>
        <p class="small text-white-50 mt-2"><i class="bi bi-info-circle"></i> El stock de los productos se sumará automáticamente al inventario.</p>
      `,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#198754', // Verde
      cancelButtonColor: '#6c757d',  // Gris
      confirmButtonText: '<i class="bi bi-box-seam me-1"></i> Sí, ingresar stock',
      cancelButtonText: 'Cancelar',
      background: '#1e1e1e', // Fondo Oscuro
      color: '#ffffff',      // Texto Blanco
      reverseButtons: true
    }).then((result) => {
      if (result.isConfirmed) {
        this.procesarCompletado(id);
      }
    });
  }

  private procesarCompletado(id: number) {
    this.isLoading = true; 
    this.pedidoService.marcarCompletado(id).subscribe({
      next: () => {
        Swal.fire({
          title: '¡Stock Actualizado!',
          text: 'El pedido ha sido completado y el inventario actualizado.',
          icon: 'success',
          background: '#1e1e1e',
          color: '#ffffff',
          timer: 3000,
          showConfirmButton: false
        });
        
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

  // --- MÉTODO CANCELAR ---
  marcarCancelado(id: number | undefined): void {
    if (!id) return;

    Swal.fire({
      title: '¿Cancelar Pedido?',
      text: `El pedido #${id} será cancelado definitivamente.`,
      icon: 'error', 
      showCancelButton: true,
      confirmButtonColor: '#dc3545', // Rojo
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, cancelar pedido',
      cancelButtonText: 'No, volver',
      background: '#1e1e1e',
      color: '#ffffff',
      reverseButtons: true
    }).then((result) => {
      if (result.isConfirmed) {
        this.procesarCancelacion(id);
      }
    });
  }

  private procesarCancelacion(id: number) {
    this.isLoading = true; 
    this.pedidoService.marcarCancelado(id).subscribe({
      next: () => {
        mostrarToast('Pedido cancelado correctamente.', 'warning');
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
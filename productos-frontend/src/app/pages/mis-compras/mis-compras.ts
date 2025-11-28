import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

// --- MODELOS ---
import { Page } from '../../models/page.model';
import { VentaDTO } from '../../models/venta.model'; // VentaDTO sigue aquí
// CORRECCIÓN: Importar VentaResumenDTO desde su propio archivo
import { VentaResumenDTO } from '../../models/venta-resumen.model'; 

import { ClienteService } from '../../service/cliente.service';
import { VentaService } from '../../service/venta.service';
import { mostrarToast } from '../../utils/toast';
import { PageEvent, MatPaginatorModule } from '@angular/material/paginator';

// Declarar Bootstrap para el modal
declare var bootstrap: any;

@Component({
  selector: 'app-mis-compras',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatPaginatorModule
  ],
  templateUrl: './mis-compras.html',
  styleUrl: './mis-compras.css'
})
export default class MisComprasComponent implements OnInit {

  private clienteService = inject(ClienteService);
  private ventaService = inject(VentaService);

  // Datos Lista
  public page?: Page<VentaResumenDTO>;
  public isLoading = true;
  
  // Datos Detalle (Modal)
  public compraSeleccionada: VentaDTO | null = null;
  public isLoadingDetalle = false;
  private modalDetalleInstance: any;

  constructor() { }

  ngOnInit(): void {
    this.loadCompras(0, 6); 
  }

  loadCompras(page: number, size: number): void {
    this.isLoading = true;
    this.clienteService.getMisCompras(page, size).subscribe({
      next: (data) => {
        this.page = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        mostrarToast('Error al cargar el historial de compras', 'danger');
        this.isLoading = false;
      }
    });
  }

  handlePageEvent(e: PageEvent): void {
    this.loadCompras(e.pageIndex, e.pageSize);
  }

  paginaAnterior(): void {
    if (this.page && !this.page.first) {
      this.loadCompras(this.page.number - 1, this.page.size);
    }
  }

  paginaSiguiente(): void {
    if (this.page && !this.page.last) {
      this.loadCompras(this.page.number + 1, this.page.size);
    }
  }

  // --- MÉTODOS DE DETALLE Y PDF ---

  verDetalle(idVenta: number): void {
    this.isLoadingDetalle = true;
    this.compraSeleccionada = null;
    
    const modalEl = document.getElementById('modalDetalleCompra');
    if (modalEl) {
      this.modalDetalleInstance = new bootstrap.Modal(modalEl);
      this.modalDetalleInstance.show();
    }

    this.ventaService.getVentaById(idVenta).subscribe({
      next: (venta) => {
        this.compraSeleccionada = venta;
        this.isLoadingDetalle = false;
      },
      error: () => {
        mostrarToast('Error al cargar el detalle.', 'danger');
        this.isLoadingDetalle = false;
        if(this.modalDetalleInstance) this.modalDetalleInstance.hide();
      }
    });
  }

  descargarComprobante(idVenta: number, event: Event): void {
    event.stopPropagation(); 
    mostrarToast('Generando comprobante...', 'info');

    this.ventaService.getComprobantePdf(idVenta).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `Comprobante-Venta-${idVenta}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        mostrarToast('Comprobante descargado.', 'success');
      },
      error: () => mostrarToast('No se pudo descargar el comprobante.', 'danger')
    });
  }
}
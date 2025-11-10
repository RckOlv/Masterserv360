import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; // ¡Importante!
import { RouterModule } from '@angular/router'; // Para los enlaces de "Ver Detalle"
import { Page } from '../../models/page.model';
import { VentaResumenDTO } from '../../models/venta-resumen.model';
import { ClienteService } from '../../service/cliente.service';
import { mostrarToast } from '../../utils/toast';
import { PageEvent, MatPaginatorModule } from '@angular/material/paginator'; // ¡Paginador!

@Component({
  selector: 'app-mis-compras',
  standalone: true, // ¡Asegúrate de que sea standalone!
  imports: [
    CommonModule,
    RouterModule, // Añadir RouterModule
    MatPaginatorModule // Añadir Paginador
  ],
  templateUrl: './mis-compras.html',
  styleUrl: './mis-compras.css'
})
export default class MisComprasComponent implements OnInit {

  // --- Inyección de Dependencias ---
  private clienteService = inject(ClienteService);

  // --- Estado del Componente ---
  public page?: Page<VentaResumenDTO>;
  public isLoading = true;
  // Columnas para la tabla (opcional, pero buena práctica)
  public displayedColumns: string[] = ['id', 'fechaVenta', 'estado', 'totalVenta', 'cantidadItems', 'codigoCuponUsado', 'acciones'];

  constructor() { }

  ngOnInit(): void {
    this.loadCompras(0, 10); // Cargar la primera página (10 items por defecto)
  }

  /**
   * Carga el historial de compras paginado
   */
  loadCompras(page: number, size: number): void {
    this.isLoading = true;
    this.clienteService.getMisCompras(page, size).subscribe({
      next: (data) => {
        this.page = data;
        this.isLoading = false;
      },
      error: (err) => {
        mostrarToast('Error al cargar el historial de compras', 'danger');
        this.isLoading = false;
      }
    });
  }

  /**
   * Maneja el evento de cambio de página desde el paginador
   */
  handlePageEvent(e: PageEvent): void {
    this.loadCompras(e.pageIndex, e.pageSize);
  }
}
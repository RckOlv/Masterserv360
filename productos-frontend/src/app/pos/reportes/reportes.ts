import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService, ValorizacionDTO, StockInmovilizadoDTO, VariacionCostoDTO } from '../../service/reporte.service';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.html',
  styles: [`
    .nav-tabs .nav-link.active {
      background-color: #0d6efd;
      color: white;
      border-color: #0d6efd;
    }
    .nav-tabs .nav-link {
      color: #495057;
      cursor: pointer;
    }
    .total-card {
      background: linear-gradient(45deg, #198754, #20c997);
      color: white;
    }
    .alert-card {
      background: linear-gradient(45deg, #dc3545, #fd7e14);
      color: white;
    }
  `]
})
export class ReportesComponent implements OnInit {
  private reporteService = inject(ReporteService);

  // Estado de Pestañas
  activeTab: 'valorizacion' | 'inmovilizado' | 'costos' = 'valorizacion';

  // Datos
  valorizacion: ValorizacionDTO[] = [];
  inmovilizado: StockInmovilizadoDTO[] = [];
  historialCostos: VariacionCostoDTO[] = [];

  // Filtros y Totales
  diasInmovilizado: number = 90;
  productoIdBusqueda: number | null = null;
  
  totalInventario: number = 0;
  totalCapitalParado: number = 0;
  loading = false;

  ngOnInit() {
    this.cargarValorizacion(); // Carga inicial
  }

  cambiarTab(tab: 'valorizacion' | 'inmovilizado' | 'costos') {
    this.activeTab = tab;
    if (tab === 'valorizacion' && this.valorizacion.length === 0) this.cargarValorizacion();
    if (tab === 'inmovilizado' && this.inmovilizado.length === 0) this.cargarInmovilizado();
  }

  // --- LOGICA VALORIZACIÓN ---
  cargarValorizacion() {
    this.loading = true;
    this.reporteService.getValorizacion().subscribe({
      next: (data) => {
        this.valorizacion = data;
        // Calculamos el gran total sumando todas las categorías
        this.totalInventario = data.reduce((acc, curr) => acc + curr.valorTotal, 0);
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  // --- LOGICA INMOVILIZADO ---
  cargarInmovilizado() {
    this.loading = true;
    this.reporteService.getStockInmovilizado(this.diasInmovilizado).subscribe({
      next: (data) => {
        this.inmovilizado = data;
        this.totalCapitalParado = data.reduce((acc, curr) => acc + curr.capitalParado, 0);
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  // --- LOGICA HISTORIAL COSTOS ---
  buscarHistorial() {
    if (!this.productoIdBusqueda) return;
    this.loading = true;
    this.reporteService.getHistorialCostos(this.productoIdBusqueda).subscribe({
      next: (data) => {
        this.historialCostos = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }
  
  // Función para imprimir reporte
  imprimir() {
    window.print();
  }
}
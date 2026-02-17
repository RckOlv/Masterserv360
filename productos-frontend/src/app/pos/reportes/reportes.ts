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
    /* Estilos para modo oscuro */
    .nav-tabs .nav-link.active {
      background-color: #0d6efd;
      color: white;
      border-color: #0d6efd;
    }
    .nav-tabs .nav-link {
      color: #adb5bd; /* Gris claro para inactivos */
      cursor: pointer;
    }
    .nav-tabs .nav-link:hover {
      color: #fff;
      border-color: #495057;
    }
    .total-card {
      background: linear-gradient(45deg, #198754, #20c997);
      color: white;
    }
    /* Input oscuro custom */
    .form-control-dark {
      background-color: #212529;
      border-color: #495057;
      color: #fff;
    }
    .form-control-dark:focus {
      background-color: #212529;
      border-color: #0d6efd;
      color: #fff;
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
  
  totalInventario: number = 0;
  totalCapitalParado: number = 0;
  loading = false;

  ngOnInit() {
    this.cargarValorizacion(); // Carga inicial
  }

  cambiarTab(tab: 'valorizacion' | 'inmovilizado' | 'costos') {
    this.activeTab = tab;
    // Carga perezosa (solo si no hay datos)
    if (tab === 'valorizacion' && this.valorizacion.length === 0) this.cargarValorizacion();
    if (tab === 'inmovilizado' && this.inmovilizado.length === 0) this.cargarInmovilizado();
    // 🔥 Carga automática de costos generales al entrar
    if (tab === 'costos' && this.historialCostos.length === 0) this.cargarCostosGenerales();
  }

  // --- LOGICA VALORIZACIÓN ---
  cargarValorizacion() {
    this.loading = true;
    this.reporteService.getValorizacion().subscribe({
      next: (data) => {
        this.valorizacion = data;
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

  // --- LOGICA HISTORIAL COSTOS (NUEVA: CARGA GENERAL) ---
  cargarCostosGenerales() {
    this.loading = true;
    this.reporteService.getUltimosCostosGenerales().subscribe({
      next: (data) => {
        this.historialCostos = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }
  
  imprimir() {
    window.print();
  }
}
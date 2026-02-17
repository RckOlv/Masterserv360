import { Component, OnInit, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService, ValorizacionDTO, StockInmovilizadoDTO, VariacionCostoDTO } from '../../service/reporte.service';

// 📄 Librerías para PDF Profesional
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

// 📊 Librerías para Gráficos
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule, BaseChartDirective], // ✅ Importante: BaseChartDirective
  templateUrl: './reportes.html',
  styles: [`
    .nav-tabs .nav-link.active { background-color: #0d6efd; color: white; border-color: #0d6efd; }
    .nav-tabs .nav-link { color: #adb5bd; cursor: pointer; }
    .nav-tabs .nav-link:hover { color: #fff; border-color: #495057; }
    .total-card { background: linear-gradient(45deg, #198754, #20c997); color: white; }
    .form-control-dark { background-color: #212529; border-color: #495057; color: #fff; }
    .form-control-dark:focus { background-color: #212529; border-color: #0d6efd; color: #fff; }
    .text-bright { color: #ffffff !important; opacity: 1 !important; }
    .text-dim { color: rgba(255,255,255,0.7) !important; }
    .chart-container { position: relative; height: 300px; width: 100%; }
  `]
})
export class ReportesComponent implements OnInit {
  private reporteService = inject(ReporteService);

  activeTab: 'valorizacion' | 'inmovilizado' | 'costos' = 'valorizacion';
  loading = false;

  // Datos
  valorizacion: ValorizacionDTO[] = [];
  inmovilizado: StockInmovilizadoDTO[] = [];
  historialCostos: VariacionCostoDTO[] = [];

  // Totales
  totalInventario: number = 0;
  totalCapitalParado: number = 0;

  // Filtros
  diasInmovilizado: number = 90;
  busquedaProducto: string = ''; 

  // 📊 Referencia al Gráfico para el PDF
  @ViewChild('myChart', { static: false }) private chartRef?: ElementRef;

  // 📈 CONFIGURACIÓN DEL GRÁFICO (Chart.js)
  public lineChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Costo Unitario ($)',
        fill: true,
        tension: 0.4,
        borderColor: '#0d6efd',
        backgroundColor: 'rgba(13, 110, 253, 0.2)',
        pointBackgroundColor: '#fff',
        pointBorderColor: '#0d6efd',
        pointHoverBackgroundColor: '#0d6efd',
        pointHoverBorderColor: '#fff'
      }
    ]
  };

  public lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { labels: { color: 'white' } },
      tooltip: { 
        backgroundColor: 'rgba(0,0,0,0.8)',
        titleColor: '#fff',
        bodyColor: '#fff'
      }
    },
    scales: {
      x: { ticks: { color: '#adb5bd' }, grid: { color: 'rgba(255,255,255,0.1)' } },
      y: { ticks: { color: '#adb5bd' }, grid: { color: 'rgba(255,255,255,0.1)' } }
    }
  };
  
  ngOnInit() {
    this.cargarValorizacion();
  }

  cambiarTab(tab: any) {
    this.activeTab = tab;
    if (tab === 'valorizacion' && this.valorizacion.length === 0) this.cargarValorizacion();
    if (tab === 'inmovilizado' && this.inmovilizado.length === 0) this.cargarInmovilizado();
    if (tab === 'costos' && this.historialCostos.length === 0) this.cargarCostos();
  }

  // --- CARGAS ---

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

  cargarCostos() {
    this.loading = true;
    
    const request = (this.busquedaProducto && this.busquedaProducto.trim() !== '')
      ? this.reporteService.buscarHistorialPorNombre(this.busquedaProducto)
      : this.reporteService.getUltimosCostosGenerales();

    request.subscribe({
      next: (data) => { 
        this.historialCostos = data;
        this.actualizarGrafico(); // 🔄 Actualizamos el gráfico con los nuevos datos
        this.loading = false; 
      },
      error: () => this.loading = false
    });
  }

  // 📊 Lógica para actualizar el gráfico
  actualizarGrafico() {
    if (this.historialCostos.length === 0) return;

    // Invertimos para que sea cronológico (Viejo -> Nuevo) en el gráfico
    const datosOrdenados = [...this.historialCostos].reverse();

    const etiquetas = datosOrdenados.map(item => {
      const date = new Date(item.fechaCompra);
      return date.toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit' });
    });

    const precios = datosOrdenados.map(item => item.costoPagado);

    this.lineChartData = {
      labels: etiquetas,
      datasets: [
        {
          data: precios,
          label: this.busquedaProducto ? `Evolución: ${this.busquedaProducto}` : 'Tendencia General',
          fill: true,
          tension: 0.4,
          borderColor: '#0d6efd',
          backgroundColor: 'rgba(13, 110, 253, 0.2)',
          pointBackgroundColor: '#fff',
          pointBorderColor: '#0d6efd'
        }
      ]
    };
  }

  // --- 🖨️ GENERACIÓN DE PDF PROFESIONAL ---
  
  exportarPDF() {
    const doc = new jsPDF();
    const fecha = new Date().toLocaleDateString();

    if (this.activeTab === 'valorizacion') {
      doc.text('Reporte de Valorización de Inventario', 14, 20);
      doc.setFontSize(10);
      doc.text(`Generado el: ${fecha}`, 14, 28);
      doc.text(`Valor Total: $${this.totalInventario.toLocaleString('es-AR')}`, 14, 34);
      
      autoTable(doc, {
        startY: 40,
        head: [['Categoría', 'Unidades', 'Valor Total ($)', '% Total']],
        body: this.valorizacion.map(item => [
          item.categoria,
          item.cantidadUnidades,
          `$ ${item.valorTotal.toLocaleString('es-AR')}`,
          ((item.valorTotal / this.totalInventario) * 100).toFixed(1) + ' %'
        ]),
      });
      doc.save('valorizacion_inventario.pdf');
    }

    if (this.activeTab === 'inmovilizado') {
      doc.text('Reporte de Stock Inmovilizado', 14, 20);
      doc.setFontSize(10);
      doc.text(`Criterio: Sin movimientos hace más de ${this.diasInmovilizado} días`, 14, 28);
      doc.text(`Capital Parado: $${this.totalCapitalParado.toLocaleString('es-AR')}`, 14, 34);

      autoTable(doc, {
        startY: 40,
        head: [['Producto', 'Categoría', 'Stock', 'Costo Unit.', 'Capital Parado', 'Días Quieto']],
        body: this.inmovilizado.map(item => [
          item.nombre,
          item.categoria,
          item.stockActual,
          `$ ${item.costoUnitario.toLocaleString()}`,
          `$ ${item.capitalParado.toLocaleString()}`,
          item.diasSinVenta
        ]),
      });
      doc.save('stock_inmovilizado.pdf');
    }

    if (this.activeTab === 'costos') {
      const titulo = this.busquedaProducto 
        ? `Historial de Costos - Búsqueda: "${this.busquedaProducto}"` 
        : 'Últimas Compras Generales (Feed de Precios)';
        
      doc.text(titulo, 14, 20);
      doc.text(`Fecha impresión: ${fecha}`, 14, 28);

      let startY = 35;

      // 🖼️ SI HAY GRÁFICO, LO PEGAMOS AL PDF
      if (this.chartRef && this.historialCostos.length > 1) {
        try {
            const canvas = this.chartRef.nativeElement as HTMLCanvasElement;
            const imagenGrafico = canvas.toDataURL('image/png', 1.0);
            doc.addImage(imagenGrafico, 'PNG', 15, 35, 180, 80); // x, y, ancho, alto
            startY = 120; // Bajamos la tabla para que no tape el gráfico
        } catch (e) {
            console.error("No se pudo exportar el gráfico al PDF", e);
        }
      }

      autoTable(doc, {
        startY: startY,
        head: [['Fecha', 'Producto', 'Proveedor', 'Orden #', 'Costo Pagado']],
        body: this.historialCostos.map(item => [
          new Date(item.fechaCompra).toLocaleDateString(),
          item.producto,
          item.proveedor,
          item.nroOrden,
          `$ ${item.costoPagado.toLocaleString('es-AR')}`
        ]),
      });
      doc.save('historial_costos.pdf');
    }
  }
}
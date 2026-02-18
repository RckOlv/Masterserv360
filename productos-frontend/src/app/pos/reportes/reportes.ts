import { Component, OnInit, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chart, registerables } from 'chart.js';
import { ReporteService, ValorizacionDTO, StockInmovilizadoDTO, VariacionCostoDTO } from '../../service/reporte.service';

// 📄 Librerías para PDF Profesional
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

// 📊 Librerías para Gráficos
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';

// ✅ Interfaz para la agrupación del acordeón
interface CostoAgrupado {
  producto: string;
  ultimoPrecio: number;
  historial: VariacionCostoDTO[];
}

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule, BaseChartDirective],
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
    /* Hover para el acordeón */
    .fila-agrupada:hover { background-color: rgba(255,255,255,0.05) !important; transition: 0.2s; }
  `]
})
export class ReportesComponent implements OnInit {
  private reporteService = inject(ReporteService);

  activeTab: 'valorizacion' | 'inmovilizado' | 'costos' = 'valorizacion';
  loading = false;
  usuarioActual: string = 'Administrador'; // 👤 Para firmar el PDF

  constructor() {
    Chart.register(...registerables);
  }

  // Datos
  valorizacion: ValorizacionDTO[] = [];
  inmovilizado: StockInmovilizadoDTO[] = [];
  historialCostos: VariacionCostoDTO[] = [];
  
  // Costos Agrupados (Acordeón)
  costosAgrupados: CostoAgrupado[] = [];
  productoExpandido: string | null = null;

  // Buscador Custom
  listaProductos: any[] = []; 
  productosFiltrados: any[] = [];
  busquedaInput: string = ''; // Lo que se tipea
  busquedaProducto: string = ''; // El producto final seleccionado
  mostrarDropdown: boolean = false;

  // Totales
  totalInventario: number = 0;
  totalCapitalParado: number = 0;
  diasInmovilizado: number = 90;

  // 📊 Referencia al Gráfico para el PDF
  @ViewChild('myChart', { static: false }) private chartRef?: ElementRef;

  public lineChartData: ChartConfiguration<'line'>['data'] = {
    labels: [], datasets: []
  };

  public lineChartOptions: ChartOptions<'line'> = {
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { labels: { color: 'white' } }, tooltip: { backgroundColor: 'rgba(0,0,0,0.8)', titleColor: '#fff', bodyColor: '#fff' } },
    scales: { x: { ticks: { color: '#adb5bd' }, grid: { color: 'rgba(255,255,255,0.1)' } }, y: { ticks: { color: '#adb5bd' }, grid: { color: 'rgba(255,255,255,0.1)' } } }
  };
  
  ngOnInit() {
    this.obtenerUsuarioActual();
    this.cargarValorizacion();
    this.reporteService.getProductosParaFiltro().subscribe({
        next: (data) => {
            this.listaProductos = data;
            this.productosFiltrados = data;
        },
        error: (err) => console.error("No se pudieron cargar los productos", err)
    });
  }

  obtenerUsuarioActual() {
    const user = localStorage.getItem('username') || localStorage.getItem('email');
    if (user) this.usuarioActual = user;
  }

  cambiarTab(tab: any) {
    this.activeTab = tab;
    if (tab === 'valorizacion' && this.valorizacion.length === 0) this.cargarValorizacion();
    if (tab === 'inmovilizado' && this.inmovilizado.length === 0) this.cargarInmovilizado();
    if (tab === 'costos' && this.historialCostos.length === 0) this.cargarCostos();
  }

  // --- LÓGICA DEL BUSCADOR ---

  filtrarProductos() {
    if (!this.busquedaInput) {
        this.productosFiltrados = this.listaProductos;
    } else {
        // Normalizamos para ignorar acentos y mayúsculas
        const texto = this.busquedaInput.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
        this.productosFiltrados = this.listaProductos.filter(p => 
            p.nombre.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").includes(texto)
        );
    }
    this.mostrarDropdown = true;
  }

  seleccionarProducto(prod: any) {
    this.busquedaProducto = prod.nombre;
    this.busquedaInput = prod.nombre;
    this.mostrarDropdown = false;
    this.productoExpandido = prod.nombre; // Lo abrimos por defecto
    this.cargarCostos();
  }

  cerrarDropdown() {
    setTimeout(() => this.mostrarDropdown = false, 200);
  }

  limpiarBusqueda() {
    this.busquedaProducto = '';
    this.busquedaInput = '';
    this.productoExpandido = null;
    this.cargarCostos();
  }

  // --- CARGAS ---

  cargarValorizacion() {
    this.loading = true;
    this.reporteService.getValorizacion().subscribe({
        next: (data) => { this.valorizacion = data; this.totalInventario = data.reduce((acc, curr) => acc + curr.valorTotal, 0); this.loading = false; },
        error: () => this.loading = false
    });
  }

  cargarInmovilizado() {
    this.loading = true;
    this.reporteService.getStockInmovilizado(this.diasInmovilizado).subscribe({
        next: (data) => { this.inmovilizado = data; this.totalCapitalParado = data.reduce((acc, curr) => acc + curr.capitalParado, 0); this.loading = false; },
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
        this.agruparCostos(); // 🔄 Agrupamos para el acordeón
        this.actualizarGrafico(); 
        this.loading = false; 
      },
      error: () => this.loading = false
    });
  }

  agruparCostos() {
    const mapa = new Map<string, VariacionCostoDTO[]>();
    this.historialCostos.forEach(item => {
        if (!mapa.has(item.producto)) mapa.set(item.producto, []);
        mapa.get(item.producto)!.push(item);
    });

    this.costosAgrupados = Array.from(mapa.entries()).map(([producto, historial]) => {
        historial.sort((a, b) => new Date(b.fechaCompra).getTime() - new Date(a.fechaCompra).getTime());
        return { producto, historial, ultimoPrecio: historial[0].costoPagado };
    });
  }

  toggleExpandir(producto: string) {
    this.productoExpandido = this.productoExpandido === producto ? null : producto;
  }

  actualizarGrafico() {
    if (this.historialCostos.length === 0) return;
    const datosOrdenados = [...this.historialCostos].reverse();
    const etiquetas = datosOrdenados.map(item => new Date(item.fechaCompra).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit' }));
    const precios = datosOrdenados.map(item => item.costoPagado);

    this.lineChartData = {
      labels: etiquetas,
      datasets: [{
          data: precios,
          label: `Evolución de Costo: ${this.busquedaProducto}`,
          fill: true, tension: 0.4, borderColor: '#0d6efd', backgroundColor: 'rgba(13, 110, 253, 0.2)', pointBackgroundColor: '#fff', pointBorderColor: '#0d6efd'
      }]
    };
  }

  // --- 🖨️ GENERACIÓN DE PDF PROFESIONAL ---
  
  exportarPDF() {
    const doc = new jsPDF();
    const fecha = new Date().toLocaleDateString();
    const auditoria = `Generado por: ${this.usuarioActual} | Fecha: ${fecha}`;

    if (this.activeTab === 'valorizacion') {
      doc.text('Reporte de Valorización de Inventario', 14, 20);
      doc.setFontSize(10);
      doc.text(auditoria, 14, 28);
      doc.text(`Valor Total: $${this.totalInventario.toLocaleString('es-AR')}`, 14, 34);
      
      autoTable(doc, {
        startY: 40,
        head: [['Categoría', 'Unidades', 'Valor Total ($)', '% Total']],
        body: this.valorizacion.map(item => [ item.categoria, item.cantidadUnidades, `$ ${item.valorTotal.toLocaleString('es-AR')}`, ((item.valorTotal / this.totalInventario) * 100).toFixed(1) + ' %' ]),
      });
      doc.save('valorizacion_inventario.pdf');
    }

    if (this.activeTab === 'inmovilizado') {
      doc.text('Reporte de Stock Inmovilizado', 14, 20);
      doc.setFontSize(10);
      doc.text(auditoria, 14, 28);
      doc.text(`Criterio: Sin movimientos hace más de ${this.diasInmovilizado} días`, 14, 34);

      autoTable(doc, {
        startY: 40,
        head: [['Producto', 'Categoría', 'Stock', 'Costo Unit.', 'Capital Parado', 'Días Quieto']],
        body: this.inmovilizado.map(item => [ item.nombre, item.categoria, item.stockActual, `$ ${item.costoUnitario.toLocaleString()}`, `$ ${item.capitalParado.toLocaleString()}`, item.diasSinVenta ]),
      });
      doc.save('stock_inmovilizado.pdf');
    }

    if (this.activeTab === 'costos') {
      const titulo = this.busquedaProducto ? `Historial de Costos - Búsqueda: "${this.busquedaProducto}"` : 'Últimas Compras Generales';
      doc.text(titulo, 14, 20);
      doc.setFontSize(10);
      doc.text(auditoria, 14, 28);

      let startY = 35;

      if (this.chartRef && this.busquedaProducto && this.historialCostos.length > 1) {
        try {
            const canvas = this.chartRef.nativeElement as HTMLCanvasElement;
            const tempCanvas = document.createElement('canvas');
            tempCanvas.width = canvas.width; tempCanvas.height = canvas.height;
            const ctx = tempCanvas.getContext('2d');
            if (ctx) { ctx.fillStyle = '#212529'; ctx.fillRect(0, 0, tempCanvas.width, tempCanvas.height); ctx.drawImage(canvas, 0, 0); }
            
            doc.addImage(tempCanvas.toDataURL('image/png', 1.0), 'PNG', 15, 35, 180, 80);
            startY = 120;
        } catch (e) { console.error("Error al exportar el gráfico", e); }
      }

      autoTable(doc, {
        startY: startY,
        head: [['Fecha', 'Producto', 'Proveedor', 'Orden #', 'Costo Pagado']],
        body: this.historialCostos.map(item => [ new Date(item.fechaCompra).toLocaleDateString(), item.producto, item.proveedor, item.nroOrden, `$ ${item.costoPagado.toLocaleString('es-AR')}` ]),
      });
      doc.save('historial_costos.pdf');
    }
  }
}
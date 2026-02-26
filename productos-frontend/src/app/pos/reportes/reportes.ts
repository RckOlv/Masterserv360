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
  styles: [`.reportes.css`] 
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
      next: (data: any) => {
          const productos = data.content ? data.content : data;
          this.listaProductos = productos;
          this.productosFiltrados = productos;
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
    // 🛡️ Agregá esta validación para que no explote si la lista no está lista
    if (!this.listaProductos || !Array.isArray(this.listaProductos)) {
        this.productosFiltrados = [];
        return;
    }

    // Acá sigue tu código normal, que seguro es algo así:
    const texto = this.busquedaInput.toLowerCase();
    this.productosFiltrados = this.listaProductos.filter(p => 
        p.nombre.toLowerCase().includes(texto)
    );
    this.mostrarDropdown = this.productosFiltrados.length > 0;
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
    
    // ✅ 1. Fecha, hora y usuario
    const fechaHora = new Date().toLocaleString('es-AR');
    const auditoria = `Generado por: ${this.usuarioActual} | Fecha y Hora: ${fechaHora}`;

    // --- LÓGICA DE CADA PESTAÑA ---
    if (this.activeTab === 'valorizacion') {
      doc.setFontSize(14); doc.setFont('helvetica', 'bold');
      doc.text('Reporte de Valorización de Inventario', 14, 20);
      doc.setFontSize(10); doc.setFont('helvetica', 'normal');
      doc.text(auditoria, 14, 28);
      doc.text(`Valor Total: $${this.totalInventario.toLocaleString('es-AR')}`, 14, 34);
      
      autoTable(doc, {
        startY: 40,
        head: [['Categoría', 'Unidades', 'Valor Total ($)', '% Total']],
        body: this.valorizacion.map(item => [ item.categoria, item.cantidadUnidades, `$ ${item.valorTotal.toLocaleString('es-AR')}`, ((item.valorTotal / this.totalInventario) * 100).toFixed(1) + ' %' ]),
      });
    }

    else if (this.activeTab === 'inmovilizado') {
      doc.setFontSize(14); doc.setFont('helvetica', 'bold');
      doc.text('Reporte de Stock Inmovilizado', 14, 20);
      doc.setFontSize(10); doc.setFont('helvetica', 'normal');
      doc.text(auditoria, 14, 28);
      doc.text(`Criterio: Sin movimientos hace más de ${this.diasInmovilizado} días`, 14, 34);

      autoTable(doc, {
        startY: 40,
        head: [['Producto', 'Categoría', 'Stock', 'Costo Unit.', 'Capital Parado', 'Días Quieto']],
        body: this.inmovilizado.map(item => [ item.nombre, item.categoria, item.stockActual, `$ ${item.costoUnitario.toLocaleString()}`, `$ ${item.capitalParado.toLocaleString()}`, item.diasSinVenta ]),
      });
    }

    else if (this.activeTab === 'costos') {
      doc.setFontSize(14); doc.setFont('helvetica', 'bold');
      const titulo = this.busquedaProducto ? `Historial de Costos: "${this.busquedaProducto}"` : 'Últimas Compras Generales';
      doc.text(titulo, 14, 20);
      
      doc.setFontSize(10); doc.setFont('helvetica', 'normal');
      doc.text(auditoria, 14, 28);

      let startY = 35;

      // Pegar gráfico si existe
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
        head: [['Fecha Compra', 'Producto', 'Proveedor', 'Orden #', 'Costo Pagado']],
        body: this.historialCostos.map(item => [ new Date(item.fechaCompra).toLocaleDateString('es-AR'), item.producto, item.proveedor, item.nroOrden, `$ ${item.costoPagado.toLocaleString('es-AR')}` ]),
      });
    }

    // ✅ 4. MAGIA DE PAGINACIÓN Y PIE DE PÁGINA (Con el fix ts)
    const pageCount = (doc as any).internal.getNumberOfPages();
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();

    for (let i = 1; i <= pageCount; i++) {
        doc.setPage(i);
        doc.setFontSize(9);
        doc.setTextColor(150);
        doc.line(14, pageHeight - 15, pageWidth - 14, pageHeight - 15);
        doc.text(`Sistema POS Masterserv - Auditoría`, 14, pageHeight - 10);
        doc.text(`Página ${i} de ${pageCount}`, pageWidth - 14, pageHeight - 10, { align: 'right' });
    }

    // ✅ 5. GUARDAR
    const nombreArchivo = `Reporte_${this.activeTab}_${fechaHora.replace(/[\/:, ]/g, '-')}.pdf`;
    doc.save(nombreArchivo);
  }
}
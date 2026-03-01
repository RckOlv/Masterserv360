import { Component, OnInit, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chart, registerables } from 'chart.js';
import { ReporteService, ValorizacionDTO, StockInmovilizadoDTO, VariacionCostoDTO } from '../../service/reporte.service';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';

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
  usuarioActual: string = 'Administrador'; 

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
    if (!this.listaProductos || !Array.isArray(this.listaProductos)) {
        this.productosFiltrados = [];
        this.mostrarDropdown = false;
        return;
    }

    const texto = this.busquedaInput.trim().toLowerCase();
    if (texto === '') {
        this.productosFiltrados = [];
        this.mostrarDropdown = false;
        return;
    }
    // Si hay texto, filtramos y abrimos el dropdown
    this.productosFiltrados = this.listaProductos.filter(p => 
        p.nombre.toLowerCase().includes(texto)
    );
    // Oculta el dropdown si no encontró nada para no mostrar un cuadro vacío
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

  // --- 🖨️ DESCARGA DE PDF PROFESIONAL DESDE EL BACKEND ---
  
  exportarPDF() {
    this.loading = true;
    let peticion;
    let nombreArchivo = '';

    if (this.activeTab === 'valorizacion') {
      peticion = this.reporteService.descargarPdfValorizacion();
      nombreArchivo = 'Reporte_Valorizacion.pdf';
    } 
    else if (this.activeTab === 'inmovilizado') {
      peticion = this.reporteService.descargarPdfInmovilizado(this.diasInmovilizado);
      nombreArchivo = 'Reporte_Stock_Inmovilizado.pdf';
    } 
    else if (this.activeTab === 'costos') {
      peticion = this.reporteService.descargarPdfCostos(this.busquedaProducto);
      nombreArchivo = 'Reporte_Evolucion_Costos.pdf';
    }

    if (peticion) {
      peticion.subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = nombreArchivo;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          a.remove();
          this.loading = false;
        },
        error: (err) => {
          console.error('Error al descargar el PDF:', err);
          this.loading = false;
          alert('Hubo un error al generar el PDF. Verifica la consola.');
        }
      });
    } else {
        this.loading = false;
    }
  }
}
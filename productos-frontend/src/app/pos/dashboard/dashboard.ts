import { Component, OnInit, inject, ViewChild } from '@angular/core'; // <--- ViewChild
import { CommonModule, CurrencyPipe, DecimalPipe, DatePipe } from '@angular/common'; 
import { HttpClient } from '@angular/common/http'; 
import { API_URL } from '../../app.config'; 
import { Router, RouterModule } from '@angular/router'; 
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { AuthService } from '../../service/auth.service'; 
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 
import { DashboardStatsDTO } from '../../models/dashboard-stats.model';
import { VentasPorDiaDTO, TopProductoDTO, VentaDTO } from '../../models/venta.model';
import { VentaService } from '../../service/venta.service';
import SalesChartComponent from './sales-chart/sales-chart'; 
import { CategoryChartComponent } from './category-chart/category-chart'; 
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    ReactiveFormsModule,
    CurrencyPipe, 
    DecimalPipe,
    DatePipe,
    HasPermissionDirective, 
    SalesChartComponent,
    CategoryChartComponent 
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export default class DashboardComponent implements OnInit {

  private http = inject(HttpClient);
  private router = inject(Router); 
  private apiUrlBase = API_URL; 
  private ventaService = inject(VentaService); 
  private authService = inject(AuthService); 
  private fb = inject(FormBuilder);

  public canManageUsers = false; 
  
  public stats: DashboardStatsDTO | null = null;
  public ventasSemanales: VentasPorDiaDTO[] = [];
  public topProductos: TopProductoDTO[] = [];
  public ultimasVentas: VentaDTO[] = [];
  public ventasPorCategoria: any[] = []; 
  
  public isLoadingStats = true;
  public isLoadingChart = true;
  public isLoadingTopProducts = true;
  public isLoadingRecentSales = true;
  public isLoadingCategories = true; 
  public isDownloading = false;

  public filtroForm: FormGroup;

  // MENTOR: Referencia al gráfico para pedirle la foto
  @ViewChild(SalesChartComponent) salesChart!: SalesChartComponent;

  constructor() {
    const hoy = new Date();
    const primerDia = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    const fechaInicio = new Date(primerDia.getTime() - (primerDia.getTimezoneOffset() * 60000)).toISOString().split('T')[0];
    const fechaFin = new Date(hoy.getTime() - (hoy.getTimezoneOffset() * 60000)).toISOString().split('T')[0];

    this.filtroForm = this.fb.group({
      fechaInicio: [fechaInicio],
      fechaFin: [fechaFin]
    });
  }

  ngOnInit(): void {
    this.canManageUsers = this.authService.hasPermission('USUARIOS_MANAGE');
    this.aplicarFiltros();
    this.cargarUltimasVentas(); 
  }

  aplicarFiltros() {
    const filtros = this.filtroForm.value;
    this.cargarEstadisticas(filtros);
    this.cargarVentasSemanales(filtros); 
    this.cargarTopProductos(filtros);
    this.cargarVentasPorCategoria(filtros); 
  }

  // ... (Métodos cargar... iguales a los anteriores) ...
  cargarEstadisticas(filtros: any) {
    this.isLoadingStats = true;
    this.http.post<DashboardStatsDTO>(`${this.apiUrlBase}/api/dashboard/estadisticas-filtradas`, filtros) 
      .subscribe({ next: (data) => { this.stats = data; this.isLoadingStats = false; }, error: (err) => { console.error(err); this.isLoadingStats = false; } });
  }
  cargarVentasSemanales(filtros: any) {
    this.isLoadingChart = true;
    this.http.post<VentasPorDiaDTO[]>(`${this.apiUrlBase}/api/dashboard/ventas-semanales`, filtros)
      .subscribe({ next: (data) => { this.ventasSemanales = data; this.isLoadingChart = false; }, error: (err) => { console.error(err); this.isLoadingChart = false; } });
  }
  cargarVentasPorCategoria(filtros: any) {
    this.isLoadingCategories = true;
    this.http.post<any[]>(`${this.apiUrlBase}/api/dashboard/ventas-categorias`, filtros)
      .subscribe({ next: (data) => { this.ventasPorCategoria = data; this.isLoadingCategories = false; }, error: (err) => { console.error(err); this.isLoadingCategories = false; } });
  }
  cargarTopProductos(filtros: any) {
    this.isLoadingTopProducts = true;
    this.http.post<TopProductoDTO[]>(`${this.apiUrlBase}/api/dashboard/top-productos`, filtros)
      .subscribe({ next: (data) => { this.topProductos = data; this.isLoadingTopProducts = false; }, error: (err) => { console.error(err); this.isLoadingTopProducts = false; } });
  }
  cargarUltimasVentas() {
    this.isLoadingRecentSales = true;
    this.ventaService.filtrarVentas({}, 0, 5).subscribe({ next: (page) => { this.ultimasVentas = page.content; this.isLoadingRecentSales = false; }, error: (err) => { console.error(err); this.isLoadingRecentSales = false; } });
  }

  // --- DESCARGAR PDF CON GRÁFICO ---
  descargarReporte() {
    this.isDownloading = true;
    const filtros = this.filtroForm.value;

    // 1. Obtener imagen optimizada para PDF (letras negras)
    let chartImage = '';
    if (this.salesChart) {
        const img = this.salesChart.getChartImageForPdf();
        if (img) chartImage = img;
    }

    const body = {
        ...filtros,
        graficoBase64: chartImage
    };

    this.http.post(`${this.apiUrlBase}/api/dashboard/reporte-pdf`, body, { responseType: 'blob' })
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `Reporte_Gestion_${filtros.fechaInicio}_${filtros.fechaFin}.pdf`;
          a.click();
          window.URL.revokeObjectURL(url);
          
          this.isDownloading = false;
          mostrarToast('Reporte generado con éxito', 'success');
        },
        error: (err) => {
          console.error('Error PDF:', err);
          mostrarToast('Error al generar el reporte PDF', 'danger');
          this.isDownloading = false;
        }
      });
  }

  nuevoProducto() {
    this.router.navigate(['/pos/productos/nuevo']);
  }
}
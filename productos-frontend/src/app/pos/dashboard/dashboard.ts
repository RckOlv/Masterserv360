import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, CurrencyPipe, DecimalPipe } from '@angular/common'; 
import { HttpClient, HttpErrorResponse } from '@angular/common/http'; // Importar HttpErrorResponse
import { API_URL } from '../../app.config'; 
import { Router, RouterModule } from '@angular/router'; 

// --- Mentor: INICIO DE LA MODIFICACIÓN ---
import { AuthService } from '../../service/auth.service'; // 1. Importar AuthService
import { HasPermissionDirective } from '../../directives/has-permission.directive'; // 2. Importar la Directiva
// --- Mentor: FIN DE LA MODIFICACIÓN ---

// --- Mentor: INICIO DE MODIFICACIÓN (Nuevos DTOs) ---
// (Asumiendo que ya creaste estos archivos en /models)
import { DashboardStatsDTO } from '../../models/dashboard-stats.model';
import { VentasPorDiaDTO, TopProductoDTO, VentaDTO } from '../../models/venta.model';
import { VentaService } from '../../service/venta.service';
import SalesChartComponent from './sales-chart/sales-chart'; 
// --- Mentor: FIN DE MODIFICACIÓN ---

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    CurrencyPipe, 
    DecimalPipe,
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    HasPermissionDirective, // 3. Añadir la Directiva a los imports
    SalesChartComponent     // 4. Añadir el componente de gráfico
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export default class DashboardComponent implements OnInit {

  private http = inject(HttpClient);
  private router = inject(Router); 
  private apiUrlBase = API_URL; 
  private ventaService = inject(VentaService); // Para últimas ventas

  // --- Mentor: INICIO DE LA MODIFICACIÓN ---
  private authService = inject(AuthService); // 5. Inyectar AuthService
  
  // 6. Crear propiedades públicas para los permisos
  public canManageUsers = false; 
  // (La variable 'isAdmin' de tu archivo original ya no es necesaria)
  // --- Mentor: FIN DE LA MODIFICACIÓN ---

  // --- Mentor: Estado del Dashboard (Actualizado) ---
  public stats: DashboardStatsDTO | null = null;
  public ventasSemanales: VentasPorDiaDTO[] = [];
  public topProductos: TopProductoDTO[] = [];
  public ultimasVentas: VentaDTO[] = [];
  
  public isLoadingStats = true;
  public isLoadingChart = true;
  public isLoadingTopProducts = true;
  public isLoadingRecentSales = true;
  // --- Mentor: FIN Estado del Dashboard ---
  
  constructor() {}

  ngOnInit(): void {
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 7. Asignar los permisos a las variables públicas
    this.canManageUsers = this.authService.hasPermission('USUARIOS_MANAGE');
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
    
    this.cargarEstadisticas();
    this.cargarVentasSemanales();
    this.cargarTopProductos();
    this.cargarUltimasVentas();
  }

  cargarEstadisticas() {
    this.isLoadingStats = true;
    this.http.get<DashboardStatsDTO>(`${this.apiUrlBase}/api/dashboard/estadisticas`) 
      .subscribe({
        next: (data) => {
          this.stats = data;
          this.isLoadingStats = false;
        },
        error: (err) => {
          console.error('Error cargando estadísticas:', err);
          this.isLoadingStats = false;
        }
      });
  }

  cargarVentasSemanales() {
    this.isLoadingChart = true;
    this.http.get<VentasPorDiaDTO[]>(`${this.apiUrlBase}/api/dashboard/ventas-semanales`)
      .subscribe({
        next: (data) => {
          this.ventasSemanales = data;
          this.isLoadingChart = false;
        },
        error: (err) => {
          console.error('Error cargando gráfico de ventas:', err);
          this.isLoadingChart = false;
        }
      });
  }

  cargarTopProductos() {
    this.isLoadingTopProducts = true;
    this.http.get<TopProductoDTO[]>(`${this.apiUrlBase}/api/dashboard/top-productos`)
      .subscribe({
        next: (data) => {
          this.topProductos = data;
          this.isLoadingTopProducts = false;
        },
        error: (err) => {
          console.error('Error cargando top productos:', err);
          this.isLoadingTopProducts = false;
        }
      });
  }
  
  cargarUltimasVentas() {
    this.isLoadingRecentSales = true;
    this.ventaService.filtrarVentas({}, 0, 5).subscribe({
      next: (page) => {
        this.ultimasVentas = page.content;
        this.isLoadingRecentSales = false;
      },
      error: (err) => {
        console.error('Error cargando últimas ventas:', err);
        this.isLoadingRecentSales = false;
      }
    });
  }

  /** 🔹 Redirección */
  nuevoProducto() {
    this.router.navigate(['/pos/productos/nuevo']);
  }
}
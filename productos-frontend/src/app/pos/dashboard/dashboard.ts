import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../../app.config'; // <-- 1. Importar la constante API_URL

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export default class DashboardComponent implements OnInit {

  private http = inject(HttpClient);
  private apiUrlBase = API_URL; // <-- 2. Usar la constante

  // Dejamos esto simple por ahora
  stats = [
    { titulo: 'Total Productos', valor: 0, icono: 'box-seam' },
    { titulo: 'Categorías', valor: 0, icono: 'tags' },
    { titulo: 'Usuarios', valor: 0, icono: 'people' },
  ];
  
  // ultimosProductos: any[] = []; // Comentado por ahora

  constructor() {}

  ngOnInit(): void {
    this.cargarEstadisticas();
    
    // El endpoint /productos/ultimos NO EXISTE.
    // Lo comentamos para que deje de dar error 403.
    // this.cargarUltimosProductos(); 
  }

  /** 🔹 Carga las estadísticas del dashboard */
  cargarEstadisticas() {
    // 3. LLAMADA CORREGIDA: Apunta a /api/dashboard/estadisticas
    this.http.get<any>(`${this.apiUrlBase}/api/dashboard/estadisticas`) 
      .subscribe({
        next: (data: any) => {
          console.log('🟢 Estadísticas recibidas:', data);
          // Asignamos los valores que SÍ DEVUELVE el backend
          this.stats[0].valor = data.productosBajoStock || 0; // Ajusta 'productosBajoStock' si lo llamaste diferente
          this.stats[1].valor = data.totalVentasMes || 0;    // Ajusta 'totalVentasMes'
          this.stats[2].valor = data.clientesActivos || 0;   // Ajusta 'clientesActivos'
        },
        error: (err: any) => console.error('🔴 Error cargando estadísticas:', err)
      });
  }

  /*
  // Este endpoint no existe, lo dejamos comentado
  cargarUltimosProductos() {
    this.http.get<any[]>(`${this.apiUrlBase}/api/productos/ultimos`) // <-- Esta ruta no existe
      .subscribe({
        next: data => {
          this.ultimosProductos = data;
        },
        error: err => console.error('🔴 Error cargando productos:', err)
      });
  }
  */

  /** 🔹 Redirección al formulario de nuevo producto */
  nuevoProducto() {
    // Esta navegación debe ser manejada por el Router, no por window.location
    // (Lo arreglaremos si es necesario, pero no es la causa del error 403)
    // window.location.href = '/productos/nuevo'; 
  }
}
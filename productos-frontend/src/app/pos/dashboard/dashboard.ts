import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, CurrencyPipe, DecimalPipe } from '@angular/common'; // ¡Añadir Pipes!
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../../app.config'; 
import { Router, RouterModule } from '@angular/router'; // <-- ¡Importar Router y RouterModule!

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, CurrencyPipe, DecimalPipe], // ¡Añadir Módulos!
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export default class DashboardComponent implements OnInit {

  private http = inject(HttpClient);
  private router = inject(Router); 
  private apiUrlBase = API_URL; 

  // --- ¡ARREGLADO! ---
  // Los títulos ahora coinciden con lo que el backend devuelve.
  stats = [
    { titulo: 'Productos Bajos de Stock', valor: 0, icono: 'box-seam', bg: 'bg-warning', link: '/pos/productos' }, // Cambiado a Warning
    { titulo: 'Ventas del Mes (ARS)', valor: 0, icono: 'cash-coin', bg: 'bg-success', link: '/pos/ventas-historial' }, // Cambiado ícono
    { titulo: 'Clientes Activos', valor: 0, icono: 'people', bg: 'bg-info', link: '/pos/usuarios' },
  ];
  // --------------------
  
  constructor() {}

  ngOnInit(): void {
    this.cargarEstadisticas();
  }

  cargarEstadisticas() {
    this.http.get<any>(`${this.apiUrlBase}/api/dashboard/estadisticas`) 
      .subscribe({
        next: (data: any) => {
          console.log('🟢 Estadísticas recibidas:', data);
          
          // --- ¡ARREGLADO! ---
          // Mapeamos los datos del DTO a nuestro array de stats
          this.stats[0].valor = data.productosBajoStock || 0;
          this.stats[1].valor = data.totalVentasMes || 0; 
          this.stats[2].valor = data.clientesActivos || 0;
        },
        error: (err: any) => console.error('🔴 Error cargando estadísticas:', err)
      });
  }

  /** 🔹 Redirección (¡Ahora con Router!) */
  nuevoProducto() {
    this.router.navigate(['/pos/productos/nuevo']);
  }
}
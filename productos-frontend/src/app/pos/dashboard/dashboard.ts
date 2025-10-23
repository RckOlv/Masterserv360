import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export default class DashboardComponent implements OnInit {

  ultimosProductos: any[] = []; // 👈 cambié el nombre para que sea más claro
  stats = [
    { titulo: 'Total Productos', valor: 0, icono: 'box-seam' },
    { titulo: 'Categorías', valor: 0, icono: 'tags' },
    { titulo: 'Usuarios', valor: 0, icono: 'people' },
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.cargarUltimosProductos();
    this.cargarEstadisticas();
  }

  /** 🔹 Carga los últimos productos */
  cargarUltimosProductos() {
    this.http.get<any[]>('http://localhost:8080/productos/ultimos')
      .subscribe({
        next: data => {
          console.log('🟢 Últimos productos recibidos:', data);
          this.ultimosProductos = data;
        },
        error: err => console.error('🔴 Error cargando productos:', err)
      });
  }

  /** 🔹 Carga las estadísticas del dashboard */
  cargarEstadisticas() {
    this.http.get<any>('http://localhost:8080/dashboard/estadisticas')
      .subscribe({
        next: data => {
          this.stats[0].valor = data.totalProductos || 0;
          this.stats[1].valor = data.totalCategorias || 0;
          this.stats[2].valor = data.totalUsuarios || 0;
        },
        error: err => console.error('🔴 Error cargando estadísticas:', err)
      });
  }

  /** 🔹 Redirección al formulario de nuevo producto */
  nuevoProducto() {
    window.location.href = '/productos/nuevo';
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { ResumenProductoCompra, DetalleComparativa } from '../models/compras.model';

@Injectable({
  providedIn: 'root'
})
export class ComprasService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/compras`;

  // Obtener lista de productos cotizados
  getProductosCotizados(): Observable<ResumenProductoCompra[]> {
    return this.http.get<ResumenProductoCompra[]>(`${this.apiUrl}/productos-cotizados`);
  }

  // Obtener detalle comparativo de un producto
  getComparativaProducto(id: number): Observable<DetalleComparativa[]> {
    return this.http.get<DetalleComparativa[]>(`${this.apiUrl}/comparativa/${id}`);
  }
}
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

export interface ValorizacionDTO {
  categoria: string;
  cantidadUnidades: number;
  valorTotal: number;
}

export interface StockInmovilizadoDTO {
  productoId: number;
  nombre: string;
  categoria: string;
  stockActual: number;
  costoUnitario: number;
  capitalParado: number;
  ultimaVenta: string; // Fecha ISO
  diasSinVenta: number;
}

export interface VariacionCostoDTO {
  producto: string;
  proveedor: string;
  fechaCompra: string;
  costoPagado: number;
  nroOrden: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReporteService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/reportes-avanzados`;

  // 1. Valorización
  getValorizacion(): Observable<ValorizacionDTO[]> {
    return this.http.get<ValorizacionDTO[]>(`${this.apiUrl}/valorizacion`);
  }

  // 2. Stock Inmovilizado (filtro de días opcional)
  getStockInmovilizado(dias: number = 90): Observable<StockInmovilizadoDTO[]> {
    return this.http.get<StockInmovilizadoDTO[]>(`${this.apiUrl}/inmovilizado?dias=${dias}`);
  }

  // 3. Historial de Costos
  getHistorialCostos(productoId: number): Observable<VariacionCostoDTO[]> {
    return this.http.get<VariacionCostoDTO[]>(`${this.apiUrl}/historial-costos/${productoId}`);
  }

  getUltimosCostosGenerales(): Observable<VariacionCostoDTO[]> {
  return this.http.get<VariacionCostoDTO[]>(`${this.apiUrl}/historial-costos-general`);
}
}
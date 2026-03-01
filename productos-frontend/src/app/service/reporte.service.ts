import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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
  ultimaVenta: string; 
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

  // 3. Historial de Costos (Feed General)
  getUltimosCostosGenerales(): Observable<VariacionCostoDTO[]> {
    return this.http.get<VariacionCostoDTO[]>(`${this.apiUrl}/historial-costos-general`);
  }

  // 4. Historial de Costos (Búsqueda por Nombre)
  buscarHistorialPorNombre(nombre: string): Observable<VariacionCostoDTO[]> {
    return this.http.get<VariacionCostoDTO[]>(`${this.apiUrl}/historial-costos/buscar?nombre=${encodeURIComponent(nombre)}`);
  }

  getProductosParaFiltro(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/productos`);
  }
  
  descargarPdfValorizacion(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/valorizacion/pdf`, { responseType: 'blob' });
  }

  descargarPdfInmovilizado(dias: number): Observable<Blob> {
    let params = new HttpParams().set('dias', dias.toString());
    return this.http.get(`${this.apiUrl}/inmovilizado/pdf`, { params, responseType: 'blob' });
  }

  descargarPdfCostos(nombre?: string): Observable<Blob> {
    let params = new HttpParams();
    if (nombre) {
      params = params.set('nombre', nombre);
    }
    return this.http.get(`${this.apiUrl}/historial-costos/pdf`, { params, responseType: 'blob' });
  }
}
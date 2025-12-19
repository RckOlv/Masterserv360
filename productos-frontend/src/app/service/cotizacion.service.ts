import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { CotizacionAdminDTO } from '../models/cotizacion-admin.model';
import { PedidoDTO } from '../models/pedido.model'; // Importa tu PedidoDTO

@Injectable({
  providedIn: 'root'
})
export class CotizacionService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/cotizaciones`; // URL base del Admin

  /**
   * Obtiene la lista de cotizaciones RECIBIDAS (listas para revisar).
   * GET /cotizaciones/recibidas
   */
  getCotizacionesRecibidas(): Observable<CotizacionAdminDTO[]> {
    return this.http.get<CotizacionAdminDTO[]>(`${this.apiUrl}/recibidas`);
  }

  /**
   * Obtiene el detalle de una cotización específica por su ID.
   * GET /cotizaciones/{id}
   */
  getCotizacionById(id: number): Observable<CotizacionAdminDTO> {
    return this.http.get<CotizacionAdminDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Cancela un item específico de una cotización.
   * PATCH /cotizaciones/item/{itemId}/cancelar
   */
  cancelarItem(itemId: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/item/${itemId}/cancelar`, {});
  }

  /**
   * Cancela una cotización completa.
   * PATCH /cotizaciones/{id}/cancelar
   */
  cancelarCotizacion(id: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${id}/cancelar`, {});
  }

  /**
   * Confirma la cotización ganadora.
   * El backend la convierte en un Pedido.
   * POST /cotizaciones/{id}/confirmar
   */
  confirmarCotizacion(id: number): Observable<PedidoDTO> {
    // Asumimos que el backend devuelve el PedidoDTO creado
    return this.http.post<PedidoDTO>(`${this.apiUrl}/${id}/confirmar`, {});
  }
}
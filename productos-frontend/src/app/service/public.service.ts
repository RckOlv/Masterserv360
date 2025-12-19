import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { CotizacionPublicaDTO } from '../models/cotizacion-publica.model';
import { OfertaProveedorDTO } from '../models/oferta-proveedor.model';

@Injectable({
  providedIn: 'root'
})
export class PublicService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/public`;

  // --- COTIZACIONES (Ya existía) ---
  getCotizacionPorToken(token: string): Observable<CotizacionPublicaDTO> {
    return this.http.get<CotizacionPublicaDTO>(`${this.apiUrl}/oferta/${token}`);
  }

  submitOferta(token: string, oferta: OfertaProveedorDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/oferta/${token}`, oferta);
  }

  // --- NUEVO: PEDIDOS (Agregado) ---
  obtenerPedidoPorToken(token: string): Observable<any> {
    // Devuelve un objeto genérico con los datos del pedido para mostrar en pantalla
    return this.http.get<any>(`${this.apiUrl}/pedido/${token}`);
  }

  /**
   * Envía la confirmación del pedido (fecha entrega y precios ajustados).
   * Llama a: POST /public/pedido/{token}/confirmar
   */
  confirmarPedidoProveedor(token: string, datos: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/pedido/${token}/confirmar`, datos);
  }
}
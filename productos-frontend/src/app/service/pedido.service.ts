// src/app/service/pedido.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { PedidoDTO } from '../models/pedido.model';
import { Page } from '../models/page.model';
import { PedidoDetallado } from '../models/pedido-detallado.model';

@Injectable({
  providedIn: 'root'
})
export class PedidoService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/pedidos`; 

  constructor() { }

  crear(pedido: PedidoDTO): Observable<PedidoDTO> {
    return this.http.post<PedidoDTO>(this.apiUrl, pedido);
  }

  getById(id: number): Observable<PedidoDTO> {
    return this.http.get<PedidoDTO>(`${this.apiUrl}/${id}`);
  }

  obtenerDetalles(id: number): Observable<PedidoDetallado> {
    return this.http.get<PedidoDetallado>(`${this.apiUrl}/${id}/detalles`);
  }

  // --- NUEVO: Descargar PDF ---
  descargarPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf`, { responseType: 'blob' });
  }
  // ---------------------------

  listarPedidos(page: number, size: number): Observable<Page<PedidoDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'fechaPedido,desc'); 
      
    return this.http.get<Page<PedidoDTO>>(this.apiUrl, { params }); 
  }

  marcarCompletado(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/completar`, {});
  }

  marcarCancelado(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/cancelar`, {});
  }
}
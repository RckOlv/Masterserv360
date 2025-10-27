// src/app/service/pedido.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { PedidoDTO } from '../models/pedido.model';
import { Page } from '../models/page.model'; // Importar Page

@Injectable({
  providedIn: 'root'
})
export class PedidoService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/pedidos`;

  constructor() { }

  /**
   * Crea un nuevo pedido.
   */
  crear(pedido: PedidoDTO): Observable<PedidoDTO> {
    return this.http.post<PedidoDTO>(this.apiUrl, pedido);
  }

  /**
   * Obtiene un pedido por su ID (con detalles).
   */
  getById(id: number): Observable<PedidoDTO> {
    return this.http.get<PedidoDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Lista todos los pedidos (paginado - ¡Endpoint pendiente en backend!)
   * TODO: Necesitamos crear /api/pedidos/filtrar en el backend.
   */
  listarPedidos(page: number, size: number): Observable<Page<PedidoDTO>> {
    // Asumimos un endpoint GET simple por ahora
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
      
    // ¡OJO! El backend aún no tiene /filtrar para pedidos, 
    // pero lo preparamos. Cambia a GET si es necesario.
    // Por ahora, simulamos un GET simple paginado:
    return this.http.get<Page<PedidoDTO>>(this.apiUrl, { params }); 
  }

  /**
   * Marca un pedido como COMPLETADO (actualiza stock).
   */
  marcarCompletado(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/completar`, {});
  }

  /**
   * Marca un pedido como CANCELADO.
   */
  marcarCancelado(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/cancelar`, {});
  }
}
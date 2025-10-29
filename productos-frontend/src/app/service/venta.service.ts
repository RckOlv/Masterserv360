// src/app/service/venta.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config'; // Tu constante de URL base
import { VentaDTO } from '../models/venta.model'; // DTO de Venta
import { Page } from './producto.service';

@Injectable({
  providedIn: 'root'
})
export class VentaService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/ventas`; // URL base para ventas

  constructor() { }

  /**
   * Llama al backend para crear (finalizar) una nueva venta.
   * Llama a: POST /api/ventas
   * @param venta DTO con los datos de la venta (clienteId, detalles).
   * @returns Observable con la VentaDTO creada por el backend.
   */
  crearVenta(venta: VentaDTO): Observable<VentaDTO> {
    // El backend obtiene el vendedorId del token, solo enviamos clienteId y detalles.
    // Asegúrate que tu VentaDTO en Angular NO incluya vendedorId como campo requerido.
    return this.http.post<VentaDTO>(this.apiUrl, venta);
  }

  getVentasPaginadas(page: number, size: number, sort: string = 'fechaVenta,desc'): Observable<Page<VentaDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort); // Añadimos el ordenamiento
    return this.http.get<Page<VentaDTO>>(this.apiUrl, { params });
  }

}
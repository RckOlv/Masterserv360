// src/app/service/venta.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config'; // Tu constante de URL base
import { VentaDTO } from '../models/venta.model'; // DTO de Venta
// Correct import for Page interface
import { Page } from '../models/page.model';
import { VentaFiltroDTO } from '../models/venta-filtro.model';

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
    return this.http.post<VentaDTO>(this.apiUrl, venta);
  }

  /**
   * Obtiene una lista paginada de ventas.
   * Llama a: GET /api/ventas?page=...&size=...&sort=fechaVenta,desc
   * @param page Número de página (base 0).
   * @param size Tamaño de la página.
   * @param sort Campo y dirección de ordenamiento.
   * @returns Observable con la página de VentaDTO.
   */
  getVentasPaginadas(page: number, size: number, sort: string = 'fechaVenta,desc'): Observable<Page<VentaDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort); // Añadimos el ordenamiento
    return this.http.get<Page<VentaDTO>>(this.apiUrl, { params });
  }

  /**
   * Obtiene los detalles completos de una venta por su ID.
   * Llama a: GET /api/ventas/{id}
   * @param id El ID de la venta a buscar.
   * @returns Observable con la VentaDTO completa.
   */
  getVentaById(id: number): Observable<VentaDTO> {
    return this.http.get<VentaDTO>(`${this.apiUrl}/${id}`);
  }


  cancelarVenta(id: number): Observable<void> {
     // Usamos http.patch y esperamos un tipo 'void' como respuesta.
     // Enviamos un cuerpo vacío {} como requiere PATCH si no hay datos que modificar.
     return this.http.patch<void>(`${this.apiUrl}/${id}/cancelar`, {});
  }

  filtrarVentas(filtro: VentaFiltroDTO, page: number, size: number, sort: string = 'fechaVenta,desc'): Observable<Page<VentaDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    // Llama al POST /filtrar enviando el objeto filtro en el cuerpo
    return this.http.post<Page<VentaDTO>>(`${this.apiUrl}/filtrar`, filtro, { params });
  }

  getComprobantePdf(id: number): Observable<Blob> {
    // ¡Clave! 'responseType: 'blob'' le dice a Angular
    // que la respuesta no es JSON, sino un archivo binario.
    return this.http.get(`${this.apiUrl}/${id}/comprobante`, {
      responseType: 'blob'
    });
  }

}

// src/app/service/carrito.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config'; // Tu constante de URL base
import { CarritoDTO } from '../models/carrito.model'; // DTO principal del carrito
// Importa los DTOs necesarios para las operaciones
import { AddItemCarritoDTO } from '../models/add-item-carrito.model'; // ¡Asegúrate de crear este archivo!
import { UpdateCantidadCarritoDTO } from '../models/update-cantidad-carrito.model'; // ¡Y este también!

@Injectable({
  providedIn: 'root'
})
export class CarritoService {

  private http = inject(HttpClient);
  // URL base para los endpoints del carrito
  private apiUrl = `${API_URL}/carrito`;

  constructor() { }

  /**
   * Obtiene el carrito actual del vendedor autenticado.
   * Llama a: GET /api/carrito
   * @returns Observable con el estado actual del carrito.
   */
  getCarrito(): Observable<CarritoDTO> {
    return this.http.get<CarritoDTO>(this.apiUrl);
  }

  /**
   * Agrega un producto (con cantidad) al carrito.
   * Llama a: POST /api/carrito/items
   * @param item DTO con productoId y cantidad.
   * @returns Observable con el estado actualizado del carrito.
   */
  agregarItem(item: AddItemCarritoDTO): Observable<CarritoDTO> {
    return this.http.post<CarritoDTO>(`${this.apiUrl}/items`, item);
  }

  /**
   * Elimina un item específico del carrito.
   * Llama a: DELETE /api/carrito/items/{itemCarritoId}
   * @param itemCarritoId ID del ItemCarrito a eliminar.
   * @returns Observable con el estado actualizado del carrito.
   */
  quitarItem(itemCarritoId: number): Observable<CarritoDTO> {
    return this.http.delete<CarritoDTO>(`${this.apiUrl}/items/${itemCarritoId}`);
  }

  /**
   * Actualiza la cantidad de un item específico en el carrito.
   * Llama a: PUT /api/carrito/items/{itemCarritoId}
   * @param itemCarritoId ID del ItemCarrito a actualizar.
   * @param nuevaCantidad DTO con la nueva cantidad.
   * @returns Observable con el estado actualizado del carrito.
   */
  actualizarCantidad(itemCarritoId: number, nuevaCantidad: UpdateCantidadCarritoDTO): Observable<CarritoDTO> {
    return this.http.put<CarritoDTO>(`${this.apiUrl}/items/${itemCarritoId}`, nuevaCantidad);
  }

  /**
   * Vacía completamente el carrito del usuario autenticado.
   * Llama a: DELETE /api/carrito
   * @returns Observable con el estado del carrito (vacío).
   */
  vaciarCarrito(): Observable<CarritoDTO> {
    return this.http.delete<CarritoDTO>(this.apiUrl);
  }

}
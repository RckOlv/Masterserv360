// src/app/service/categoria.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config'; // Asegúrate de tener esta constante
import { CategoriaDTO } from '../models/categoria.model';

@Injectable({
  providedIn: 'root'
})
export class CategoriaService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/categorias`; // Endpoint del backend para categorías

  constructor() { }

  /**
   * Obtiene todas las categorías (idealmente, solo las activas).
   * El backend debería encargarse de filtrar por estado si implementaste soft delete.
   */
  listarCategorias(): Observable<CategoriaDTO[]> {
    return this.http.get<CategoriaDTO[]>(this.apiUrl);
  }

  /**
   * Obtiene una categoría por su ID.
   */
  getById(id: number): Observable<CategoriaDTO> {
    return this.http.get<CategoriaDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea una nueva categoría.
   */
  crear(categoria: CategoriaDTO): Observable<CategoriaDTO> {
    // Quitamos el ID si existe, el backend lo genera
    const { id, ...categoriaParaCrear } = categoria;
    return this.http.post<CategoriaDTO>(this.apiUrl, categoriaParaCrear);
  }

  /**
   * Actualiza una categoría existente.
   * El DTO debe contener el ID.
   */
  actualizar(categoria: CategoriaDTO): Observable<CategoriaDTO> {
     if (!categoria.id) {
       // En un caso real, devolveríamos un Observable con error
       throw new Error('ID de categoría es requerido para actualizar');
     }
     return this.http.put<CategoriaDTO>(`${this.apiUrl}/${categoria.id}`, categoria);
  }

  /**
   * Realiza un borrado lógico (soft delete) de la categoría.
   */
  softDelete(id: number): Observable<any> {
    // Asume que el backend DELETE /api/categorias/{id} hace el soft delete
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  /**
   * Reactiva una categoría marcada como inactiva.
   */
  reactivar(id: number): Observable<any> {
     // Asume un endpoint PATCH (o PUT) para reactivar
     // Ajusta la URL si tu endpoint es diferente
     return this.http.patch<any>(`${this.apiUrl}/${id}/reactivar`, {});
  }
}
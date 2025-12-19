// src/app/service/proveedor.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { ProveedorDTO } from '../models/proveedor.model';

@Injectable({
  providedIn: 'root'
})
export class ProveedorService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/proveedores`;

  constructor() { }

  /**
   * Obtiene la lista de proveedores, filtrada por estado.
   * Llama a los métodos EAGER del backend.
   */
  listarProveedores(estado?: string | null): Observable<ProveedorDTO[]> {
    let params = new HttpParams();
    if (estado) {
      params = params.set('estado', estado); // Envía ?estado=ACTIVO, INACTIVO o TODOS
    }
    return this.http.get<ProveedorDTO[]>(this.apiUrl, { params });
  }

  /**
   * Obtiene un proveedor por su ID (versión Eager).
   */
  getById(id: number): Observable<ProveedorDTO> {
    return this.http.get<ProveedorDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea un nuevo proveedor.
   */
  crear(proveedor: ProveedorDTO): Observable<ProveedorDTO> {
    const { id, fechaCreacion, fechaModificacion, ...data } = proveedor; // Quita campos de auditoría e ID
    return this.http.post<ProveedorDTO>(this.apiUrl, data);
  }

  /**
   * Actualiza un proveedor existente.
   */
  actualizar(id: number, proveedor: ProveedorDTO): Observable<ProveedorDTO> {
     return this.http.put<ProveedorDTO>(`${this.apiUrl}/${id}`, proveedor);
  }

  /**
   * Realiza un borrado lógico (soft delete).
   */
  softDelete(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  /**
   * Reactiva un proveedor inactivo.
   */
  reactivar(id: number): Observable<any> {
     return this.http.patch<any>(`${this.apiUrl}/${id}/reactivar`, {});
  }
}
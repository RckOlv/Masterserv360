import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { PermisoDTO } from '../models/permiso.model'; // DTO Corregido

@Injectable({
  providedIn: 'root'
})
export class PermisoService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/permisos`; // Asumo esta ruta de backend

  /**
   * Obtiene la lista de permisos (idealmente, filtrados por estado si aplica)
   */
  listarPermisos(): Observable<PermisoDTO[]> {
    return this.http.get<PermisoDTO[]>(this.apiUrl);
  }

  /**
   * Obtiene un permiso por su ID.
   */
  getById(id: number): Observable<PermisoDTO> {
    return this.http.get<PermisoDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea un nuevo permiso.
   */
  crear(permiso: PermisoDTO): Observable<PermisoDTO> {
    // Quitamos el ID si existe, el backend lo genera
    const { id, ...data } = permiso;
    return this.http.post<PermisoDTO>(this.apiUrl, data);
  }

  /**
   * Actualiza un permiso existente.
   * El DTO debe contener el ID.
   */
  actualizar(permiso: PermisoDTO): Observable<PermisoDTO> {
     if (!permiso.id) {
       throw new Error('ID de permiso es requerido para actualizar');
     }
     return this.http.put<PermisoDTO>(`${this.apiUrl}/${permiso.id}`, permiso);
  }

  /**
   * Realiza un borrado lógico (soft delete) del permiso.
   * (Asegúrate de implementar esto en el backend)
   */
  softDelete(id: number): Observable<any> {
    // Asume endpoint DELETE /api/permisos/{id} que hace soft delete en backend
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }
}
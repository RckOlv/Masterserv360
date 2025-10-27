import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { RolDTO } from '../models/rol.model'; 

@Injectable({
  providedIn: 'root'
})
export class RolService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/roles`; // Ruta del backend

  constructor() { }

  /**
   * Obtiene la lista de todos los roles.
   */
  listarRoles(): Observable<RolDTO[]> {
    return this.http.get<RolDTO[]>(this.apiUrl);
  }

  /**
   * Crea un nuevo rol.
   */
  crear(rol: RolDTO): Observable<RolDTO> {
     // El backend espera un DTO sin ID, aunque el DTO de TS lo tenga opcional
     const { id, ...rolParaCrear } = rol;
     return this.http.post<RolDTO>(this.apiUrl, rolParaCrear);
  }

  /**
   * Actualiza un rol existente.
   */
  actualizar(id: number, rol: RolDTO): Observable<RolDTO> {
     // El backend espera el DTO completo en el body
     return this.http.put<RolDTO>(`${this.apiUrl}/${id}`, rol);
  }

  /**
   * Elimina un rol (borrado físico).
   */
  eliminar(id: number): Observable<any> { // Devuelve 'any' o 'void'
     return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }
}
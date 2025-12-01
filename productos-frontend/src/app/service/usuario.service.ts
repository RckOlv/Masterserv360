import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { UsuarioDTO } from '../models/usuario.model';
import { UsuarioFiltroDTO } from '../models/usuario-filtro.model'; // Importar
import { Page } from '../models/page.model'; // Importar

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/usuarios`;

  constructor() { }

  /**
   * MODIFICADO: Llama al endpoint de filtrado y paginación
   */
  filtrarUsuarios(filtro: UsuarioFiltroDTO, page: number, size: number): Observable<Page<UsuarioDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    // Usa el endpoint POST /filtrar que creamos en el backend
    return this.http.post<Page<UsuarioDTO>>(`${this.apiUrl}/filtrar`, filtro, { params });
  }

  /**
   * Obtiene un usuario por ID (para el form de edición)
   */
  getById(id: number): Observable<UsuarioDTO> {
    return this.http.get<UsuarioDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea un nuevo usuario (usado por el form de admin)
   */
  crearUsuarioAdmin(usuario: UsuarioDTO): Observable<UsuarioDTO> {
     const { id, ...data } = usuario; 
     return this.http.post<UsuarioDTO>(this.apiUrl, data);
  }

  /**
   * Actualiza un usuario (usado por el form de admin)
   */
  actualizarUsuarioAdmin(id: number, usuario: UsuarioDTO): Observable<UsuarioDTO> {
     return this.http.put<UsuarioDTO>(`${this.apiUrl}/${id}`, usuario);
  }

  /**
   * Soft Delete
   */
  softDelete(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  /**
   * Reactivar
   */
  reactivar(id: number): Observable<any> {
     return this.http.patch<any>(`${this.apiUrl}/${id}/reactivar`, {});
  }

  crearClienteRapido(usuario: any): Observable<UsuarioDTO> {
    // Llama al endpoint específico que permite acceso a vendedores
    return this.http.post<UsuarioDTO>(`${this.apiUrl}/cliente-rapido`, usuario);
  }

  getMiPerfil(): Observable<UsuarioDTO> {
    return this.http.get<UsuarioDTO>(`${this.apiUrl}/perfil`);
  }

  actualizarMiPerfil(usuario: UsuarioDTO): Observable<UsuarioDTO> {
    return this.http.put<UsuarioDTO>(`${this.apiUrl}/perfil`, usuario);
  }

  cambiarPassword(data: { passwordActual: string; passwordNueva: string }): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/perfil/cambiar-password`, data);
  }
}
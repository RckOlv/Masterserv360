import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config'; // Asegúrate que la ruta sea correcta
import { UsuarioDTO } from '../models/usuario.model'; // Asegúrate que la ruta sea correcta
// Importar RolDTO si es necesario para 'cambiarRol' o 'crear/actualizar'
// import { RolDTO } from '../models/rol.model'; 

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/usuarios`; // Endpoint del backend para usuarios

  constructor() { }

  /**
   * Obtiene la lista de usuarios (DTOs).
   * Idealmente, el backend permitiría filtrar/paginar. Por ahora, trae todos.
   */
  listarUsuarios(): Observable<UsuarioDTO[]> {
    return this.http.get<UsuarioDTO[]>(this.apiUrl);
  }

  /**
   * Obtiene los detalles de un usuario por su ID.
   */
  getById(id: number): Observable<UsuarioDTO> {
    return this.http.get<UsuarioDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea un nuevo usuario (función de Admin).
   * NOTA: El DTO enviado podría necesitar incluir el ID del rol o roles.
   * Ajustar según lo que espere el endpoint /api/usuarios (POST) del backend.
   */
  crearUsuarioAdmin(usuario: UsuarioDTO): Observable<UsuarioDTO> { // O usar un DTO específico de creación
     // Asume endpoint POST /api/usuarios
     // Quitamos ID si existe
     const { id, ...usuarioParaCrear } = usuario; 
     return this.http.post<UsuarioDTO>(this.apiUrl, usuarioParaCrear);
  }

  /**
   * Actualiza un usuario existente (función de Admin).
   * El DTO debe contener el ID.
   * Ajustar según lo que espere el endpoint /api/usuarios/{id} (PUT) del backend.
   */
  actualizarUsuarioAdmin(id: number, usuario: UsuarioDTO): Observable<UsuarioDTO> {
     // Asume endpoint PUT /api/usuarios/{id}
     return this.http.put<UsuarioDTO>(`${this.apiUrl}/${id}`, usuario);
  }

  /**
   * Realiza un borrado lógico (soft delete) del usuario.
   */
  softDelete(id: number): Observable<any> {
    // Asume endpoint DELETE /api/usuarios/{id} que hace soft delete en backend
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  /**
   * Reactiva un usuario marcado como inactivo.
   */
  reactivar(id: number): Observable<any> {
     // Asume endpoint PATCH /api/usuarios/{id}/reactivar (o similar)
     return this.http.patch<any>(`${this.apiUrl}/${id}/reactivar`, {});
  }

  /**
   * Cambia el rol (o roles) de un usuario.
   * NOTA: El endpoint del backend podría esperar un DTO específico con los IDs de los roles.
   * Esta es una implementación simplificada.
   */
  cambiarRol(idUsuario: number, idRol: number): Observable<UsuarioDTO> { // O devuelve 'any' si el backend no devuelve el usuario
    // Asume endpoint PUT o PATCH /api/usuarios/{idUsuario}/rol/{idRol} o similar
    // Ajustar URL y body según tu backend
    return this.http.put<UsuarioDTO>(`${this.apiUrl}/${idUsuario}/rol/${idRol}`, {});
  }

  /* // MÉTODOS DE REGISTRO ELIMINADOS (Ahora en AuthService)
   registrarCliente(usuario: UsuarioDTO): Observable<UsuarioDTO> { ... }
   registrarUsuario(usuario: UsuarioDTO): Observable<UsuarioDTO> { ... }
  */
}
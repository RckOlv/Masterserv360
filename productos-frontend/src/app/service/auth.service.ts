import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginResponse } from '../models/login-response.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  /**
   * Inicia sesión enviando email y password al backend.
   * Si la respuesta es exitosa, guarda el token JWT y los datos del usuario.
   */
  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap((response) => {
        if (response.status === 'success' && response.usuario && response.token) {
          // 🔹 Guardar token JWT
          localStorage.setItem('token', response.token);

          // 🔹 Guardar información del usuario
          localStorage.setItem('user', JSON.stringify(response.usuario));
        }
      })
    );
  }

  /**
   * Cierra sesión eliminando token y usuario del almacenamiento.
   */
  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  /**
   * Retorna el usuario logueado (si existe).
   */
  getUser(): any {
    const data = localStorage.getItem('user');
    return data ? JSON.parse(data) : null;
  }

  /**
   * Retorna el rol actual del usuario.
   */
  getUserRole(): string | null {
    const user = this.getUser();
    return user ? user.rol : null;
  }

  /**
   * Retorna true si hay usuario logueado.
   */
  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  /**
   * Control de permisos básico por rol.
   */
  tienePermiso(permiso: string): boolean {
    const user = this.getUser();
    if (!user) return false;

    if (user.rol === 'ADMIN') return true;

    const permisosPorRol: Record<string, string[]> = {
      'VENDEDOR': ['USUARIO_VER', 'PRODUCTO_VER'],
      'CLIENTE': ['PRODUCTO_VER']
    };

    const permisos = permisosPorRol[user.rol] || [];
    return permisos.includes(permiso);
  }
}

// src/app/service/rol.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError, shareReplay } from 'rxjs/operators';
import { API_URL } from '../app.config';
import { RolDTO } from '../models/rol.model';
import { mostrarToast } from '../utils/toast'; // Asegúrate que la ruta sea correcta

@Injectable({
  providedIn: 'root'
})
export class RolService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/roles`;

  // Propiedad para la Caché del ID Cliente
  // La hacemos directamente Observable<number | null> para simplificar
  private clienteRoleId$: Observable<number | null> | null = null;

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
     const { id, ...rolParaCrear } = rol;
     return this.http.post<RolDTO>(this.apiUrl, rolParaCrear);
  }

  /**
   * Actualiza un rol existente.
   */
  actualizar(id: number, rol: RolDTO): Observable<RolDTO> {
     return this.http.put<RolDTO>(`${this.apiUrl}/${id}`, rol);
  }

  /**
   * Elimina un rol (borrado físico).
   */
  eliminar(id: number): Observable<any> {
     return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  /**
   * Busca un rol por su nombre exacto en el backend.
   */
  getRolByNombre(nombre: string): Observable<RolDTO> {
    return this.http.get<RolDTO>(`${this.apiUrl}/by-nombre`, { params: { nombre } });
  }

  /**
   * Obtiene el ID numérico del rol 'ROLE_CLIENTE' desde la API (con caché).
   * @returns Un Observable que emite el ID del rol cliente o null si no se encuentra/hay error.
   */
  getClienteRoleId(): Observable<number | null> {
    if (!this.clienteRoleId$) {
      this.clienteRoleId$ = this.getRolByNombre('ROLE_CLIENTE').pipe(
        // --- CORRECCIÓN AQUÍ ---
        // Aseguramos que el map devuelva 'number' o 'null', nunca 'undefined'
        map(rol => rol?.id ?? null), // Usa optional chaining (?.) y nullish coalescing (??)
        // Alternativa si no usas TS moderno: map(rol => (rol && rol.id) ? rol.id : null),
        // -------------------------
        catchError((error) => {
            console.error("¡ERROR CRÍTICO! No se pudo obtener el ID del rol ROLE_CLIENTE desde la API.", error);
            // Asegúrate que mostrarToast esté disponible aquí o usa console.error
             try {
               mostrarToast("Error crítico: No se pudo configurar el rol de cliente.", "danger");
             } catch (e) {
               console.error("Fallo al mostrar toast:", e);
             }
            return of(null); // catchError devuelve Observable<null>
        }),
        shareReplay(1) // Cachea el resultado (number | null)
      );
    }
    // Devolvemos el observable cacheado, que ahora sí es Observable<number | null>
    return this.clienteRoleId$;
  }
}
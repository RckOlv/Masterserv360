// src/app/service/rol.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError, shareReplay } from 'rxjs/operators';
import { API_URL } from '../app.config';
import { RolDTO } from '../models/rol.model'; // Asegúrate de tener este modelo
import { mostrarToast } from '../utils/toast'; // Importar si lo usas aquí

@Injectable({
  providedIn: 'root'
})
export class RolService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/roles`;

  // --- Propiedades para Caché de IDs ---
  private clienteRoleId$: Observable<number | null> | null = null;
  private vendedorRoleId$: Observable<number | null> | null = null; // <-- NUEVA CACHÉ
  // ------------------------------------

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
   */
  getClienteRoleId(): Observable<number | null> {
    if (!this.clienteRoleId$) {
      this.clienteRoleId$ = this.getRolByNombre('ROLE_CLIENTE').pipe( // Busca por el nombre exacto
        map(rol => rol?.id ?? null), // Extrae el ID o devuelve null
        catchError((error) => {
            console.error("¡ERROR CRÍTICO! No se pudo obtener el ID del rol ROLE_CLIENTE desde la API.", error);
             try {
               mostrarToast("Error crítico: No se pudo configurar el rol de cliente.", "danger");
             } catch (e) { console.error("Fallo al mostrar toast:", e); }
            return of(null);
        }),
        shareReplay(1) // Cachea el resultado
      );
    }
    return this.clienteRoleId$;
  }

  // --- ¡NUEVO MÉTODO CON CACHÉ PARA VENDEDOR! ---
  /**
   * Obtiene el ID numérico del rol 'ROLE_VENDEDOR' desde la API (con caché).
   * **IMPORTANTE:** Asegúrate de que 'ROLE_VENDEDOR' sea el nombre exacto en tu BD.
   * @returns Un Observable que emite el ID del rol vendedor o null si no se encuentra/hay error.
   */
  getVendedorRoleId(): Observable<number | null> {
    if (!this.vendedorRoleId$) {
      // Reemplaza 'ROLE_VENDEDOR' si el nombre en tu BD es diferente (ej. 'VENDEDOR')
      this.vendedorRoleId$ = this.getRolByNombre('ROLE_VENDEDOR').pipe(
        map(rol => rol?.id ?? null), // Extrae el ID o devuelve null
        catchError((error) => {
            console.error("¡ERROR CRÍTICO! No se pudo obtener el ID del rol ROLE_VENDEDOR desde la API.", error);
             try {
               mostrarToast("Error crítico: No se pudo configurar el rol de vendedor.", "danger");
             } catch (e) { console.error("Fallo al mostrar toast:", e); }
            return of(null);
        }),
        shareReplay(1) // Cachea el resultado
      );
    }
    return this.vendedorRoleId$;
  }
  // ---------------------------------------------
}
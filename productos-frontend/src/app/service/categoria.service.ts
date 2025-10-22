import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Categoria } from '../models/categoria.model';

@Injectable({
  providedIn: 'root'
})
export class CategoriaService {
  private apiUrl = 'http://localhost:8080/categorias';

  constructor(private http: HttpClient) {}

  /** 🔹 Listar todas las categorías */
  listarCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(this.apiUrl).pipe(
      catchError(this.manejarError)
    );
  }

  /** 🔹 Listar solo las activas */
  listarActivas(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.apiUrl}/activas`).pipe(
      catchError(this.manejarError)
    );
  }

  /** 🔹 Listar solo las inactivas */
  listarInactivas(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.apiUrl}/inactivas`).pipe(
      catchError(this.manejarError)
    );
  }

  /** 🔹 Buscar por nombre */
  buscarPorNombre(nombre: string): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.apiUrl}/buscar?nombre=${nombre}`).pipe(
      catchError(this.manejarError)
    );
  }

  /** 🔹 Crear nueva categoría */
  crear(categoria: Categoria): Observable<Categoria> {
    return this.http.post<Categoria>(this.apiUrl, categoria).pipe(
      map((data) => data as Categoria),
      catchError(this.manejarError)
    );
  }

  /** 🔹 Actualizar categoría existente */
  actualizar(categoria: Categoria): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.apiUrl}/${categoria.idCategoria}`, categoria).pipe(
      map((data) => data as Categoria),
      catchError(this.manejarError)
    );
  }

  /** 🔹 Eliminar categoría (lógico o físico según backend) */
  darDebajaLogico(idCategoria: number): Observable<Categoria> {
    return this.http.delete<Categoria>(`${this.apiUrl}/${idCategoria}`).pipe(
      map((data) => data as Categoria),
      catchError(this.manejarError)
    );
  }

  /** 🔹 Reactivar categoría */
  reactivar(idCategoria: number): Observable<Categoria> {
    return this.http.post<Categoria>(`${this.apiUrl}/${idCategoria}/reactivar`, {}).pipe(
      map((data) => data as Categoria),
      catchError(this.manejarError)
    );
  }

  /** ⚠️ Manejo centralizado de errores */
  private manejarError(error: HttpErrorResponse) {
    let mensaje = 'Ocurrió un error desconocido';

    if (error.status === 0) {
      mensaje = 'No se puede conectar con el servidor.';
    } else if (error.status === 404) {
      mensaje = 'Categoría no encontrada.';
    } else if (error.status >= 400 && error.status < 500) {
      mensaje = 'Error en la solicitud (verifique los datos).';
    } else if (error.status >= 500) {
      mensaje = 'Error interno del servidor.';
    }

    console.error('Error HTTP:', error);
    return throwError(() => new Error(mensaje));
  }

  inactivarCategoria(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/inactivar`, {});
  }

  reactivarCategoria(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/reactivar`, {});
  }

}

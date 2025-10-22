import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Producto } from '../models/producto.model';

@Injectable({
  providedIn: 'root'
})
export class ProductoService {
  private apiUrl = 'http://localhost:8080/productos'; // ✅ URL base del backend

  constructor(private http: HttpClient) {}

  /** 🔹 Listar todos los productos (activos e inactivos) */
  listarTodos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.apiUrl}/todos`)
      .pipe(catchError(this.manejarError));
  }

  /** 🔹 Crear producto */
  crearProducto(producto: Producto): Observable<any> {
    return this.http.post(this.apiUrl, producto)
      .pipe(catchError(this.manejarError));
  }

  /** 🔹 Actualizar producto */
  actualizarProducto(producto: Producto): Observable<any> {
    return this.http.put(`${this.apiUrl}/${producto.idProducto}`, producto)
      .pipe(catchError(this.manejarError));
  }

  /** 🔹 Inactivar producto */
  inactivarProducto(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/inactivar`, {})
      .pipe(catchError(this.manejarError));
  }

  /** 🔹 Reactivar producto */
  reactivarProducto(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/reactivar`, {})
      .pipe(catchError(this.manejarError));
  }

 /** 🔹 Filtrar productos por varios criterios combinados */
filtrarProductos(
  nombre?: string,
  categoriaId?: number,
  activo?: boolean,
  desde?: string,
  hasta?: string
): Observable<Producto[]> {
  let params = new HttpParams();

  if (nombre) params = params.set('nombre', nombre);
  if (categoriaId) params = params.set('categoriaId', categoriaId.toString());
  if (activo !== undefined) params = params.set('activo', String(activo)); // 🔹 clave correcta
  if (desde) params = params.set('desde', desde);
  if (hasta) params = params.set('hasta', hasta);

  return this.http.get<Producto[]>(`${this.apiUrl}/filtrar`, { params })
    .pipe(catchError(this.manejarError));
}

  /** 🔹 Manejo de errores */
  private manejarError(error: any) {
    console.error('Error HTTP:', error);
    return throwError(() => new Error('Error en la solicitud HTTP'));
  }
}
